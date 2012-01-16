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

public class MainActivity extends Activity {

	private static final String TOPIC_LIST_FRAG = "topicList";

	private static final String THING_LIST_FRAG = "thingList";

	private static final String THING_FRAG_TAG = "thing";

	private static final String TAG = "MainActivity";
	
	private FragmentManager manager;

	private View thingContainer;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
 
        thingContainer = findViewById(R.id.thing_container);
        
        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(new OnBackStackChangedListener() {
        	public void onBackStackChanged() {
        		refreshBackStackChanges();
        	}
        });
        
        TopicListFragment topicFrag = (TopicListFragment) manager.findFragmentById(R.id.topic_list);
        topicFrag.setOnTopicSelectedListener(new OnTopicSelectedListener() {
        	public void onTopicSelected(Topic topic, int position) {
        		replaceThingListFragment(topic, position, true);
        	}
        });
    
        replaceThingListFragment(Topic.frontPage(), 0, false);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
	}
	
	private void replaceThingListFragment(Topic topic, int position, boolean addToBackStack) {
		Log.v(TAG, "replaceThingListFragment");
		FragmentTransaction transaction = manager.beginTransaction();
		
		Fragment thingFrag = manager.findFragmentByTag(THING_FRAG_TAG);
		if (thingFrag != null) {
			transaction.hide(thingFrag);
		}
		
		ThingListFragment thingListFrag = new ThingListFragment();
		thingListFrag.setTopic(topic, position);
		thingListFrag.setOnThingSelectedListener(new OnThingSelectedListener() {
			public void onThingSelected(Thing thing, int position) {
				replaceThingFragment(thing, position);
			}
		});
		
		transaction.replace(R.id.thing_list_container, thingListFrag, THING_LIST_FRAG);
		if (addToBackStack) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
	}
	
	private void replaceThingFragment(Thing thing, int position) {
		Log.v(TAG, "replaceThingFragment");
		FragmentTransaction transaction = manager.beginTransaction();
		
		ThingFragment thingFragment = new ThingFragment();
		thingFragment.setThing(thing, position);
		
		transaction.replace(R.id.thing_container, thingFragment, THING_FRAG_TAG);
		transaction.addToBackStack(null);
		transaction.commit();		
	}
	
	private void refreshBackStackChanges() {
		TopicListFragment topicFrag = (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_FRAG);
		ThingListFragment thingListFrag = (ThingListFragment) manager.findFragmentByTag(THING_LIST_FRAG);
		ThingFragment thingFrag = (ThingFragment) manager.findFragmentByTag(THING_FRAG_TAG);

		thingContainer.setVisibility(thingFrag != null && thingFrag.isVisible() ? View.VISIBLE : View.GONE);
		
		int topicPosition = thingListFrag.getTopicPosition();
		topicFrag.setTopicSelected(topicPosition);
		
		int thingPosition = thingFrag != null && thingFrag.isVisible() ? thingFrag.getThingPosition() : -1;
		thingListFrag.setThingSelected(thingPosition);
	}
}