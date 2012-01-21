package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment {

	interface OnThingSelectedListener {
		void onThingSelected(Thing thread, int position);
	}

	private static final String TAG = "ThingListFragment";

	private static final String STATE_POSITION = "position";

	private ThingListAdapter adapter;
	private ThingListTask task;
	private OnThingSelectedListener listener;
	
	private int position;
	
	private TopicHolder topicHolder;
	
	public static ThingListFragment newInstance() {
		return new ThingListFragment();
	}
	
	@Override
	public void onAttach(Activity activity) {
		Log.v(TAG, "onAttach");
		super.onAttach(activity);
		listener = (OnThingSelectedListener) activity;
		topicHolder = (TopicHolder) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt(STATE_POSITION);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		
		LayoutInfo info = (LayoutInfo) getActivity();
		boolean singleChoice = info.hasThingContainer();
		setHasOptionsMenu(!info.hasTopicListContainer());
		
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		list.setChoiceMode(singleChoice ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		adapter = new ThingListAdapter(getActivity());
		setListAdapter(adapter);
		setListShown(false);
		setItemChecked(-1);
		updateThreadList();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_POSITION, position);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listener.onThingSelected(adapter.getItem(position), position);
	}
	
	@Override
	public void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
		if (task != null) {
			task.cancel(true);
		}
	}
	
	private void updateThreadList() {
		if (task != null) {
			task.cancel(true);
		}
		task = new ThingListTask(this, adapter);
		task.execute(topicHolder.getTopic());
	}
	
	public void setItemChecked(int position) {
		ListView list = getListView();
		if (position < 0) {
			Log.v(TAG, "Clearing choices");
			list.clearChoices();
		} else {
			Log.v(TAG, "Setting position");
			list.setItemChecked(position, true);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.main, menu);
	}
}

