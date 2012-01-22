package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.TopicListFragment.OnTopicSelectedListener;

public class MainActivity extends Activity implements OnBackStackChangedListener,
		OnTopicSelectedListener, OnThingSelectedListener, TopicHolder, ThingHolder, LayoutInfo {

	private static final String TAG = "MainActivity";
	
	private static final String CONTROL_TAG = "control";
	private static final String TOPIC_LIST_TAG = "topicList";
	private static final String THING_LIST_TAG = "thingList";
	private static final String THING_TAG = "thing";

	private FragmentManager manager;
	private ActionBar bar;
	
	private View singleContainer;
	private View topicListContainer;
	private View thingListContainer;
	private View thingContainer;

	private int topicListContainerId;
	private int thingListContainerId;

	private FragmentBreadCrumbs crumbs;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);
        
        singleContainer = findViewById(R.id.single_container);
        topicListContainer = findViewById(R.id.topic_list_container);
        thingListContainer = findViewById(R.id.thing_list_container);
        thingContainer = findViewById(R.id.thing_container);
        if (thingContainer != null) {
        	thingContainer.setVisibility(View.GONE);
        }
        
        crumbs = new FragmentBreadCrumbs(this);
        crumbs.setActivity(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(singleContainer == null);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(crumbs);
        
        topicListContainerId = topicListContainer != null ? R.id.topic_list_container : R.id.single_container;
        thingListContainerId = thingListContainer != null ? R.id.thing_list_container : R.id.single_container;
		
        if (savedInstanceState == null) {
        	setupFragments();
        }
	}

	private void setupFragments() {
		Topic topic = Topic.newTopic("all");
		ControlFragment controlFrag = ControlFragment.newInstance(topic, 0, null, -1);
		TopicListFragment topicFrag = TopicListFragment.newInstance(0);
		
		FragmentTransaction trans = manager.beginTransaction();
		trans.add(controlFrag, CONTROL_TAG);
		trans.replace(topicListContainerId, topicFrag, TOPIC_LIST_TAG);
		trans.commit();
	
		if (thingListContainer != null) {
			replaceThingList(topic, 0, false);
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Log.v(TAG, "onRestoreInstanceState");
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			onBackStackChanged();
	    }
	}
	
	public void onTopicSelected(Topic topic, int position) {
		Log.v(TAG, "onTopicSelected");
		replaceThingList(topic, position, true);
	}
	
	private void replaceThingList(Topic topic, int position, boolean addToBackStack) {
		manager.popBackStack(THING_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		manager.popBackStack(THING_LIST_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		FragmentTransaction trans = manager.beginTransaction();

		ControlFragment controlFrag = ControlFragment.newInstance(topic, position, null, -1);
		trans.add(controlFrag, CONTROL_TAG);
		
		if (singleContainer != null) {
			TopicListFragment topicFrag = getTopicListFragment();
			if (topicFrag != null) {
				trans.remove(topicFrag);
			}
		}
		
		ThingListFragment thingListFrag = ThingListFragment.newInstance();
		trans.replace(thingListContainerId, thingListFrag, THING_LIST_TAG);
		
		ThingFragment thingFrag = getThingFragment();
		if (thingFrag != null) {
			trans.remove(thingFrag);
		}
		
		trans.setBreadCrumbTitle(topic.title);
		if (addToBackStack) {
			trans.addToBackStack(THING_LIST_TAG);
		}
		trans.commit();
	}
	
	public void onThingSelected(Thing thing, int position) {
		Log.v(TAG, "onThingSelected");
		if (thingContainer != null) {
			replaceThingFragment(thing, position);
		} else {
			startThingActivity(thing, position);
		}
	}
	
	private void replaceThingFragment(Thing thing, int position) {
		manager.popBackStack(THING_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		ControlFragment controlFrag = ControlFragment.newInstance(getTopic(), getTopicPosition(), thing, position);
		ThingFragment frag = ThingFragment.newInstance();
		
		FragmentTransaction trans = manager.beginTransaction();
		trans.add(controlFrag, CONTROL_TAG);
		trans.replace(R.id.thing_container, frag, THING_TAG);
		trans.setBreadCrumbTitle(thing.title);
		trans.addToBackStack(THING_TAG);
		trans.commit();		
	}
	
	private void startThingActivity(Thing thing, int position) {
		Intent intent = new Intent(this, ThingActivity.class);
		intent.putExtra(ThingActivity.EXTRA_THING, thing);
		intent.putExtra(ThingActivity.EXTRA_POSITION, position);
		startActivity(intent);
	}
	
	public Topic getTopic() {
		return getControlFragment().getTopic();
	}

	public Thing getThing() {
		return getControlFragment().getThing();
	}
	
	public boolean hasTopicListContainer() {
		return topicListContainer != null;
	}
	
	public boolean hasThingListContainer() {
		return thingListContainer != null;
	}
	
	public boolean hasThingContainer() {
		return thingContainer != null;
	}
	
	private int getTopicPosition() {
		return getControlFragment().getTopicPosition();
	}
	
	private int getThingPosition() {
		return getControlFragment().getThingPosition();
	}
	
	private ControlFragment getControlFragment() {
		return (ControlFragment) manager.findFragmentByTag(CONTROL_TAG);
	}
	
	private TopicListFragment getTopicListFragment() {
		return (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
	}
	
	private ThingListFragment getThingListFragment() {
		return (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);
	}
	
	private ThingFragment getThingFragment() {
		return (ThingFragment) manager.findFragmentByTag(THING_TAG);
	}
	
	public void onBackStackChanged() {
		refreshHome();
		refreshCheckedItems();
		refreshThingContainer();
	}
	
	private void refreshHome() {
		bar.setDisplayHomeAsUpEnabled(getThing() != null);
	}
	
	private void refreshCheckedItems() {
		TopicListFragment topicFrag = getTopicListFragment();
		if (topicFrag != null && topicFrag.isAdded()) {
			topicFrag.setItemChecked(getTopicPosition());
		}
		
		ThingListFragment thingListFrag = getThingListFragment();
		if (thingListFrag != null && thingListFrag.isAdded()) {
			thingListFrag.setItemChecked(getThingPosition());
		}
	}
	
	private void refreshThingContainer() {
		if (thingContainer != null) {
			thingContainer.setVisibility(getThing() != null ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case android.R.id.home:
			handleUpItem();
			return true;
		}
		return false;
	}
	
	private void handleUpItem() {
		manager.popBackStack(THING_LIST_TAG, 0);
	}
}