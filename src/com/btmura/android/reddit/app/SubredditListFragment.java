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
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.view.SwipeTouchListener;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditView;

public class SubredditListFragment extends ThingProviderListFragment implements
        MultiChoiceModeListener, OnItemSelectedListener {

    public static final String TAG = "SubredditListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SELECTED_SUBREDDIT = "selectedSubreddit";
    private static final String ARG_QUERY = "query";
    private static final String ARG_FLAGS = "flags";

    private static final String STATE_SESSION_ID = "sessionId";
    private static final String STATE_ACCOUNT_NAME = ARG_ACCOUNT_NAME;
    private static final String STATE_SELECTED_SUBREDDIT = ARG_SELECTED_SUBREDDIT;
    private static final String STATE_SELECTED_ACTION_ACCOUNT = "selectedActionAccount";

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

    private boolean singleChoice;
    private SubredditAdapter adapter;

    private ActionMode actionMode;
    private AccountNameAdapter accountNameAdapter;
    private TextView subredditCountText;
    private Spinner accountSpinner;
    private String selectedActionAccount;

    public static SubredditListFragment newInstance(String accountName, String selectedSubreddit,
            String query, int flags) {
        Bundle args = new Bundle(4);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SELECTED_SUBREDDIT, selectedSubreddit);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);

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

        String query = getArguments().getString(ARG_QUERY);
        int flags = getArguments().getInt(ARG_FLAGS);
        singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);
        if (!TextUtils.isEmpty(query)) {
            adapter = SubredditAdapter.newSearchInstance(getActivity(), query, singleChoice);
        } else {
            adapter = SubredditAdapter.newSubredditsInstance(getActivity(), singleChoice);
        }

        if (savedInstanceState == null) {
            adapter.setAccountName(getArguments().getString(ARG_ACCOUNT_NAME));
            adapter.setSelectedSubreddit(getArguments().getString(ARG_SELECTED_SUBREDDIT));
        } else {
            adapter.setSessionId(savedInstanceState.getLong(STATE_SESSION_ID, -1));
            adapter.setAccountName(savedInstanceState.getString(STATE_ACCOUNT_NAME));
            adapter.setSelectedSubreddit(savedInstanceState.getString(STATE_SELECTED_SUBREDDIT));
            selectedActionAccount = savedInstanceState.getString(STATE_SELECTED_ACTION_ACCOUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);

        SwipeTouchListener touchListener = new SwipeTouchListener(listView);
        listView.setOnTouchListener(touchListener);
        listView.setOnScrollListener(touchListener.makeScrollListener());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        // Only show the spinner if this is a single pane display since showing
        // two spinners can be annoying.
        setListShown(singleChoice);
        loadIfPossible();
    }

    public void loadIfPossible() {
        if (adapter.isLoadable()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return adapter.getLoader(getActivity());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);

        adapter.updateLoaderUri(getActivity(), loader);
        adapter.swapCursor(cursor);
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
                listener.onInitialSubredditSelected(adapter.getName(0), false);
            }
        }
    }

    private String getEmptyText(boolean error) {
        if (singleChoice) {
            return ""; // Don't show duplicate message in multipane layout.
        }
        return getString(error ? R.string.error : R.string.empty_list);
    }

    @Override
    protected void onSessionIdLoaded(long sessionId) {
        adapter.setSessionId(sessionId);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        throw new IllegalStateException();
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
        adapter.deleteSessionData(getActivity());
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        String selectedSubreddit = adapter.setSelectedPosition(position);
        if (singleChoice && view instanceof SubredditView) {
            ((SubredditView) view).setChosen(true);
        }
        if (listener != null) {
            listener.onSubredditSelected(view, selectedSubreddit);
        }
    }

    static class ViewHolder {
        TextView subredditCountText;
        Spinner accountSpinner;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (adapter.getCursor() == null) {
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
        // We can set the subreddit count if the adapter is ready.
        boolean isAdapterReady = adapter.getCursor() != null;
        if (isAdapterReady) {
            int count = getListView().getCheckedItemCount();
            String text = getResources().getQuantityString(R.plurals.subreddits, count, count);
            subredditCountText.setText(text);
            subredditCountText.setVisibility(View.VISIBLE);
        } else {
            subredditCountText.setVisibility(View.GONE);
        }

        // Show spinner in subreddit search and accounts have been loaded.
        boolean isQuery = adapter.isQuery();
        if (adapter.isQuery() && accountResultHolder != null
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
                if (selectedActionAccount == null) {
                    selectedActionAccount = result.getLastAccount(getActivity());
                }
                int position = accountNameAdapter.findAccountName(selectedActionAccount);
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
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                return handleAdd(mode);

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
        selectedActionAccount = null;
    }

    public void onItemSelected(AdapterView<?> av, View v, int position, long id) {
        selectedActionAccount = accountNameAdapter.getItem(position);
    }

    public void onNothingSelected(AdapterView<?> av) {
        selectedActionAccount = null;
    }

    private boolean handleAdd(ActionMode mode) {
        String accountName = getSelectedAccountName();
        String[] subreddits = getCheckedSubreddits();
        Provider.addSubredditAsync(getActivity(), accountName, subreddits);
        mode.finish();
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
        int i = 0, j = 0, count = adapter.getCount();
        for (; i < count; i++) {
            if (checked.get(i) && adapter.isDeletable(i)) {
                subreddits[j++] = adapter.getName(i);
            }
        }
        return Arrays.copyOf(subreddits, j);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SESSION_ID, adapter.getSessionId());
        outState.putString(STATE_ACCOUNT_NAME, adapter.getAccountName());
        outState.putString(STATE_SELECTED_SUBREDDIT, adapter.getSelectedSubreddit());
        outState.putString(STATE_SELECTED_ACTION_ACCOUNT, selectedActionAccount);
    }

    public String getAccountName() {
        return adapter.getAccountName();
    }

    public void setAccountName(String accountName) {
        adapter.setAccountName(accountName);
    }

    public String getSelectedSubreddit() {
        return adapter.getSelectedSubreddit();
    }

    public void setSelectedSubreddit(String subreddit) {
        adapter.setSelectedSubreddit(subreddit);
    }

    public String getQuery() {
        return adapter.getQuery();
    }
}
