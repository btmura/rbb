package com.btmura.android.reddit;

import android.app.Fragment;
import android.os.Bundle;

public class ControlFragment extends Fragment {
	
	private static final String ARG_TOPIC = "topic";
	private static final String ARG_THING = "thing";
	private static final String ARG_TOPIC_POSITION = "topicPosition";
	private static final String ARG_THING_POSITION = "thingPosition";
	
	private Topic topic;
	private Entity thing;
	private int topicPosition;
	private int thingPosition;
	
	public static ControlFragment newInstance(Topic topic, int topicPosition, Entity thing, int thingPosition) {
		ControlFragment frag = new ControlFragment();
		Bundle b = new Bundle(4);
		b.putParcelable(ARG_TOPIC, topic);
		b.putParcelable(ARG_THING, thing);
		b.putInt(ARG_TOPIC_POSITION, topicPosition);
		b.putInt(ARG_THING_POSITION, thingPosition);
		frag.setArguments(b);
		return frag;
	}
	
	public Topic getTopic() {
		return topic;
	}
	
	public Entity getThing() {
		return thing;
	}
	
	public int getTopicPosition() {
		return topicPosition;
	}
	
	public int getThingPosition() {
		return thingPosition;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		topic = getArguments().getParcelable(ARG_TOPIC);
		thing = getArguments().getParcelable(ARG_THING);
		topicPosition = getArguments().getInt(ARG_TOPIC_POSITION);
		thingPosition = getArguments().getInt(ARG_THING_POSITION);
	}
}
