package com.btmura.android.reddit;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

public class CommentListFragment extends ListFragment implements TaskListener<List<Comment>> {

	private ThingHolder thingHolder;

	private CommentAdapter adapter;
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		loadComments();
	}
	
	private void loadComments() {
		if (adapter == null) {
			task = new CommentLoaderTask(this);
			task.execute(thingHolder.getThing());
		}
	}
	
	public void onPreExecute() {
	}
	
	public void onPostExecute(List<Comment> comments) {
		adapter = new CommentAdapter(getActivity());
		if (comments != null) {
			adapter.addAll(comments);
		}
		setEmptyText(getString(comments != null ? R.string.empty : R.string.error));
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
