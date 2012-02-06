package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
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
		OnTopicSelectedListener, OnThingSelectedListener {

	@SuppressWarnings("unused")
	private static final String TAG = "MainActivity";
	
	private static final String CONTROL_TAG = "control";
	private static final String TOPIC_LIST_TAG = "topicList";
	private static final String THING_SUBREDDIT_TAG = "thingList";
	private static final String THING_LINK_TAG = "thingLink";
	private static final String THING_COMMENTS_TAG = "thingComments";
	
	private FragmentManager manager;
	private ActionBar bar;
	
	private View singleContainer;
	private View topicListContainer;
	private View thingListContainer;
	private View thingContainer;

	private int topicListContainerId;
	private int thingListContainerId;
	private int thingContainerId;

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
        
        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);

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
			TopicListFragment topicFrag = TopicListFragment.newInstance(-1, hasThingListContainer());
			
			FragmentTransaction trans = manager.beginTransaction();
			trans.add(controlFrag, CONTROL_TAG);
			trans.replace(topicListContainerId, topicFrag);
			trans.commit();
		}
		
		Topic topic = Topic.newTopic("all");
		
		if (topicListContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(topic, 0, null, -1);
			TopicListFragment topicFrag = TopicListFragment.newInstance(0, hasThingListContainer());
			
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
		manager.popBackStack(THING_SUBREDDIT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		
		FragmentTransaction trans = manager.beginTransaction();

		ControlFragment controlFrag = ControlFragment.newInstance(topic, position, null, -1);
		trans.add(controlFrag, CONTROL_TAG);
		
		ThingListFragment thingListFrag = ThingListFragment.newInstance(topic, hasThingContainer());
		trans.replace(thingListContainerId, thingListFrag, THING_SUBREDDIT_TAG);
		
		if (addToBackStack) {
			trans.addToBackStack(THING_SUBREDDIT_TAG);
		}
		trans.commit();
	}
	
	public void onThingSelected(Entity thing, int position) {
		replaceThing(thing, position, thing.isSelf ? THING_COMMENTS_TAG : THING_LINK_TAG, false);
	}
	
	private void replaceThing(Entity thing, int position, String tag, boolean popImmediately) {
		if (popImmediately) {
			manager.popBackStackImmediate(THING_LINK_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			manager.popBackStackImmediate(THING_COMMENTS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			manager.popBackStack(THING_LINK_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			manager.popBackStack(THING_COMMENTS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
		
		ControlFragment controlFrag = ControlFragment.newInstance(getTopic(), getTopicPosition(), thing, position);
		Fragment frag = THING_COMMENTS_TAG.equals(tag) ? CommentListFragment.newInstance(thing) : LinkFragment.newInstance(thing);
		
		FragmentTransaction trans = manager.beginTransaction();
		trans.add(controlFrag, CONTROL_TAG);
		trans.replace(thingContainerId, frag, tag);
		trans.addToBackStack(tag);
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
		return (ThingListFragment) manager.findFragmentByTag(THING_SUBREDDIT_TAG);
	}
	
	public void onBackStackChanged() {
		refreshHome();
		refreshTitle();
		refreshCheckedItems();
		refreshThingContainer();
		invalidateOptionsMenu();
	}
	
	private void refreshHome() {
		bar.setDisplayHomeAsUpEnabled(singleContainer != null && getTopic() != null || getThing() != null);
	}
	
	private void refreshTitle() {
		Topic topic = getTopic();
		Entity thing = getThing();
		if (thing != null) {
			bar.setTitle(thing.title);
		} else if (topic != null) {
			bar.setTitle(topic.title);
		} else {
			bar.setTitle(R.string.app_name);
		}
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
		boolean isSelf = hasThing && getThing().isSelf;
		menu.findItem(R.id.menu_link).setVisible(hasThing && !isSelf && isVisible(THING_COMMENTS_TAG));
		menu.findItem(R.id.menu_comments).setVisible(hasThing && !isSelf && isVisible(THING_LINK_TAG));
		menu.findItem(R.id.menu_copy_link).setVisible(hasThing && !isSelf);
		menu.findItem(R.id.menu_copy_reddit_link).setVisible(hasThing);
		menu.findItem(R.id.menu_view).setVisible(hasThing && !isSelf);
		return true;
	}
	
	private boolean isVisible(String tag) {
		Fragment f = manager.findFragmentByTag(tag);
		return f != null && f.isVisible();
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
			
		case R.id.menu_copy_reddit_link:
			handleCopyRedditLink();
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
			String tag = manager.getBackStackEntryAt(count - 1).getName();
			if (THING_COMMENTS_TAG.equals(tag) || THING_LINK_TAG.equals(tag) || THING_SUBREDDIT_TAG.equals(tag)) {
				manager.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}
	
	private void handleLink() {
		replaceThing(getThing(), getThingPosition(), THING_LINK_TAG, singleContainer != null);
	}
	
	private void handleComments() {
		replaceThing(getThing(), getThingPosition(), THING_COMMENTS_TAG, singleContainer != null);
	}
	
	private void handleCopyLink() {
		clipText(getThing().url);
	}
	
	private void handleCopyRedditLink() {
		clipText("http://www.reddit.com" + getThing().permaLink);
	}
	
	private void clipText(String text) {
		ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();		
	}
	
	private void handleView() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getThing().url));
		startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));	
	}
}