package com.btmura.android.reddit;


import android.app.Fragment;
import android.os.Bundle;

public class ControlFragment extends Fragment {
	
	private static final String ARG_SUBREDDIT = "subreddit";
	private static final String ARG_THING = "thing";
	private static final String ARG_THING_POSITION = "thingPosition";
	private static final String ARG_FILTER = "filter";
	
	private Subreddit topic;
	private Entity thing;
	private int thingPosition;
	private int filter;
	
	public static ControlFragment newInstance(Subreddit sr, Entity thing, int thingPosition, int filter) {
		ControlFragment frag = new ControlFragment();
		Bundle b = new Bundle(4);
		b.putParcelable(ARG_SUBREDDIT, sr);
		b.putParcelable(ARG_THING, thing);
		b.putInt(ARG_THING_POSITION, thingPosition);
		b.putInt(ARG_FILTER, filter);
		frag.setArguments(b);
		return frag;
	}
	
	public Subreddit getTopic() {
		return topic;
	}
	
	public Entity getThing() {
		return thing;
	}

	public int getThingPosition() {
		return thingPosition;
	}
	
	public int getFilter() {
		return filter;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		topic = getArguments().getParcelable(ARG_SUBREDDIT);
		thing = getArguments().getParcelable(ARG_THING);
		thingPosition = getArguments().getInt(ARG_THING_POSITION);
		filter = getArguments().getInt(ARG_FILTER);
	}
}
