package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.TopicListFragment.OnTopicSelectedListener;

public class MainActivity extends Activity implements OnBackStackChangedListener,
		OnTopicSelectedListener, OnThingSelectedListener, TopicHolder, ThingHolder, LayoutInfo {

	@SuppressWarnings("unused")
	private static final String TAG = "MainActivity";
	
	private static final String CONTROL_TAG = "control";
	private static final String TOPIC_LIST_TAG = "topicList";
	private static final String THING_LIST_TAG = "thingList";
	private static final String THING_TAG = "thing";
	
	private static final String THING_FRAG_TYPE = "type";
	private static final int THING_FRAG_LINK = 0;
	private static final int THING_FRAG_COMMENTS = 1;

	private FragmentManager manager;
	private ActionBar bar;
	
	private View singleContainer;
	private View topicListContainer;
	private View thingListContainer;
	private View thingContainer;

	private int topicListContainerId;
	private int thingListContainerId;
	private int thingContainerId;

	private FragmentBreadCrumbs crumbs;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
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
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(crumbs);
        
        if (singleContainer != null) {
        	bar.setDisplayShowTitleEnabled(false);
        	crumbs.setMaxVisible(1);
        }
        
        topicListContainerId = topicListContainer != null ? R.id.topic_list_container : R.id.single_container;
        thingListContainerId = thingListContainer != null ? R.id.thing_list_container : R.id.single_container;
        thingContainerId = thingContainer != null ? R.id.thing_container : R.id.single_container;
		
        if (savedInstanceState == null) {
        	setupFragments();
        }
	}

	private void setupFragments() {
		if (singleContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(null, -1, null, -1);
			TopicListFragment topicFrag = TopicListFragment.newInstance(-1);
			
			FragmentTransaction trans = manager.beginTransaction();
			trans.add(controlFrag, CONTROL_TAG);
			trans.replace(topicListContainerId, topicFrag);
			trans.commit();
		}
		
		Topic topic = Topic.newTopic("all");
		
		if (topicListContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(topic, 0, null, -1);
			TopicListFragment topicFrag = TopicListFragment.newInstance(0);
			
			FragmentTransaction trans = manager.beginTransaction();
			trans.add(controlFrag, CONTROL_TAG);
			trans.replace(topicListContainerId, topicFrag, TOPIC_LIST_TAG);
			trans.commit();
		}
			
		if (thingListContainer != null) {
			replaceThingList(topic, 0, false);
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			onBackStackChanged();
	    }
	}
	
	public void onTopicSelected(Topic topic, int position) {
		replaceThingList(topic, position, true);
	}
	
	private void replaceThingList(Topic topic, int position, boolean addToBackStack) {
		manager.popBackStack(THING_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		manager.popBackStack(THING_LIST_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		FragmentTransaction trans = manager.beginTransaction();

		ControlFragment controlFrag = ControlFragment.newInstance(topic, position, null, -1);
		trans.add(controlFrag, CONTROL_TAG);
		
		ThingListFragment thingListFrag = ThingListFragment.newInstance();
		trans.replace(thingListContainerId, thingListFrag, THING_LIST_TAG);
		
		trans.setBreadCrumbTitle(topic.title);
		if (addToBackStack) {
			trans.addToBackStack(THING_LIST_TAG);
		}
		trans.commit();
	}
	
	public void onThingSelected(Entity thing, int position) {
		replaceThing(thing, position, thing.isSelf ? THING_FRAG_COMMENTS : THING_FRAG_LINK);
	}
	
	private void replaceThing(Entity thing, int position, int thingFragType) {
		manager.popBackStack(THING_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		ControlFragment controlFrag = ControlFragment.newInstance(getTopic(), getTopicPosition(), thing, position);
		Fragment frag = thingFragType == THING_FRAG_COMMENTS ? CommentListFragment.newInstance() : LinkFragment.newInstance();
		
		Bundle args = new Bundle();
		args.putInt(THING_FRAG_TYPE, thingFragType);
		frag.setArguments(args);
		
		FragmentTransaction trans = manager.beginTransaction();
		trans.add(controlFrag, CONTROL_TAG);
		trans.replace(thingContainerId, frag, THING_TAG);
		trans.setBreadCrumbTitle(thing.title);
		trans.addToBackStack(THING_TAG);
		trans.commit();		
	}
	
	public Topic getTopic() {
		return getControlFragment().getTopic();
	}

	public Entity getThing() {
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
	
	private Fragment getThingFragment() {
		return manager.findFragmentByTag(THING_TAG);
	}

	public void onBackStackChanged() {
		refreshHome();
		refreshCheckedItems();
		refreshThingContainer();
		invalidateOptionsMenu();
	}
	
	private void refreshHome() {
		bar.setDisplayHomeAsUpEnabled(singleContainer != null && getTopic() != null || getThing() != null);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		boolean hasThing = getThing() != null;
		menu.findItem(R.id.menu_link).setVisible(hasThing && isThingFragmentType(THING_FRAG_COMMENTS));
		menu.findItem(R.id.menu_comments).setVisible(hasThing && isThingFragmentType(THING_FRAG_LINK));
		menu.findItem(R.id.menu_copy_link).setVisible(hasThing);
		menu.findItem(R.id.menu_view).setVisible(hasThing);
		return true;
	}
	
	private boolean isThingFragmentType(int type) {
		Fragment thingFrag = getThingFragment();
		return thingFrag != null && thingFrag.getArguments().getInt(THING_FRAG_TYPE) == type;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case android.R.id.home:
			handleHome();
			return true;

		case R.id.menu_link:
			handleLink();
			return true;
			
		case R.id.menu_comments:
			handleComments();
			return true;
			
		case R.id.menu_copy_link:
			handleCopyLink();
			return true;
		
		case R.id.menu_view:
			handleView();
			return true;
		}
		return false;
	}
	
	private void handleHome() {
		int count = manager.getBackStackEntryCount();
		if (count > 0) {
			String name = manager.getBackStackEntryAt(count - 1).getName();
			if (THING_TAG.equals(name)) {
				manager.popBackStack(THING_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} else if (THING_LIST_TAG.equals(name)) {
				manager.popBackStack(THING_LIST_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}
	
	private void handleLink() {
		replaceThing(getThing(), getThingPosition(), THING_FRAG_LINK);
	}
	
	private void handleComments() {
		replaceThing(getThing(), getThingPosition(), THING_FRAG_COMMENTS);
	}
	
	private void handleCopyLink() {
		String url = getThing().url;
		ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(url);
		Toast.makeText(this, url, Toast.LENGTH_SHORT).show();	
	}
	
	private void handleView() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getThing().url));
		startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));	
	}
}