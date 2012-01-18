package com.btmura.android.reddit;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class ThingCommentsFragment extends ListFragment implements ThingFragment {

	private static final String TAG = "ThingCommentsFragment";

	private static final String STATE_THING = "thing";
	private static final String STATE_THING_POSITION = "thingPosition";

	private ThingPartAdapter adapter;
	private ThingCommentsTask task;	
	private OnThingPartSelectedListener listener;
	
	private Thing thing;
	private int thingPosition;
	
	
	public void setThing(Thing thing, int position) {
		this.thing = thing;
		this.thingPosition = position;
	}
	
	public Thing getThing() {
		return thing;
	}
	
	public int getThingPosition() {
		return thingPosition;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			thing = savedInstanceState.getParcelable(STATE_THING);
			thingPosition = savedInstanceState.getInt(STATE_THING_POSITION);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable(STATE_THING, thing);
		outState.putInt(STATE_THING_POSITION, thingPosition);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (OnThingPartSelectedListener) activity;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		View view = super.onCreateView(inflater, container, savedInstanceState);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		adapter = new ThingPartAdapter(getActivity());
		setListAdapter(adapter);
		updateThread();
	}
		
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		ThingPart part = adapter.getItem(position);
		listener.onThingPartSelected(getThing(), getThingPosition(), part);
	}
	
	private void updateThread() {
		if (task != null) {
			task.cancel(true);
		}
		task = new ThingCommentsTask(this, adapter);
		task.execute(getThing());
	}
	
	@Override
	public void onStop() {
		Log.v(TAG, "onStop");
		super.onStop();
		if (task != null) {
			task.cancel(true);
		}
	}
}
