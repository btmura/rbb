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

	private static final String FRAG_CONTROL = "control";
	private static final String FRAG_TOPIC_LIST = "topicList";
	private static final String FRAG_SUBREDDIT = "subreddit";
	private static final String FRAG_LINK = "link";
	private static final String FRAG_COMMENT = "comment";
	
	private FragmentManager manager;
	private ActionBar bar;
	
	private View singleContainer;
	private View thingContainer;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);
        
        singleContainer = findViewById(R.id.single_container);        
        thingContainer = findViewById(R.id.thing_container);
    
        if (savedInstanceState == null) {
        	setupFragments();
        }
	}

	private void setupFragments() {
		if (singleContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(null, -1, null, -1);
			TopicListFragment topicFrag = TopicListFragment.newInstance(-1, false);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, topicFrag);
			ft.commit();
		}
		
		if (thingContainer != null) {
			Topic topic = Topic.newTopic("all");
			
			ControlFragment controlFrag = ControlFragment.newInstance(topic, 0, null, -1);
			TopicListFragment topicFrag = TopicListFragment.newInstance(0, true);
			ThingListFragment thingListFrag = ThingListFragment.newInstance(topic, true);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.topic_list_container, topicFrag, FRAG_TOPIC_LIST);
			ft.replace(R.id.thing_list_container, thingListFrag, FRAG_SUBREDDIT);
			ft.commit();
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
		if (singleContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(topic, position, null, -1);
			ThingListFragment thingListFrag = ThingListFragment.newInstance(topic, false);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, thingListFrag, FRAG_SUBREDDIT);
			ft.addToBackStack(FRAG_SUBREDDIT);
			ft.commit();
		}
		
		if (thingContainer != null) {
			ControlFragment controlFrag = ControlFragment.newInstance(topic, position, null, -1);
			ThingListFragment thingListFrag = ThingListFragment.newInstance(topic, true);
			Fragment linkFrag = getLinkFragment();
			Fragment commentFrag = getCommentFragment();
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.thing_list_container, thingListFrag, FRAG_SUBREDDIT);
			if (linkFrag != null) {
				ft.remove(linkFrag);
			}
			if (commentFrag != null) {
				ft.remove(commentFrag);
			}
			ft.addToBackStack(FRAG_SUBREDDIT);
			ft.commit();
		}
	}
	
	public void onThingSelected(Entity thing, int position) {
		selectThing(thing, thing.isSelf ? FRAG_COMMENT : FRAG_LINK, position);
	}
	
	private void selectThing(Entity thing, String tag, int position) {
		ControlFragment controlFrag = ControlFragment.newInstance(getTopic(), getTopicPosition(), thing, position);
		Fragment thingFrag;
		if (FRAG_LINK.equalsIgnoreCase(tag)) {
			thingFrag = LinkFragment.newInstance(thing);
		} else if (FRAG_COMMENT.equalsIgnoreCase(tag)) {
			thingFrag = CommentListFragment.newInstance(thing);
		} else {
			throw new IllegalArgumentException(tag);
		}
		
		if (singleContainer != null) {
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, thingFrag, tag);
			ft.addToBackStack(tag);
			ft.commit();
		}
		
		if (thingContainer != null) {
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.thing_container, thingFrag, tag);
			ft.addToBackStack(tag);
			ft.commit();		
		}
	}
	
	private Topic getTopic() {
		return getControlFragment().getTopic();
	}

	private Entity getThing() {
		return getControlFragment().getThing();
	}

	private int getTopicPosition() {
		return getControlFragment().getTopicPosition();
	}
	
	private int getThingPosition() {
		return getControlFragment().getThingPosition();
	}
	
	private ControlFragment getControlFragment() {
		return (ControlFragment) manager.findFragmentByTag(FRAG_CONTROL);
	}
	
	private TopicListFragment getTopicListFragment() {
		return (TopicListFragment) manager.findFragmentByTag(FRAG_TOPIC_LIST);
	}
	
	private ThingListFragment getThingListFragment() {
		return (ThingListFragment) manager.findFragmentByTag(FRAG_SUBREDDIT);
	}
	
	private Fragment getLinkFragment() {
		return manager.findFragmentByTag(FRAG_LINK);
	}
	
	private Fragment getCommentFragment() {
		return manager.findFragmentByTag(FRAG_COMMENT);
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
			bar.setTitle(topic.name);
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
		menu.findItem(R.id.menu_link).setVisible(hasThing && !isSelf && isVisible(FRAG_COMMENT));
		menu.findItem(R.id.menu_comments).setVisible(hasThing && !isSelf && isVisible(FRAG_LINK));
		menu.findItem(R.id.menu_copy_link).setVisible(hasThing);
		menu.findItem(R.id.menu_view).setVisible(hasThing);
		return true;
	}
	
	private boolean isVisible(String tag) {
		Fragment f = manager.findFragmentByTag(tag);
		return f != null && f.isAdded();
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
			String tag = manager.getBackStackEntryAt(count - 1).getName();
			if (FRAG_COMMENT.equals(tag) || FRAG_LINK.equals(tag)) {
				for (int i = manager.getBackStackEntryCount() - 1; i >= 0; i--) {
					String name = manager.getBackStackEntryAt(i).getName();
					if (!FRAG_SUBREDDIT.equals(name)) {
						manager.popBackStack();
					} else {
						break;
					}
				}
			} else if (FRAG_SUBREDDIT.equals(tag)) {
				manager.popBackStack(FRAG_SUBREDDIT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}
	
	private void handleLink() {
		selectThing(getThing(), FRAG_LINK, getThingPosition());
	}
	
	private void handleComments() {
		selectThing(getThing(), FRAG_COMMENT, getThingPosition());
	}
	
	private void handleCopyLink() {
		clipText(getLink());
	}
	
	private void clipText(String text) {
		ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		clip.setText(text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();		
	}
	
	private String getLink() {
		return isVisible(FRAG_LINK) ? getThing().url : "http://www.reddit.com" + getThing().permaLink;
	}
	
	private void handleView() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getLink()));
		startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));	
	}
}