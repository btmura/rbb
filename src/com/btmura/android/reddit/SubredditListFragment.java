package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

public class SubredditListFragment extends ListFragment implements LoaderCallbacks<Cursor>, MultiChoiceModeListener {
	
	private static final String ARGS_SINGLE_CHOICE = "singleChoice";

	interface OnSubredditSelectedListener {
		static final int FLAG_LOAD_FINISHED = 0;
		static final int FLAG_ITEM_CLICKED = 1;
		void onSubredditSelected(Subreddit sr, int event);
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
					Subreddit sr = adapter.getSubreddit(getActivity(), 0);
					listener.onSubredditSelected(sr, OnSubredditSelectedListener.FLAG_LOAD_FINISHED);
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
		Subreddit sr = adapter.getSubreddit(getActivity(), position);
		adapter.setSelectedSubreddit(sr);
		listener.onSubredditSelected(sr, OnSubredditSelectedListener.FLAG_ITEM_CLICKED);
	}
	
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.subreddit, menu);
		menu.findItem(R.id.menu_add_subreddits).setVisible(false);
		return true;
	}
	
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
	
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete_subreddits:
			long[] ids = getListView().getCheckedItemIds();
			Provider.deleteSubredditInBackground(getActivity(), ids);
			mode.finish();
			return true;
		}
		return false;
	}
	
	public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
	}
	
	public void onDestroyActionMode(ActionMode mode) {
	}
	
	public void setSelectedSubreddit(Subreddit subreddit) {
		adapter.setSelectedSubreddit(subreddit);
		adapter.notifyDataSetChanged();
	}
}
