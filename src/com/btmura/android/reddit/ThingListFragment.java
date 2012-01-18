package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment {

	interface OnThingSelectedListener {
		void onThingSelected(Thing thread, int position);
	}

	private static final String TAG = "ThingListFragment";
	
	private static final String TOPIC_STATE = "topic";
	private static final String TOPIC_POSITION_STATE = "topicPosition";
	private static final String SINGLE_CHOICE_STATE = "singleChoice";

	private ThingListAdapter adapter;
	private ThingListTask task;
	private OnThingSelectedListener listener;
	private boolean singleChoice;
	
	private Topic topic = Topic.frontPage();
	private int topicPosition;
	
	public void setTopic(Topic topic, int position) {
		this.topic = topic;
		this.topicPosition = position;
	}
	
	public Topic getTopic() {
		return topic;
	}
	
	public int getTopicPosition() {
		return topicPosition;
	}

	public void setSingleChoice(boolean singleChoice) {
		this.singleChoice = singleChoice;
	}
	
	public void setThingSelected(int position) {
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnThingSelectedListener) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			topic = savedInstanceState.getParcelable(TOPIC_STATE);
			topicPosition = savedInstanceState.getInt(TOPIC_POSITION_STATE);
			singleChoice = savedInstanceState.getBoolean(SINGLE_CHOICE_STATE);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
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
		setThingSelected(-1);
		updateThreadList();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable(TOPIC_STATE, topic);
		outState.putInt(TOPIC_POSITION_STATE, topicPosition);
		outState.putBoolean(SINGLE_CHOICE_STATE, singleChoice);
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
		task.execute(topic);
	}
}

