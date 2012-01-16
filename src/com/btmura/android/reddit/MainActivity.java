package com.btmura.android.reddit;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.TopicListFragment.OnTopicSelectedListener;

public class MainActivity extends Activity implements OnBackStackChangedListener,
		OnTopicSelectedListener, OnThingSelectedListener {

	private static final String TAG = "MainActivity";
	
	private static final String TOPIC_LIST_TAG = "topicList";
	private static final String THING_LIST_TAG = "thingList";
	private static final String THING_TAG = "thing";

	private FragmentManager manager;

	private View thingContainer;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
 
        thingContainer = findViewById(R.id.thing_container);
        
        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);
        
        if (savedInstanceState != null) {
        	Log.v(TAG, "savedInstanceState found");
        	restoreState(savedInstanceState);
        } else {
        	insertTopicListFragment();
	        replaceThingListFragment(Topic.frontPage(), 0, false);
        }
	}
	
	private void restoreState(Bundle savedInstanceState) {
		Log.v(TAG, "restoreState");
    	TopicListFragment topicFrag = (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
    	if (topicFrag != null) {
    		Log.v(TAG, "setting topic listener");
            topicFrag.setOnTopicSelectedListener(this);
    	}
    	
    	ThingListFragment thingListFrag = (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);        	
    	if (thingListFrag != null) {
    		Log.v(TAG, "setting thingList listener");
    		thingListFrag.setOnThingSelectedListener(this);
    	}
    	
    	ThingFragment thingFrag = (ThingFragment) manager.findFragmentByTag(THING_TAG);
    	thingContainer.setVisibility(thingFrag != null ? View.VISIBLE : View.GONE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}
	
	private void insertTopicListFragment() {
		FragmentTransaction transaction = manager.beginTransaction();
		TopicListFragment topicFrag = new TopicListFragment();
        topicFrag.setOnTopicSelectedListener(this);
        transaction.replace(R.id.topic_list_container, topicFrag, TOPIC_LIST_TAG);
		transaction.commit();
	}
	
	public void onTopicSelected(Topic topic, int position) {
		replaceThingListFragment(topic, position, true);
	}
	
	private void replaceThingListFragment(Topic topic, int position, boolean addToBackStack) {
		Log.v(TAG, "replaceThingListFragment");
		FragmentTransaction transaction = manager.beginTransaction();
		
		Fragment thingFrag = manager.findFragmentByTag(THING_TAG);
		if (thingFrag != null) {
			transaction.remove(thingFrag);
		}
		thingContainer.setVisibility(thingFrag != null ? View.VISIBLE : View.GONE);
		
		ThingListFragment thingListFrag = new ThingListFragment();
		thingListFrag.setTopic(topic, position);
		thingListFrag.setOnThingSelectedListener(this);
		
		transaction.replace(R.id.thing_list_container, thingListFrag, THING_LIST_TAG);
		if (addToBackStack) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
	}
	
	public void onThingSelected(Thing thing, int position) {
		replaceThingFragment(thing, position);
	}
	
	private void replaceThingFragment(Thing thing, int position) {
		Log.v(TAG, "replaceThingFragment");
		FragmentTransaction transaction = manager.beginTransaction();
		
		ThingFragment thingFragment = new ThingFragment();
		thingFragment.setThing(thing, position);
		
		transaction.replace(R.id.thing_container, thingFragment, THING_TAG);
		transaction.addToBackStack(null);
		transaction.commit();		
	}
	
	public void onBackStackChanged() {
		Log.v(TAG, "onBackStackChanged");
		
		TopicListFragment topicFrag = (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
		ThingListFragment thingListFrag = (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);
		ThingFragment thingFrag = (ThingFragment) manager.findFragmentByTag(THING_TAG);

		thingContainer.setVisibility(thingFrag != null ? View.VISIBLE : View.GONE);
		
		int topicPosition = thingListFrag.getTopicPosition();
		topicFrag.setTopicSelected(topicPosition);
		
		int thingPosition = thingFrag != null ? thingFrag.getThingPosition() : -1;
		thingListFrag.setThingSelected(thingPosition);
	}
}