package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.btmura.android.reddit.ThingFragment.OnThingPartSelectedListener;
import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.TopicListFragment.OnTopicSelectedListener;

public class MainActivity extends Activity implements OnBackStackChangedListener,
		OnTopicSelectedListener, OnThingSelectedListener, OnThingPartSelectedListener {

	private static final String TAG = "MainActivity";
	
	private static final String TOPIC_LIST_TAG = "topicList";
	private static final String THING_LIST_TAG = "thingList";
	private static final String THING_TAG = "thing";

	private FragmentManager manager;

	private boolean hasTopicList;
	private boolean hasThingList;
	private boolean hasThing;

	private View thingContainer;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setCustomView(new FragmentBreadCrumbs(this));
 
		hasTopicList = findViewById(R.id.topic_list_container) != null;
		hasThingList = findViewById(R.id.thing_list_container) != null;
		thingContainer = findViewById(R.id.thing_container);
        hasThing = thingContainer != null;
		
        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);
    
        setupFragments();
        refreshThingContainer();
	}
	
	private void setupFragments() {
		Log.v(TAG, "hasThing: " + hasThing);
		
		if (hasTopicList) {
			TopicListFragment frag = (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
			boolean createFrag = frag == null;
			if (createFrag) {
				frag = new TopicListFragment();
			}
			frag.setSingleChoice(hasThingList);
			if (createFrag) {
				FragmentTransaction transaction = manager.beginTransaction();
				transaction.replace(R.id.topic_list_container, frag, TOPIC_LIST_TAG);
				transaction.commit();
			}
		}
		
		if (hasThingList) {
			ThingListFragment frag = (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);
			boolean createFrag = frag == null;
			if (createFrag) {
				frag = new ThingListFragment();
			}
			frag.setSingleChoice(hasThing);
			if (createFrag) {
				FragmentTransaction transaction = manager.beginTransaction();
				transaction.replace(R.id.thing_list_container, frag, THING_LIST_TAG);
				transaction.commit();
			}
		}
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

		ThingListFragment thingListFrag = new ThingListFragment();
		thingListFrag.setTopic(topic, position);
		thingListFrag.setSingleChoice(hasThing);
		
		transaction.setBreadCrumbTitle(topic.title);
		transaction.replace(R.id.thing_list_container, thingListFrag, THING_LIST_TAG);
		if (addToBackStack) {
			transaction.addToBackStack(null);
		}
		transaction.commit();
	}
	
	public void onThingSelected(Thing thing, int position) {
		if (thingContainer != null) {
			replaceThingFragment(thing, position);
		} else {
			Intent intent = new Intent(this, ThingActivity.class);
			intent.putExtra(ThingActivity.EXTRA_THING, thing);
			intent.putExtra(ThingActivity.EXTRA_POSITION, position);
			startActivity(intent);
		}
	}
	
	private void replaceThingFragment(Thing thing, int position) {
		Log.v(TAG, "replaceThingFragment");
		FragmentTransaction transaction = manager.beginTransaction();
		
		ThingCommentsFragment frag = new ThingCommentsFragment();
		frag.setThing(thing, position);
		
		transaction.replace(R.id.thing_container, frag, THING_TAG);
		transaction.addToBackStack(null);
		transaction.commit();		
	}
	
	public void onThingPartSelected(Thing thing, int position, ThingPart part) {
		Log.v(TAG, "onThingPartSelected");
		FragmentTransaction transaction = manager.beginTransaction();
		
		ThingWebFragment frag = new ThingWebFragment();	
		frag.setThing(thing, position);
		frag.setUrl(part.value);
		
		transaction.replace(R.id.thing_container, frag, THING_TAG);
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	public void onBackStackChanged() {
		Log.v(TAG, "onBackStackChanged");
		refreshThingContainer();
		
		TopicListFragment topicFrag = (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
		ThingListFragment thingListFrag = (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);
		ThingFragment thingFrag = (ThingFragment) manager.findFragmentByTag(THING_TAG);
		
		if (topicFrag != null) {
			int topicPosition = thingListFrag.getTopicPosition();
			topicFrag.setTopicSelected(topicPosition);
		}
		
		boolean thingShowing = thingFrag != null && thingFrag.isAdded();
		if (thingListFrag != null && !thingShowing) {
			setTitle(thingListFrag.getTopic().title);
		} else if (thingShowing) {
			setTitle(thingFrag.getThing().title);
		}
		
		int thingPosition = thingShowing ? thingFrag.getThingPosition() : -1;
		thingListFrag.setThingSelected(thingPosition);
	}
	
	private void refreshThingContainer() {
		Log.v(TAG, "refreshThingContainer");
		if (thingContainer != null) {
			Fragment frag = manager.findFragmentByTag(THING_TAG);
			thingContainer.setVisibility(frag != null && frag.isAdded() ? View.VISIBLE : View.GONE);
		}
	}
}