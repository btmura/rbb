package com.btmura.android.reddit;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class TopicListFragment extends ListFragment {
	
	private static final String TAG = "TopicListFragment";
	
	interface OnTopicSelectedListener {
		void onTopicSelected(Topic topic, int position);
	}
	
	private TopicAdapter adapter;
	private OnTopicSelectedListener listener;
	
	public void setOnTopicSelectedListener(OnTopicSelectedListener listener) {
		this.listener = listener;
	}
	
	public void setTopicSelected(int position) {
		getListView().setItemChecked(position, true);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
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
		adapter = new TopicAdapter(getActivity());
		setListAdapter(adapter);
		setTopicSelected(0);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listener.onTopicSelected(adapter.getItem(position), position);
	}
}
