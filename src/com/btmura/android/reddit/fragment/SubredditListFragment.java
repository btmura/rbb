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

package com.btmura.android.reddit.fragment;

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
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.widget.SubredditAdapter;

public class SubredditListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    public static final String TAG = "SubredditListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_SELECTED_SUBREDDIT = "ss";
    private static final String ARG_QUERY = "q";
    private static final String ARG_FLAGS = "f";

    private static final String STATE_SESSION_ID = "sessionId";

    public interface OnSubredditSelectedListener {
        void onInitialSubredditSelected(Subreddit subreddit);

        void onSubredditSelected(Subreddit subreddit);
    }

    private String accountName;
    private String sessionId;
    private Subreddit selectedSubreddit;
    private String query;
    private int flags;
    private boolean sync;
    private SubredditAdapter adapter;
    private OnSubredditSelectedListener listener;

    public static SubredditListFragment newInstance(String accountName,
            Subreddit selectedSubreddit, String query, int flags) {
        Bundle args = new Bundle(4);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putParcelable(ARG_SELECTED_SUBREDDIT, selectedSubreddit);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);

        SubredditListFragment frag = new SubredditListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnSubredditSelectedListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
        accountName = bundle.getString(ARG_ACCOUNT_NAME);
        selectedSubreddit = bundle.getParcelable(ARG_SELECTED_SUBREDDIT);
        query = bundle.getString(ARG_QUERY);
        flags = bundle.getInt(ARG_FLAGS);
        sync = savedInstanceState == null;

        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getString(STATE_SESSION_ID);
        } else if (!TextUtils.isEmpty(query)) {
            sessionId = query + "-" + System.currentTimeMillis();
        }

        adapter = new SubredditAdapter(getActivity(), query, isSingleChoice());
        adapter.setSelectedSubreddit(Subreddit.getName(selectedSubreddit));
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
        setListShown(false);
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
            Subreddit sr = Subreddit.newInstance(adapter.getName(0));
            listener.onInitialSubredditSelected(sr);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        super.onListItemClick(l, view, position, id);
        String subreddit = adapter.getName(position);
        Subreddit sr = Subreddit.newInstance(subreddit);
        selectedSubreddit = sr;
        adapter.setSelectedSubreddit(subreddit);
        listener.onSubredditSelected(sr);
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
        CheckedInfo info = (CheckedInfo) mode.getTag();
        menu.findItem(R.id.menu_add).setVisible(isQuery());
        menu.findItem(R.id.menu_add_combined).setVisible(isQuery() && info.checkedCount > 1);
        menu.findItem(R.id.menu_combine).setVisible(!isQuery()
                && TextUtils.isEmpty(getAccountName())
                && info.checkedCount > 1);
        menu.findItem(R.id.menu_split).setVisible(!isQuery()
                && TextUtils.isEmpty(getAccountName())
                && info.checkedCount == 1
                && info.numSplittable > 0);
        menu.findItem(R.id.menu_delete).setVisible(!isQuery());
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
        SubredditProvider.addMultipleSubredditsInBackground(getActivity(), values);
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
        SubredditProvider.combineInBackground(getActivity(), names, null);
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
        SubredditProvider.combineInBackground(getActivity(), names, ids);
        mode.finish();
    }

    private void handleSplit(ActionMode mode) {
        ArrayList<String> names = getCheckedNames();
        long[] ids = getListView().getCheckedItemIds();
        SubredditProvider.splitInBackground(getActivity(), names.get(0), ids[0]);
        mode.finish();
    }

    private void handleDelete(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        SubredditProvider.deleteInBackground(getActivity(), getAccountName(), ids);
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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onSaveInstanceState an:" + accountName + " ss:" + selectedSubreddit
                    + " q:" + query + " f:" + flags);
        }
        outState.putString(ARG_ACCOUNT_NAME, accountName);
        outState.putParcelable(ARG_SELECTED_SUBREDDIT, selectedSubreddit);
        outState.putString(ARG_QUERY, query);
        outState.putInt(ARG_FLAGS, flags);
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setSelectedSubreddit(Subreddit subreddit) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setSelectedSubreddit subreddit:" + subreddit);
        }
        this.selectedSubreddit = subreddit;
        adapter.setSelectedSubreddit(subreddit.name);
        adapter.notifyDataSetChanged();
    }

    public Subreddit getSelectedSubreddit() {
        return selectedSubreddit;
    }

    public String getQuery() {
        return query;
    }

    private boolean isQuery() {
        return query != null;
    }

    private boolean isSingleChoice() {
        return Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);
    }
}
