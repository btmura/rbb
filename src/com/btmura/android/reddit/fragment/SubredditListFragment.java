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

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.widget.SubredditAdapter;

public class SubredditListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    public static final String TAG = "SubredditListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_SELECTED_SUBREDDIT = "s";
    private static final String ARG_QUERY = "q";
    private static final String ARG_FLAGS = "f";

    public interface OnSubredditSelectedListener {
        void onSubredditLoaded(Subreddit subreddit);

        void onSubredditSelected(Subreddit subreddit);
    }

    private SubredditAdapter adapter;
    private OnSubredditSelectedListener listener;

    public static SubredditListFragment newInstance(Subreddit selectedSubreddit, int flags) {
        SubredditListFragment f = new SubredditListFragment();
        Bundle args = new Bundle(2);
        args.putParcelable(ARG_SELECTED_SUBREDDIT, selectedSubreddit);
        args.putInt(ARG_FLAGS, flags);
        f.setArguments(args);
        return f;
    }

    public static SubredditListFragment newSearchInstance(String query, int flags) {
        SubredditListFragment f = new SubredditListFragment();
        Bundle args = new Bundle(2);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnSubredditSelectedListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SubredditAdapter(getActivity(), getQuery(), isSingleChoice());
        adapter.setSelectedSubreddit(getSelectedSubreddit());
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
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return SubredditAdapter.createLoader(getActivity().getApplicationContext(), getQuery());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        setEmptyText(getString(data != null ? R.string.empty : R.string.error));
        setListShown(true);
        if (data.getCount() > 0) {
            getListView().post(new Runnable() {
                public void run() {
                    Subreddit sr = Subreddit.newInstance(adapter.getName(getActivity(), 0));
                    listener.onSubredditLoaded(sr);
                }
            });
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Subreddit sr = Subreddit.newInstance(adapter.getName(getActivity(), position));
        adapter.setSelectedSubreddit(sr);
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
        menu.findItem(R.id.menu_combine).setVisible(!isQuery() && info.checkedCount > 1);
        menu.findItem(R.id.menu_split).setVisible(
                !isQuery() && info.checkedCount == 1 && info.numSplittable > 0);
        menu.findItem(R.id.menu_delete).setVisible(!isQuery());
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        CheckedInfo info = (CheckedInfo) mode.getTag();
        info.checkedCount = getListView().getCheckedItemCount();
        if (adapter.getName(getActivity(), position).indexOf('+') != -1) {
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
                values[j++].put(Subreddits.COLUMN_NAME, adapter.getName(getActivity(), i));
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
                String name = adapter.getName(getActivity(), i);
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        Provider.combineSubredditsInBackground(getActivity(), names, new long[0]);
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
        Provider.splitSubredditInBackground(getActivity(), names.get(0), ids[0]);
        mode.finish();
    }

    private void handleDelete(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        Provider.deleteInBackground(getActivity(), Subreddits.CONTENT_URI, ids);
        mode.finish();
    }

    private ArrayList<String> getCheckedNames() {
        int checkedCount = getListView().getCheckedItemCount();
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        ArrayList<String> names = new ArrayList<String>(checkedCount);
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (checked.get(i)) {
                String name = adapter.getName(getActivity(), i);
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public void setSelectedSubreddit(Subreddit subreddit) {
        adapter.setSelectedSubreddit(subreddit);
        adapter.notifyDataSetChanged();
    }

    private Subreddit getSelectedSubreddit() {
        return getArguments().getParcelable(ARG_SELECTED_SUBREDDIT);
    }

    private String getQuery() {
        return getArguments().getString(ARG_QUERY);
    }

    private boolean isQuery() {
        return getQuery() != null;
    }

    private int getFlags() {
        return getArguments().getInt(ARG_FLAGS);
    }

    private boolean isSingleChoice() {
        return Flag.isEnabled(getFlags(), FLAG_SINGLE_CHOICE);
    }
}
