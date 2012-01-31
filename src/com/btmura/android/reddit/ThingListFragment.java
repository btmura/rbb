package com.btmura.android.reddit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingListFragment extends ListFragment {
	
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
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
		loadThings(null);
	}
	
	private void loadThings(String after) {
		if (adapter == null || after != null) {
			task = new ThingLoaderTask(after == null ? new InitialLoadListener() : new LoadMoreListener());
			task.execute(topicHolder.getTopic().withTopic(after));	
		}
	}
	
	class InitialLoadListener implements TaskListener<ArrayList<Entity>> {
		public void onPreExecute() {
		}
		
		public void onPostExecute(ArrayList<Entity> things) {
			adapter = new EntityAdapter(getActivity(), things);
			setEmptyText(getString(things != null ? R.string.empty : R.string.error));
			setListAdapter(adapter);
			setPosition();
		}
	}
	
	class LoadMoreListener implements TaskListener<ArrayList<Entity>> {
		public void onPreExecute() {
		}
		
		public void onPostExecute(ArrayList<Entity> things) {
			adapter.remove(adapter.getCount() - 1);
			adapter.addAll(things);
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Entity e = adapter.getItem(position);
		switch (e.type) {
		case Entity.TYPE_TITLE:
			listener.onThingSelected(e, position);
			break;
			
		case Entity.TYPE_MORE:
			v.findViewById(R.id.progress).setVisibility(View.VISIBLE);
			loadThings(e.after);
			break;
		}
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

