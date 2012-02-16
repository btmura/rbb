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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.btmura.android.reddit.R;

public class SubredditListFragment extends ListFragment implements MultiChoiceModeListener, LoaderCallbacks<List<String>> {

	interface OnSubredditAddedListener {
		void onSubredditsAdded(ArrayList<String> subreddits);
	}
	
	private static final String ARG_QUERY = "query";
	
	private ArrayAdapter<String> adapter;
	
	public static SubredditListFragment newInstance(String query) {
		SubredditListFragment frag = new SubredditListFragment();
		Bundle args = new Bundle(1);
		args.putString(ARG_QUERY, query);
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
		adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_activated_1);
		setListAdapter(adapter);
		setEmptyText(getString(R.string.empty));
		setListShown(false);
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ArrayList<String> subreddits = new ArrayList<String>(1);
		subreddits.add(adapter.getItem(position));
		getListener().onSubredditsAdded(subreddits);
	}
	
	public Loader<List<String>> onCreateLoader(int id, Bundle args) {
		return new SubredditLoader(getActivity(), getArguments().getString(ARG_QUERY));
	}
	
	public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
		adapter.clear();
		adapter.addAll(data);
		setListShown(true);
	}
	
	public void onLoaderReset(Loader<List<String>> loader) {
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
		ArrayList<String> names = new ArrayList<String>();
		int count = adapter.getCount();
		for (int i = 0; i < count; i++) {
			if (positions.get(i)) {
				names.add(adapter.getItem(i));
			}
		}
		getListener().onSubredditsAdded(names);
	}
	
	private OnSubredditAddedListener getListener() {
		return (OnSubredditAddedListener) getActivity();
	}
	
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
	}
	
	public void onDestroyActionMode(ActionMode mode) {
	}
}
