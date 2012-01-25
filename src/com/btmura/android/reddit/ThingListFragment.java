package com.btmura.android.reddit;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment implements TaskListener<List<Thing>> {
	
	private OnThingSelectedListener listener;
	private TopicHolder topicHolder;
	private LayoutInfo layoutInfo;
	
	private ThingAdapter adapter;
	private ThingLoaderTask task;

	interface OnThingSelectedListener {
		void onThingSelected(Thing thing, int position);
	}
	
	public static ThingListFragment newInstance() {
		return new ThingListFragment();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnThingSelectedListener) activity;
		topicHolder = (TopicHolder) activity;
		layoutInfo = (LayoutInfo) activity;
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView list = (ListView) view.findViewById(android.R.id.list);
		list.setChoiceMode(layoutInfo.hasThingContainer() ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		loadThings();
	}
	
	private void loadThings() {
		if (adapter == null) {
			adapter = new ThingAdapter(getActivity());
		}
		if (task == null) {
			task = new ThingLoaderTask(this);
			task.execute(topicHolder.getTopic());	
		}
	}
	
	public void onPreExecute() {
		adapter.clear();
	}

	public void onPostExecute(List<Thing> things) {
		if (things != null) {
			adapter.addAll(things);
		}
		setEmptyText(getString(things != null ? R.string.empty : R.string.error));
		setListAdapter(adapter);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listener.onThingSelected(adapter.getItem(position), position);
	}
	
	public void setItemChecked(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (task != null) {
			task.cancel(true);	
		}
	}
}

