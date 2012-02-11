package com.btmura.android.reddit;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class SubredditListFragment extends ListFragment implements LoaderCallbacks<List<Subreddit>> {
	
	private static final String ARG_SINGLE_CHOICE = "singleChoice";
	private static final String ARG_POSITION = "position";
	
	private static final String STATE_POSITION = "position";
	
	interface OnSubredditSelectedListener {
		void onSubredditSelected(Subreddit sr, int position);
	}

	private int position;
	private boolean singleChoice;
	
	private SubredditListAdapter adapter;
	private OnSubredditSelectedListener listener;

	public static SubredditListFragment newInstance(int position, boolean singleChoice) {
		SubredditListFragment frag = new SubredditListFragment();
		Bundle b = new Bundle(2);
		b.putInt(ARG_POSITION, position);
		b.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
		frag.setArguments(b);
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
		setRetainInstance(true);
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt(STATE_POSITION);
		} else {
			position = getArguments().getInt(ARG_POSITION);
		}
		singleChoice = getArguments().getBoolean(ARG_SINGLE_CHOICE);
		adapter = new SubredditListAdapter(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		list.setChoiceMode(singleChoice ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter(adapter);
		setItemChecked(position);
		getLoaderManager().initLoader(0, null, this);
	}
	
	public Loader<List<Subreddit>> onCreateLoader(int id, Bundle args) {
		return new SubredditLoader(getActivity());
	}
	
	public void onLoadFinished(Loader<List<Subreddit>> loader, List<Subreddit> data) {
		adapter.clear();
		adapter.addAll(data);
	}
	
	public void onLoaderReset(Loader<List<Subreddit>> loader) {
		adapter.clear();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_POSITION, position);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		this.position = position;
		listener.onSubredditSelected(adapter.getItem(position), position);
	}
	
	public void setItemChecked(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
}
