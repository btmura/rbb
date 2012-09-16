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

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
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
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.widget.SubredditAdapter;

public class SubredditListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    public static final String TAG = "SubredditListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SELECTED_SUBREDDIT = "selectedSubreddit";
    private static final String ARG_QUERY = "query";
    private static final String ARG_FLAGS = "fags";

    private static final String STATE_ACCOUNT_NAME = ARG_ACCOUNT_NAME;
    private static final String STATE_SELECTED_SUBREDDIT = ARG_SELECTED_SUBREDDIT;
    private static final String STATE_SESSION_ID = "sessionId";

    public interface OnSubredditSelectedListener {
        void onInitialSubredditSelected(String subreddit);

        void onSubredditSelected(String subreddit);
    }

    private String accountName;
    private String sessionId;
    private String selectedSubreddit;
    private String query;
    private boolean sync;
    private boolean singleChoice;
    private SubredditAdapter adapter;
    private OnSubredditSelectedListener listener;

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        query = getArguments().getString(ARG_QUERY);
        sync = savedInstanceState == null;

        if (savedInstanceState == null) {
            accountName = getArguments().getString(ARG_ACCOUNT_NAME);
            selectedSubreddit = getArguments().getString(ARG_SELECTED_SUBREDDIT);
            if (!TextUtils.isEmpty(query)) {
                sessionId = query + "-" + System.currentTimeMillis();
            }
        } else {
            accountName = savedInstanceState.getString(STATE_ACCOUNT_NAME);
            selectedSubreddit = savedInstanceState.getString(STATE_SELECTED_SUBREDDIT);
            sessionId = savedInstanceState.getString(STATE_SESSION_ID);
        }

        int flags = getArguments().getInt(ARG_FLAGS);
        singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);

        adapter = new SubredditAdapter(getActivity(), query, singleChoice);
        adapter.setSelectedSubreddit(selectedSubreddit);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(this);
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
        if (accountName != null) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        return SubredditAdapter.getLoader(getActivity(), accountName, sessionId, query, sync);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }
        sync = false;
        SubredditAdapter.updateLoader(getActivity(), loader, accountName, sessionId, query, sync);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_subreddits : R.string.error));
        setListShown(true);
        if (cursor != null && cursor.getCount() > 0) {
            listener.onInitialSubredditSelected(adapter.getName(0));
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        selectedSubreddit = adapter.setSelectedPosition(position);
        if (listener != null) {
            listener.onSubredditSelected(selectedSubreddit);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.sr_action_menu, menu);
        if (mode.getTag() == null) {
            mode.setTag(new CheckedInfo());
        }
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean isQuery = query != null;
        CheckedInfo info = (CheckedInfo) mode.getTag();
        menu.findItem(R.id.menu_add).setVisible(isQuery);
        menu.findItem(R.id.menu_add_combined).setVisible(isQuery && info.checkedCount > 1);
        menu.findItem(R.id.menu_combine).setVisible(!isQuery
                && TextUtils.isEmpty(getAccountName())
                && info.checkedCount > 1);
        menu.findItem(R.id.menu_split).setVisible(!isQuery
                && TextUtils.isEmpty(getAccountName())
                && info.checkedCount == 1
                && info.numSplittable > 0);
        menu.findItem(R.id.menu_delete).setVisible(!isQuery);
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        CheckedInfo info = (CheckedInfo) mode.getTag();
        info.checkedCount = getListView().getCheckedItemCount();
        if (adapter.getName(position).indexOf('+') != -1) {
            info.numSplittable = info.numSplittable + (checked ? 1 : -1);
        }
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleActionAdd(mode);
                return true;

            case R.id.menu_add_combined:
                handleActionAddCombined(mode);
                return true;

            case R.id.menu_combine:
                handleCombine(mode);
                return true;

            case R.id.menu_split:
                handleSplit(mode);
                return true;

            case R.id.menu_delete:
                handleDelete(mode);
                return true;

            default:
                return false;
        }
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    static class CheckedInfo {
        int checkedCount;
        int numSplittable;
    }

    private void handleActionAdd(ActionMode mode) {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        ContentValues[] values = new ContentValues[positions.size()];
        int count = adapter.getCount();
        int i, j;
        for (i = 0, j = 0; i < count; i++) {
            if (positions.get(i)) {
                values[j] = new ContentValues(1);
                values[j++].put(Subreddits.COLUMN_NAME, adapter.getName(i));
            }
        }
        Provider.addMultipleSubredditsInBackground(getActivity(), values);
        mode.finish();
    }

    private void handleActionAddCombined(ActionMode mode) {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        ArrayList<String> names = new ArrayList<String>();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (positions.get(i)) {
                String name = adapter.getName(i);
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        Provider.combineSubredditsInBackground(getActivity(), names, null);
        mode.finish();
    }

    private void handleCombine(ActionMode mode) {
        ArrayList<String> names = getCheckedNames();
        int size = names.size();
        if (size <= 1) {
            mode.finish();
            return;
        }

        long[] ids = getListView().getCheckedItemIds();
        Provider.combineSubredditsInBackground(getActivity(), names, ids);
        mode.finish();
    }

    private void handleSplit(ActionMode mode) {
        ArrayList<String> names = getCheckedNames();
        long[] ids = getListView().getCheckedItemIds();
        Provider.splitSubredditsInBackground(getActivity(), names.get(0), ids[0]);
        mode.finish();
    }

    private void handleDelete(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        Provider.deleteSubredditInBackground(getActivity(), getAccountName(), ids);
        mode.finish();
    }

    private ArrayList<String> getCheckedNames() {
        int checkedCount = getListView().getCheckedItemCount();
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        ArrayList<String> names = new ArrayList<String>(checkedCount);
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (checked.get(i)) {
                String name = adapter.getName(i);
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACCOUNT_NAME, accountName);
        outState.putString(STATE_SELECTED_SUBREDDIT, selectedSubreddit);
        outState.putString(STATE_SESSION_ID, sessionId);
    }

    @Override
    public void onDestroy() {
        // Only search queries will have sessions but its ok to always do this.
        // TODO: Remove deleteSessionData method duplication in adapters.
        if (!getActivity().isChangingConfigurations()) {
            SubredditAdapter.deleteSessionData(getActivity(), sessionId, query);
        }
        super.onDestroy();
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setSelectedSubreddit(String subreddit) {
        selectedSubreddit = subreddit;
        adapter.setSelectedSubreddit(subreddit);
    }

    public String getSelectedSubreddit() {
        return selectedSubreddit;
    }

    public String getQuery() {
        return query;
    }
}
