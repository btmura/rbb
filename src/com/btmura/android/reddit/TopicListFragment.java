package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class TopicListFragment extends ListFragment {
	
	private static final String ARG_SINGLE_CHOICE = "singleChoice";
	private static final String ARG_POSITION = "position";
	
	private static final String STATE_POSITION = "position";
	
	interface OnTopicSelectedListener {
		void onTopicSelected(Topic topic, int position);
	}

	private int position;
	private boolean singleChoice;
	
	private TopicAdapter adapter;
	private OnTopicSelectedListener listener;

	public static TopicListFragment newInstance(int position, boolean singleChoice) {
		TopicListFragment frag = new TopicListFragment();
		Bundle b = new Bundle(2);
		b.putInt(ARG_POSITION, position);
		b.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
		frag.setArguments(b);
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
		position = getArguments().getInt(ARG_POSITION);
		singleChoice = getArguments().getBoolean(ARG_SINGLE_CHOICE);
		adapter = new TopicAdapter(getActivity());
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
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt(STATE_POSITION);
		}
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
		if (position == ListView.INVALID_POSITION) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
}
