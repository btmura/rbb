package com.btmura.android.reddit.addsubreddits;

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

import com.btmura.android.reddit.R;

public class SubredditListFragment extends ListFragment implements MultiChoiceModeListener, LoaderCallbacks<List<SubredditInfo>> {

	interface OnSelectedListener {
		static final int EVENT_LIST_LOADED = 0;
		static final int EVENT_LIST_ITEM_CLICKED = 1;
		static final int EVENT_ACTION_ITEM_CLICKED = 2;
		void onSelected(List<SubredditInfo> srInfos, int position, int event);
	}
	
	private static final String ARG_QUERY = "q";
	private static final String ARG_SINGLE_CHOICE = "s";
	
	private static final String STATE_CHOSEN = "c";
	
	private SubredditInfoAdapter adapter;
	
	public static SubredditListFragment newInstance(String query, boolean singleChoice) {
		SubredditListFragment frag = new SubredditListFragment();
		Bundle args = new Bundle(2);
		args.putString(ARG_QUERY, query);
		args.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
		frag.setArguments(args);
		return frag;
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
		adapter = new SubredditInfoAdapter(getActivity().getLayoutInflater(), getArguments().getBoolean(ARG_SINGLE_CHOICE));
		adapter.setChosenPosition(savedInstanceState != null ? savedInstanceState.getInt(STATE_CHOSEN) : 0);
		setListAdapter(adapter);
		setEmptyText(getString(R.string.empty));
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
		return new SubredditLoader(getActivity(), getArguments().getString(ARG_QUERY));
	}
	
	public void onLoadFinished(Loader<List<SubredditInfo>> loader, final List<SubredditInfo> data) {
		adapter.clear();
		if (data != null) {
			setEmptyText(getString(R.string.empty));
			adapter.addAll(data);
		} else {
			setEmptyText(getString(R.string.error));
		}
		setListShown(true);
		if (!data.isEmpty()) {
			getListView().post(new Runnable() {
				public void run() {
					getListener().onSelected(data, 0, OnSelectedListener.EVENT_LIST_LOADED);
				}
			});
		}
	}
	
	public void onLoaderReset(Loader<List<SubredditInfo>> loader) {
		adapter.clear();
	}
	
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.subreddit, menu);
		menu.findItem(R.id.menu_delete_subreddits).setVisible(false);
		return true;
	}
	
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
	
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_subreddits:
			handleAddSubreddits();
			mode.finish();
			return true;
		}
		return false;
	}
	
	private void handleAddSubreddits() {
		final SparseBooleanArray positions = getListView().getCheckedItemPositions();
		List<SubredditInfo> added = new ArrayList<SubredditInfo>(positions.size());
		int count = adapter.getCount();
		for (int i = 0; i < count; i++) {
			if (positions.get(i)) {
				added.add(adapter.getItem(i));
			}
		}
		getListener().onSelected(added, -1, OnSelectedListener.EVENT_ACTION_ITEM_CLICKED);
	}
	
	private OnSelectedListener getListener() {
		return (OnSelectedListener) getActivity();
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
	
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
	}
	
	public void onDestroyActionMode(ActionMode mode) {
	}
}
