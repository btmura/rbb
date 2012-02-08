package com.btmura.android.reddit;

import java.util.ArrayList;

import android.app.ListFragment;
import android.os.Bundle;

public class CommentListFragment extends ListFragment implements TaskListener<ArrayList<Entity>> {

	private static final String ARG_THING = "thing";

	private EntityAdapter adapter;
	private CommentLoaderTask task;

	public static CommentListFragment newInstance(Entity thing) {
		CommentListFragment frag = new CommentListFragment();
		Bundle b = new Bundle(1);
		b.putParcelable(ARG_THING, thing);
		frag.setArguments(b);
		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		loadComments();
	}
	
	private void loadComments() {
		if (adapter == null) {
			task = new CommentLoaderTask(getActivity().getApplicationContext(), this);
			Entity thing = getArguments().getParcelable(ARG_THING);
			task.execute(thing);
		}
	}
	
	public void onPreExecute() {
	}
	
	public void onPostExecute(ArrayList<Entity> entities) {
		adapter = new EntityAdapter(entities, getActivity().getLayoutInflater());
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
