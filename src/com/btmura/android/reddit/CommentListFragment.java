package com.btmura.android.reddit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

public class CommentListFragment extends ListFragment implements TaskListener<ArrayList<Entity>> {

	private ThingHolder thingHolder;

	private EntityAdapter adapter;
	private CommentLoaderTask task;

	public static CommentListFragment newInstance() {
		return new CommentListFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		thingHolder = (ThingHolder) activity;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		loadComments();
	}
	
	private void loadComments() {
		if (adapter == null) {
			task = new CommentLoaderTask(getActivity().getApplicationContext(), this);
			task.execute(thingHolder.getThing());
		}
	}
	
	public void onPreExecute() {
	}
	
	public void onPostExecute(ArrayList<Entity> entities) {
		adapter = new EntityAdapter(getActivity(), entities);
		setEmptyText(getString(entities != null ? R.string.empty : R.string.error));
		setListAdapter(adapter);
	}
		
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (task != null) {
			task.cancel(true);
		}
	}
}
