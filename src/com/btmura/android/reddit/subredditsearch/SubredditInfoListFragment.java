package com.btmura.android.reddit.subredditsearch;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
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
import com.btmura.android.reddit.R;

public class SubredditInfoListFragment extends ListFragment implements MultiChoiceModeListener,
        LoaderCallbacks<List<SubredditInfo>> {

    interface OnSelectedListener {
        static final int EVENT_LIST_ITEM_CLICKED = 0;
        static final int EVENT_ACTION_ITEM_CLICKED = 1;

        void onSelected(List<SubredditInfo> srInfos, int position, int event);
    }

    private static final String ARG_QUERY = "q";
    private static final String ARG_SINGLE_CHOICE = "s";

    private static final String STATE_CHOSEN = "c";

    private SubredditInfoAdapter adapter;

    public static SubredditInfoListFragment newInstance(String query, boolean singleChoice) {
        SubredditInfoListFragment frag = new SubredditInfoListFragment();
        Bundle args = new Bundle(2);
        args.putString(ARG_QUERY, query);
        args.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SubredditInfoAdapter(getActivity().getLayoutInflater(), getArguments()
                .getBoolean(ARG_SINGLE_CHOICE));
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
        ArrayList<SubredditInfo> subreddits = new ArrayList<SubredditInfo>(1);
        subreddits.add(adapter.getItem(position));
        getListener().onSelected(subreddits, position, OnSelectedListener.EVENT_LIST_ITEM_CLICKED);
    }

    public Loader<List<SubredditInfo>> onCreateLoader(int id, Bundle args) {
        return new SubredditInfoLoader(getActivity(), getArguments().getString(ARG_QUERY));
    }

    public void onLoadFinished(Loader<List<SubredditInfo>> loader, final List<SubredditInfo> data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<List<SubredditInfo>> loader) {
        adapter.swapData(null);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.subreddit, menu);
        menu.findItem(R.id.menu_split).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(false);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.findItem(R.id.menu_combine).setVisible(getListView().getCheckedItemCount() > 1);
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleAdd(mode);
                return true;

            case R.id.menu_combine:
                handleCombine(mode);
                return true;
        }
        return false;
    }

    private void handleAdd(ActionMode mode) {
        final SparseBooleanArray positions = getListView().getCheckedItemPositions();
        List<SubredditInfo> added = new ArrayList<SubredditInfo>(positions.size());
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (positions.get(i)) {
                added.add(adapter.getItem(i));
            }
        }
        getListener().onSelected(added, -1, OnSelectedListener.EVENT_ACTION_ITEM_CLICKED);
        mode.finish();
    }

    private void handleCombine(ActionMode mode) {
        ArrayList<String> names = new ArrayList<String>();
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (checked.get(i)) {
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

    private OnSelectedListener getListener() {
        return (OnSelectedListener) getActivity();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }
}
