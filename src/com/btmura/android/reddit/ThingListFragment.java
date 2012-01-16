package com.btmura.android.reddit;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment {
	
	private static final String TAG = "ThreadListFragment";

	interface OnThingSelectedListener {
		void onThingSelected(Thing thread, int position);
	}

	private ThingListAdapter adapter;
	
	private ThingListTask task;
	
	private OnThingSelectedListener listener;
	
	private Topic topic = Topic.frontPage();
	
	private int topicPosition;
	
	public void setTopic(Topic topic, int position) {
		this.topic = topic;
		this.topicPosition = position;
	}
	
	public int getTopicPosition() {
		return topicPosition;
	}

	public void setOnThingSelectedListener(OnThingSelectedListener listener) {
		this.listener = listener;
	}
	
	public void setThingSelected(int position) {
		if (position < 0) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			topic = savedInstanceState.getParcelable("topic");
			topicPosition = savedInstanceState.getInt("topicPosition");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
		outState.putParcelable("topic", topic);
		outState.putInt("topicPosition", topicPosition);
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

