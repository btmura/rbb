package com.btmura.android.reddit;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ThingFragment extends ListFragment {
	
	private static final String TAG = "ThingFragment";

	private ThingAdapter adapter;
	
	private ThingTask task;
	
	private Thing thing;
	
	private int thingPosition;
	
	public void setThing(Thing thing, int position) {
		this.thing = thing;
		this.thingPosition = position;
	}
	
	public int getThingPosition() {
		return thingPosition;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			thing = savedInstanceState.getParcelable("thing");
			thingPosition = savedInstanceState.getInt("thingPosition");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		View view = super.onCreateView(inflater, container, savedInstanceState);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		adapter = new ThingAdapter(getActivity());
		setListAdapter(adapter);
		updateThread();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		outState.putParcelable("thing", thing);
		outState.putInt("thingPosition", thingPosition);
	}
	
	private void updateThread() {
		if (task != null) {
			task.cancel(true);
		}
		task = new ThingTask(this, adapter);
		task.execute(thing);
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

