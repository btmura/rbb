package com.btmura.android.reddit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment implements TaskListener<ArrayList<Entity>> {
	
	private OnThingSelectedListener listener;
	private TopicHolder topicHolder;
	private LayoutInfo layoutInfo;
	
	private EntityAdapter adapter;
	private ThingLoaderTask task;
	private int position = ListView.INVALID_POSITION;

	interface OnThingSelectedListener {
		void onThingSelected(Entity thing, int position);
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
			task = new ThingLoaderTask(this);
			task.execute(topicHolder.getTopic());	
		}
	}
	
	public void onPreExecute() {
	}

	public void onPostExecute(ArrayList<Entity> things) {
		adapter = new EntityAdapter(things, getActivity().getLayoutInflater());
		setEmptyText(getString(things != null ? R.string.empty : R.string.error));
		setListAdapter(adapter);
		setPosition();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		listener.onThingSelected(adapter.getItem(position), position);
	}
	
	public void setItemChecked(int position) {
		this.position = position;
	}
	
	private void setPosition() {
		if (position == ListView.INVALID_POSITION) {
			getListView().clearChoices();
		} else {
			getListView().setItemChecked(position, true);
			getListView().setSelection(position);
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (task != null) {
			task.cancel(true);
		}
	}
}

