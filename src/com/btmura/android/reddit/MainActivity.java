package com.btmura.android.reddit;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
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
	
	private View topicListContainer;
	private View thingContainer;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);
        
        topicListContainer = findViewById(R.id.topic_list_container);
        thingContainer = findViewById(R.id.thing_container);
        if (thingContainer != null) {
        	thingContainer.setVisibility(View.GONE);
        }
        
        setupFragments();
	}

	private void setupFragments() {
		if (manager.findFragmentByTag(CONTROL_TAG) == null) {
			MainControlFragment controlFrag = MainControlFragment.newInstance(Topic.frontPage(), 0, null, -1);
			manager.beginTransaction().add(controlFrag, CONTROL_TAG).commit();
		}
		
		if (manager.findFragmentByTag(TOPIC_LIST_TAG) == null) {
			TopicListFragment frag = TopicListFragment.newInstance(0, true);
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(R.id.topic_list_container, frag, TOPIC_LIST_TAG);
			transaction.commit();
		}
		
		if (manager.findFragmentByTag(THING_LIST_TAG) == null) {
			ThingListFragment frag = ThingListFragment.newInstance();
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(R.id.thing_list_container, frag, THING_LIST_TAG);
			transaction.commit();
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
		FragmentTransaction transaction = manager.beginTransaction();

		MainControlFragment controlFrag = MainControlFragment.newInstance(topic, position, null, -1);
		transaction.add(controlFrag, CONTROL_TAG);
		
		ThingListFragment thingListFrag = ThingListFragment.newInstance();
		transaction.replace(R.id.thing_list_container, thingListFrag, THING_LIST_TAG);
		
		Fragment thingFrag = manager.findFragmentByTag(THING_TAG);
		if (thingFrag != null) {
			transaction.remove(thingFrag);
		}

		transaction.addToBackStack(null);
		transaction.commit();
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
		FragmentTransaction transaction = manager.beginTransaction();
		
		MainControlFragment controlFrag = MainControlFragment.newInstance(getTopic(), getTopicPosition(), thing, position);
		transaction.add(controlFrag, CONTROL_TAG);
		
		ThingTabFragment frag = ThingTabFragment.newInstance();
		transaction.replace(R.id.thing_container, frag, THING_TAG);
		
		transaction.addToBackStack(null);
		transaction.commit();		
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
	
	public boolean hasThingContainer() {
		return thingContainer != null;
	}
	
	private int getTopicPosition() {
		return getControlFragment().getTopicPosition();
	}
	
	private int getThingPosition() {
		return getControlFragment().getThingPosition();
	}
	
	private MainControlFragment getControlFragment() {
		return (MainControlFragment) manager.findFragmentByTag(CONTROL_TAG);
	}
	
	private TopicListFragment getTopicListFragment() {
		return (TopicListFragment) manager.findFragmentByTag(TOPIC_LIST_TAG);
	}
	
	private ThingListFragment getThingListFragment() {
		return (ThingListFragment) manager.findFragmentByTag(THING_LIST_TAG);
	}
	
	public void onBackStackChanged() {
		Log.v(TAG, "onBackStackChanged");
		refreshCheckedItems();
		refreshThingContainer();
	}
	
	private void refreshCheckedItems() {
		getTopicListFragment().setItemChecked(getTopicPosition());
		getThingListFragment().setItemChecked(getThingPosition());
	}
	
	private void refreshThingContainer() {
		if (thingContainer != null) {
			thingContainer.setVisibility(getThing() != null ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.v(TAG, "onPrepareOptionsMenu");
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_subreddits).setVisible(topicListContainer.getVisibility() != View.VISIBLE);
		return true;
	}
}