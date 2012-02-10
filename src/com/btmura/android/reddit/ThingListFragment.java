package com.btmura.android.reddit;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends EntityListFragment<String> {
	
	private static final String ARG_TOPIC = "topic";
	private static final String ARG_FILTER = "filter";
	private static final String ARG_SINGLE_CHOICE = "singleChoice";
	
	private static final String STATE_POSITION = "position";
	
	private OnThingSelectedListener listener;

	private int position = ListView.INVALID_POSITION;

	interface OnThingSelectedListener {
		void onThingSelected(Entity thing, int position);
	}
	
	public static ThingListFragment newInstance(Topic topic, int filter, boolean singleChoice) {
		ThingListFragment frag = new ThingListFragment();
		Bundle b = new Bundle(3);
		b.putParcelable(ARG_TOPIC, topic);
		b.putInt(ARG_FILTER, filter);
		b.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
		frag.setArguments(b);
		return frag;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnThingSelectedListener) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		boolean singleChoice = getArguments().getBoolean(ARG_SINGLE_CHOICE);
		list.setChoiceMode(singleChoice ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		list.setOnScrollListener(this);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null) {
			position = savedInstanceState.getInt(STATE_POSITION);
		}
		setItemChecked(position);
		getListView().setSelection(position);
	}
	
	@Override
	protected AsyncTask<Void, Void, LoadResult<String>> createLoadTask(String moreKey) {
		Topic t = getArguments().getParcelable(ARG_TOPIC);
		int filter = getArguments().getInt(ARG_FILTER);
		CharSequence url = Urls.subredditUrl(t.name, filter, moreKey);
		return new ThingLoaderTask(getActivity().getApplicationContext(), url, this, "all".equals(t.name));
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
		
		Entity e = adapter.getItem(position);
		switch (e.type) {
		case Entity.TYPE_THING:
			listener.onThingSelected(adapter.getItem(position), position);
			break;
		}
	}

	public void setItemChecked(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
}

