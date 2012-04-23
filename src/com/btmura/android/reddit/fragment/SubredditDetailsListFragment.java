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
import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Loader;
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
import com.btmura.android.reddit.content.SubredditDetailsLoader;
import com.btmura.android.reddit.entity.SubredditDetails;
import com.btmura.android.reddit.widget.SubredditDetailsAdapter;

public class SubredditDetailsListFragment extends ListFragment implements
        MultiChoiceModeListener,
        LoaderCallbacks<List<SubredditDetails>> {

    public static final String TAG = "SubredditDetailsListFragment";

    public interface OnSubredditDetailsSelectedListener {
        void onSubredditDetailsSelected(SubredditDetails details, int position);
    }

    private static final String ARG_QUERY = "q";
    private static final String ARG_SINGLE_CHOICE = "s";

    private static final String STATE_CHOSEN = "c";

    private SubredditDetailsAdapter adapter;

    public static SubredditDetailsListFragment newInstance(String query, boolean singleChoice) {
        SubredditDetailsListFragment frag = new SubredditDetailsListFragment();
        Bundle args = new Bundle(2);
        args.putString(ARG_QUERY, query);
        args.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SubredditDetailsAdapter(getActivity(), isSingleChoice());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter.setChosenPosition(savedInstanceState != null ? savedInstanceState
                .getInt(STATE_CHOSEN) : -1);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        adapter.setChosenPosition(position);
        getListener().onSubredditDetailsSelected(adapter.getItem(position), position);
    }

    public Loader<List<SubredditDetails>> onCreateLoader(int id, Bundle args) {
        return new SubredditDetailsLoader(getActivity(), getArguments().getString(ARG_QUERY));
    }

    public void onLoadFinished(Loader<List<SubredditDetails>> loader,
            final List<SubredditDetails> data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<List<SubredditDetails>> loader) {
        adapter.swapData(null);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.sr_action_menu, menu);
        menu.findItem(R.id.menu_combine).setVisible(false);
        menu.findItem(R.id.menu_split).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(false);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.findItem(R.id.menu_add_combined).setVisible(getListView().getCheckedItemCount() > 1);
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleActionAdd(mode);
                return true;

            case R.id.menu_add_combined:
                handleActionAddCombined(mode);
                return true;
        }
        return false;
    }

    private void handleActionAdd(ActionMode mode) {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        ContentValues[] values = new ContentValues[positions.size()];
        int count = adapter.getCount();
        int i, j;
        for (i = 0, j = 0; i < count; i++) {
            if (positions.get(i)) {
                values[j] = new ContentValues(1);
                values[j++].put(Subreddits.COLUMN_NAME, adapter.getItem(i).displayName);
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
                String name = adapter.getItem(i).displayName;
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }
        Provider.combineSubredditsInBackground(getActivity(), names, new long[0]);
        mode.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CHOSEN, adapter.getChosenPosition());
    }

    public void setChosenPosition(int position) {
        if (position != adapter.getChosenPosition()) {
            adapter.setChosenPosition(position);
            adapter.notifyDataSetChanged();
        }
    }

    private OnSubredditDetailsSelectedListener getListener() {
        return (OnSubredditDetailsSelectedListener) getActivity();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    private boolean isSingleChoice() {
        return getArguments().getBoolean(ARG_SINGLE_CHOICE, false);
    }
}
