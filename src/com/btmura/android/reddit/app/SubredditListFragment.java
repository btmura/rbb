/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.app;

import java.util.Arrays;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;
import com.btmura.android.reddit.view.SwipeDismissTouchListener.OnSwipeDismissListener;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.SubredditView;

public class SubredditListFragment extends ThingProviderListFragment implements
        OnSwipeDismissListener, MultiChoiceModeListener, OnItemSelectedListener {

    public static final String TAG = "SubredditListFragment";

    /** Integer argument specifying the type of content shown in this fragment. */
    private static final String EXTRA_TYPE = "type";

    private static final int TYPE_ACCOUNT = 1;
    private static final int TYPE_SEARCH = 2;

    public interface OnSubredditSelectedListener {
        /**
         * Notifies the listener of the first subreddit in the loaded list. If there are no
         * subreddits, then subreddit is null. If there was an error, then subreddit is null but
         * error is true. Otherwise, subreddit is non-null with error set to false.
         */
        void onInitialSubredditSelected(String subreddit, boolean error);

        void onSubredditSelected(View view, String subreddit);
    }

    private OnSubredditSelectedListener listener;
    private AccountResultHolder accountResultHolder;

    private SubredditListController controller;

    private ActionMode actionMode;
    private AccountNameAdapter accountNameAdapter;
    private TextView subredditCountText;
    private Spinner accountSpinner;

    public static SubredditListFragment newAccountInstance(String accountName,
            String selectedSubreddit, boolean singleChoice) {
        Bundle args = new Bundle(4);
        args.putInt(EXTRA_TYPE, TYPE_ACCOUNT);
        args.putString(AccountSubredditListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(AccountSubredditListController.EXTRA_SELECTED_SUBREDDIT, selectedSubreddit);
        args.putBoolean(AccountSubredditListController.EXTRA_SINGLE_CHOICE, singleChoice);
        return newFragment(args);
    }

    public static SubredditListFragment newSearchInstance(String accountName, String query,
            boolean singleChoice) {
        Bundle args = new Bundle(4);
        args.putInt(EXTRA_TYPE, TYPE_SEARCH);
        args.putString(SubredditSearchController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(SubredditSearchController.EXTRA_QUERY, query);
        args.putBoolean(SubredditSearchController.EXTRA_SINGLE_CHOICE, singleChoice);
        return newFragment(args);
    }

    private static SubredditListFragment newFragment(Bundle args) {
        SubredditListFragment frag = new SubredditListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubredditSelectedListener) {
            listener = (OnSubredditSelectedListener) activity;
        }
        if (activity instanceof AccountResultHolder) {
            accountResultHolder = (AccountResultHolder) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = createController();
        if (savedInstanceState != null) {
            controller.restoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);

        SwipeDismissTouchListener touchListener = new SwipeDismissTouchListener(listView, this);
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(controller.getAdapter());
        // Only show the spinner if this is a single pane display since showing
        // two spinners can be annoying.
        setListShown(controller.isSingleChoice());
        loadIfPossible();
    }

    public void loadIfPossible() {
        if (controller.isLoadable()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return controller.createLoader();
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (controller.swapCursor(cursor)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onLoadFinished");
            }

            // TODO: Remove dependency on ThingProviderListFragment.
            super.onLoadFinished(loader, cursor);

            setEmptyText(getEmptyText(cursor == null));
            setListShown(true);

            if (actionMode != null) {
                actionMode.invalidate();
            }
            if (listener != null) {
                if (cursor == null) {
                    listener.onInitialSubredditSelected(null, true);
                } else if (cursor.getCount() == 0) {
                    listener.onInitialSubredditSelected(null, false);
                } else {
                    listener.onInitialSubredditSelected(getSubreddit(0), false);
                }
            }
        }
    }

    private String getEmptyText(boolean error) {
        if (controller.isSingleChoice()) {
            return ""; // Don't show duplicate message in multipane layout.
        }
        return getString(error ? R.string.error : R.string.empty_list);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        throw new IllegalStateException();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        controller.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        String selectedSubreddit = controller.setSelectedPosition(position);
        if (controller.isSingleChoice() && view instanceof SubredditView) {
            ((SubredditView) view).setChosen(true);
        }
        if (listener != null) {
            listener.onSubredditSelected(view, selectedSubreddit);
        }
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return controller.isSwipeDismissable(position);
    }

    @Override
    public void onSwipeDismiss(ListView listView, View view, int position) {
        Provider.removeSubredditAsync(getActivity(), getAccountName(), getSubreddit(position));
    }

    static class ViewHolder {
        TextView subredditCountText;
        Spinner accountSpinner;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (controller.getAdapter().getCursor() == null) {
            getListView().clearChoices();
            return false;
        }

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.subreddit_cab, null, false);
        mode.setCustomView(v);

        actionMode = mode;
        subredditCountText = (TextView) v.findViewById(R.id.subreddit_count);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);

        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();

        // We can set the subreddit count if the adapter is ready.
        boolean isAdapterReady = controller.getAdapter().getCursor() != null;
        if (isAdapterReady) {
            String text = getResources().getQuantityString(R.plurals.subreddits, count, count);
            subredditCountText.setText(text);
            subredditCountText.setVisibility(View.VISIBLE);
        } else {
            subredditCountText.setVisibility(View.GONE);
        }

        // Show spinner in subreddit search and accounts have been loaded.
        boolean isQuery = controller.getAdapter().isQuery();
        if (controller.getAdapter().isQuery() && accountResultHolder != null
                && accountResultHolder.getAccountResult() != null) {
            if (accountNameAdapter == null) {
                accountNameAdapter = new AccountNameAdapter(getActivity(),
                        R.layout.account_name_row);
            } else {
                accountNameAdapter.clear();
            }

            AccountResult result = accountResultHolder.getAccountResult();
            accountNameAdapter.addAll(result.accountNames);
            accountSpinner.setAdapter(accountNameAdapter);
            accountSpinner.setOnItemSelectedListener(this);

            // Don't show the spinner if only the app storage account is
            // available, since there is only one choice.
            if (result.accountNames.length > 0) {
                if (!controller.hasActionAccountName()) {
                    controller.setActionAccountName(result.getLastAccount(getActivity()));
                }
                int position = accountNameAdapter.findAccountName(
                        controller.getActionAccountName());
                accountSpinner.setSelection(position);
                accountSpinner.setVisibility(View.VISIBLE);
            } else {
                accountSpinner.setSelection(0);
                accountSpinner.setVisibility(View.GONE);
            }
        } else {
            accountSpinner.setAdapter(null);
            accountSpinner.setVisibility(View.GONE);
        }

        menu.findItem(R.id.menu_add).setVisible(isQuery && isAdapterReady);
        menu.findItem(R.id.menu_delete).setVisible(!isQuery && isAdapterReady);

        MenuItem subredditItem = menu.findItem(R.id.menu_subreddit);
        subredditItem.setVisible(isAdapterReady && count == 1
                && hasSidebar(getFirstCheckedPosition()));

        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                return handleAdd(mode);

            case R.id.menu_subreddit:
                return handleSubreddit(mode);

            case R.id.menu_delete:
                return handleDelete(mode);

            default:
                return false;
        }
    }

    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        subredditCountText = null;
        accountSpinner = null;

        // Don't persist the selected account across action modes.
        controller.setActionAccountName(null);
    }

    public void onItemSelected(AdapterView<?> av, View v, int position, long id) {
        controller.setActionAccountName(accountNameAdapter.getItem(position));
    }

    public void onNothingSelected(AdapterView<?> av) {
        controller.setActionAccountName(null);
    }

    private boolean handleAdd(ActionMode mode) {
        String accountName = getSelectedAccountName();
        String[] subreddits = getCheckedSubreddits();
        Provider.addSubredditAsync(getActivity(), accountName, subreddits);
        mode.finish();
        return true;
    }

    private boolean handleSubreddit(ActionMode mode) {
        MenuHelper.startSidebarActivity(getActivity(), getSubreddit(getFirstCheckedPosition()));
        return true;
    }

    private boolean handleDelete(ActionMode mode) {
        String accountName = getSelectedAccountName();
        String[] subreddits = getCheckedSubreddits();
        Provider.removeSubredditAsync(getActivity(), accountName, subreddits);
        mode.finish();
        return true;
    }

    private String getSelectedAccountName() {
        if (getQuery() != null) {
            return accountNameAdapter.getItem(accountSpinner.getSelectedItemPosition());
        }
        return getAccountName();
    }

    private String[] getCheckedSubreddits() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int checkedCount = getListView().getCheckedItemCount();
        String[] subreddits = new String[checkedCount];
        int i = 0, j = 0, count = controller.getAdapter().getCount();
        for (; i < count; i++) {
            if (checked.get(i) && controller.getAdapter().isDeletable(i)) {
                subreddits[j++] = getSubreddit(i);
            }
        }
        return Arrays.copyOf(subreddits, j);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveInstanceState(outState);
    }

    public String getAccountName() {
        return controller.getAccountName();
    }

    public void setAccountName(String accountName) {
        controller.setAccountName(accountName);
    }

    public String getSelectedSubreddit() {
        return controller.getSelectedSubreddit();
    }

    public void setSelectedSubreddit(String subreddit) {
        controller.setSelectedSubreddit(subreddit);
    }

    public String getQuery() {
        return controller.getAdapter().getQuery();
    }

    private boolean hasSidebar(int position) {
        return Subreddits.hasSidebar(getSubreddit(position));
    }

    private String getSubreddit(int position) {
        return controller.getAdapter().getName(position);
    }

    private int getFirstCheckedPosition() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int size = controller.getAdapter().getCount();
        for (int i = 0; i < size; i++) {
            if (checked.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private SubredditListController createController() {
        switch (getTypeArgument()) {
            case TYPE_ACCOUNT:
                return new AccountSubredditListController(getActivity(), getArguments());

            case TYPE_SEARCH:
                return new SubredditSearchController(getActivity(), getArguments());

            default:
                throw new IllegalArgumentException();
        }
    }

    // Getters for fragment arguments.

    private int getTypeArgument() {
        return getArguments().getInt(EXTRA_TYPE);
    }
}
