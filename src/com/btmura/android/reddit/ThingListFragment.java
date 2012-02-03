package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.ThingLoaderTask.ThingLoaderResult;

public class ThingListFragment extends ListFragment implements OnScrollListener, TaskListener<ThingLoaderResult> {
	
	@SuppressWarnings("unused")
	private static final String TAG = "ThingListFragment";
	
	private OnThingSelectedListener listener;
	private TopicHolder topicHolder;
	private LayoutInfo layoutInfo;
	
	private EntityAdapter adapter;
	private ThingLoaderTask task;
	private String pendingAfter;
	
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
		list.setOnScrollListener(this);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setPosition();
		if (adapter == null) {
			loadThings(null);
		}
	}
	
	private void loadThings(String after) {
		Topic topic = topicHolder.getTopic().withTopic(after);
		task = new ThingLoaderTask(this, "all".equals(topic.title));
		task.execute(topic);
	}
	
	public void onPreExecute() {
	}
	
	public void onPostExecute(ThingLoaderResult result) {
		pendingAfter = result.after;
		if (adapter == null) {
			adapter = new EntityAdapter(result.entities, getActivity().getLayoutInflater());
			setListAdapter(adapter);
			setEmptyText(getString(result.entities != null ? R.string.empty : R.string.error));
		} else {
			if (result.entities != null) {
				removeProgressItem();
				adapter.addAll(result.entities);
			} else {
				updateProgressItem(R.string.loading_error, false);
			}
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Entity e = adapter.getItem(position);
		switch (e.type) {
		case Entity.TYPE_THING:
			listener.onThingSelected(adapter.getItem(position), position);
			break;
			
		case Entity.TYPE_MORE:
			if (task.getStatus() == Status.FINISHED) {
				updateProgressItem(R.string.loading, true);
				loadThings(e.after);
			}
			break;
		}
	}
	
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (totalItemCount <= 0) {
			return;
		}
		int lastVisibleItem = firstVisibleItem + visibleItemCount;
		if (lastVisibleItem >= totalItemCount) {
			if (pendingAfter != null) {
				addProgressItem(R.string.loading, true);
				loadThings(pendingAfter);
				pendingAfter = null;
			}
		}
	}
	
	private void addProgressItem(int text, boolean progress) {
		Entity e = new Entity();
		e.type = Entity.TYPE_MORE;
		e.line1 = new SpannedString(getString(text));
		e.progress = progress;
		e.after = pendingAfter;
		adapter.add(e);
	}
	
	private void updateProgressItem(int text, boolean progress) {
		Entity e = adapter.getItem(adapter.getCount() - 1);
		e.line1 = new SpannedString(getString(text));
		e.progress = progress;
		adapter.notifyDataSetChanged();
	}
	
	private void removeProgressItem() {
		adapter.remove(adapter.getCount() - 1);
	}
	
	public void onScrollStateChanged(AbsListView view, int scrollState) {
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

