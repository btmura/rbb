package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class TopicListFragment extends ListFragment {
	
	@SuppressWarnings("unused")
	private static final String TAG = "TopicListFragment";
	
	private static final String STATE_POSITION = "position";
	
	interface OnTopicSelectedListener {
		void onTopicSelected(Topic topic, int position);
	}
	
	private TopicAdapter adapter;
	private OnTopicSelectedListener listener;
	private int position;

	public static TopicListFragment newInstance(int position) {
		TopicListFragment frag = new TopicListFragment();
		frag.position = position;
		return frag;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnTopicSelectedListener) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		adapter = new TopicAdapter(getActivity());
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt(STATE_POSITION);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		LayoutInfo info = (LayoutInfo) getActivity();
		boolean singleChoice = info.hasThingListContainer();
		
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
		listener.onTopicSelected(adapter.getItem(position), position);
	}
	
	public void setItemChecked(int position) {
		if (position < 0) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
}
