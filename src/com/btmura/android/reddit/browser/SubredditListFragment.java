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

package com.btmura.android.reddit.browser;

import java.util.ArrayList;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.R;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
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

public class SubredditListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    private static final String ARGS_SINGLE_CHOICE = "singleChoice";

    public interface OnSubredditSelectedListener {
        void onSubredditLoaded(Subreddit sr);

        void onSubredditSelected(Subreddit s);
    }

    private SubredditAdapter adapter;
    private OnSubredditSelectedListener listener;

    public static SubredditListFragment newInstance(boolean singleChoice) {
        SubredditListFragment frag = new SubredditListFragment();
        Bundle args = new Bundle(1);
        args.putBoolean(ARGS_SINGLE_CHOICE, singleChoice);
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
        adapter = new SubredditAdapter(getActivity(), getArguments().getBoolean(ARGS_SINGLE_CHOICE));
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
        return SubredditAdapter.createLoader(getActivity());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
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

    class CheckedInfo {
        int checkedCount;
        int numSplittable;
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.subreddit, menu);
        menu.findItem(R.id.menu_add).setVisible(false);
        if (mode.getTag() == null) {
            mode.setTag(new CheckedInfo());
        }
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        CheckedInfo info = (CheckedInfo) mode.getTag();
        menu.findItem(R.id.menu_add_combined).setVisible(false);
        menu.findItem(R.id.menu_combine).setVisible(info.checkedCount > 1);
        menu.findItem(R.id.menu_split).setVisible(info.checkedCount == 1 && info.numSplittable > 0);
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_combine:
                handleCombined(mode);
                return true;

            case R.id.menu_split:
                handleSplit(mode);
                return true;

            case R.id.menu_delete:
                handleDelete(mode);
                return true;
        }
        return false;
    }

    private void handleCombined(ActionMode mode) {
        ArrayList<String> names = getCheckedNames();
        int size = names.size();
        if (size <= 1) {
            mode.finish();
            return;
        }

        long[] ids = getListView().getCheckedItemIds();

        Provider.combineSubredditsInBackground(getActivity().getApplicationContext(), names, ids);
        mode.finish();
    }

    private void handleSplit(ActionMode mode) {
        ArrayList<String> names = getCheckedNames();
        long[] ids = getListView().getCheckedItemIds();
        Provider.splitSubredditInBackground(getActivity().getApplicationContext(), names.get(0),
                ids[0]);
        mode.finish();
    }

    private void handleDelete(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        Provider.deleteSubredditInBackground(getActivity().getApplicationContext(), ids);
        mode.finish();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        CheckedInfo info = (CheckedInfo) mode.getTag();
        info.checkedCount = getListView().getCheckedItemCount();
        if (adapter.getName(getActivity(), position).indexOf('+') != -1) {
            info.numSplittable = info.numSplittable + (checked ? 1 : -1);
        }
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    public void setSelectedSubreddit(Subreddit subreddit) {
        adapter.setSelectedSubreddit(subreddit);
        adapter.notifyDataSetChanged();
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
}
