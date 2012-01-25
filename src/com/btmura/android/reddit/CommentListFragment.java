package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;

public class CommentListFragment extends ListFragment implements TaskListener<Comment, Boolean> {

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
			adapter = new CommentAdapter(getActivity());
		}
		if (task == null) {
			task = new CommentLoaderTask(this);
			task.execute(thingHolder.getThing());
		}
	}
	
	public void onPreExecute() {
		adapter.clear();
	}
	
	public void onProgressUpdate(Comment[] comments) {
		adapter.addAll(comments);
		setListAdapter(adapter);
	}
	
	public void onPostExecute(Boolean success) {
		setEmptyText(getString(success ? R.string.empty : R.string.error));
		setListAdapter(adapter);
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (task != null) {
			task.cancel(true);
			task = null;
		}
		if (adapter != null) {
			adapter = null;
		}
	}
}
