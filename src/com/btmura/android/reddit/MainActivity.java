package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
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

import com.btmura.android.reddit.AddSubredditFragment.OnSubredditAddedListener;
import com.btmura.android.reddit.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;

public class MainActivity extends Activity implements OnBackStackChangedListener, OnNavigationListener,
		OnSubredditAddedListener, OnSubredditSelectedListener, OnThingSelectedListener {

	private static final String FRAG_CONTROL = "control";
	private static final String FRAG_SUBREDDIT_LIST = "subredditList";
	private static final String FRAG_THING_LIST = "thingList";
	private static final String FRAG_LINK = "link";
	private static final String FRAG_COMMENT = "comment";
	private static final String FRAG_ADD_SUBREDDIT = "addSubreddit";

	private static final String STATE_LAST_SELECTED_FILTER = "lastSelectedFilter";
	
	private FragmentManager manager;
	private ActionBar bar;

	private FilterAdapter filterSpinner;
	private int lastSelectedFilter;
	
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
        
        filterSpinner = new FilterAdapter(this);
        bar.setListNavigationCallbacks(filterSpinner, this);

        singleContainer = findViewById(R.id.single_container);
        thingContainer = findViewById(R.id.thing_container);
    
        if (savedInstanceState == null) {
        	setupFragments();
        }
	}

	private void setupFragments() {
		if (singleContainer != null) {			
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			ControlFragment controlFrag = ControlFragment.newInstance(null, -1, null, -1, 0);	
			SubredditListFragment subredditFrag = SubredditListFragment.newInstance();
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, subredditFrag);
			ft.commit();
		}
		
		if (thingContainer != null) {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			ControlFragment controlFrag = ControlFragment.newInstance(null, -1, null, -1, 0);
			SubredditListFragment subredditFrag = SubredditListFragment.newInstance();
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.subreddit_list_container, subredditFrag, FRAG_SUBREDDIT_LIST);
			ft.commit();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_LAST_SELECTED_FILTER, lastSelectedFilter);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			lastSelectedFilter = savedInstanceState.getInt(STATE_LAST_SELECTED_FILTER);
			onBackStackChanged();
	    }
	}
	
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		lastSelectedFilter = itemPosition;
		if (itemId != getFilter()) {
			selectSubreddit(getSubreddit(), getSubredditPosition(), itemPosition, true);
		}
		return true;
	}
	
	public void onSubredditSelected(Subreddit sr, int position, int event) {
		switch (event) {
		
		case OnSubredditSelectedListener.FLAG_ITEM_CLICKED:
			selectSubreddit(sr, position, lastSelectedFilter, true);
			break;
		
		case OnSubredditSelectedListener.FLAG_LOAD_FINISHED:
			if (thingContainer != null && !isVisible(FRAG_THING_LIST)) {
				setNavigationListMode(sr);
				selectSubreddit(sr, position, lastSelectedFilter, false);
			}
			break;
		}		
	}
	
	private void selectSubreddit(Subreddit sr, int position, int filter, boolean addToBackStack) {
		ControlFragment controlFrag = ControlFragment.newInstance(sr, position, null, -1, filter);
		
		if (singleContainer != null) {
			ThingListFragment thingListFrag = ThingListFragment.newInstance(sr, filter, false);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, thingListFrag, FRAG_THING_LIST);
			ft.addToBackStack(FRAG_THING_LIST);
			ft.commit();
		}
		
		if (thingContainer != null) {
			ThingListFragment thingListFrag = ThingListFragment.newInstance(sr, filter, true);
			Fragment linkFrag = getLinkFragment();
			Fragment commentFrag = getCommentFragment();
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.thing_list_container, thingListFrag, FRAG_THING_LIST);
			if (linkFrag != null) {
				ft.remove(linkFrag);
			}
			if (commentFrag != null) {
				ft.remove(commentFrag);
			}
			if (addToBackStack) {
				ft.addToBackStack(FRAG_THING_LIST);
			}
			ft.commit();
		}
	}
	
	public void onThingSelected(Entity thing, int position) {
		selectThing(thing, thing.isSelf ? FRAG_COMMENT : FRAG_LINK, position);
	}
	
	private void selectThing(Entity thing, String tag, int position) {
		ControlFragment controlFrag = ControlFragment.newInstance(getSubreddit(), getSubredditPosition(), thing, position, getFilter());
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
	
	private Subreddit getSubreddit() {
		return getControlFragment().getTopic();
	}

	private Entity getThing() {
		return getControlFragment().getThing();
	}

	private int getSubredditPosition() {
		return getControlFragment().getTopicPosition();
	}
	
	private int getThingPosition() {
		return getControlFragment().getThingPosition();
	}
	
	private int getFilter() {
		return getControlFragment().getFilter();
	}
	
	private ControlFragment getControlFragment() {
		return (ControlFragment) manager.findFragmentByTag(FRAG_CONTROL);
	}
	
	private ThingListFragment getThingListFragment() {
		return (ThingListFragment) manager.findFragmentByTag(FRAG_THING_LIST);
	}
	
	private Fragment getLinkFragment() {
		return manager.findFragmentByTag(FRAG_LINK);
	}
	
	private Fragment getCommentFragment() {
		return manager.findFragmentByTag(FRAG_COMMENT);
	}
	
	public void onBackStackChanged() {
		refreshActionBar();
		refreshCheckedItems();
		refreshContainers();
		invalidateOptionsMenu();
	}
	
	private void refreshActionBar() {
		Subreddit sr = getSubreddit();
		Entity thing = getThing();
		if (thing != null && !isVisible(FRAG_SUBREDDIT_LIST)) {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			bar.setDisplayShowTitleEnabled(true);
			bar.setTitle(thing.title);
		} else if (sr != null) {
			setNavigationListMode(sr);
		} else {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			bar.setDisplayShowTitleEnabled(true);
			bar.setTitle(R.string.app_name);
		}
		
		bar.setDisplayHomeAsUpEnabled(singleContainer != null && getSubreddit() != null || getThing() != null);
		if (bar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
			bar.setSelectedNavigationItem(getFilter());
		}
	}
	
	private void setNavigationListMode(Subreddit sr) {
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		bar.setDisplayShowTitleEnabled(false);
		filterSpinner.setSubreddit(sr.name);
	}
	
	private void refreshCheckedItems() {
		ThingListFragment thingListFrag = getThingListFragment();
		if (thingListFrag != null && thingListFrag.isAdded()) {
			thingListFrag.setItemChecked(getThingPosition());
		}
	}
	
	private void refreshContainers() {
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
			
		case R.id.menu_add_subreddit:
			handleAddSubreddit();
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
	
	private void handleAddSubreddit() {
		AddSubredditFragment f = new AddSubredditFragment();
		f.show(manager, FRAG_ADD_SUBREDDIT);
	}
	
	private void handleHome() {
		int count = manager.getBackStackEntryCount();
		if (count > 0) {
			String tag = manager.getBackStackEntryAt(count - 1).getName();
			if (FRAG_COMMENT.equals(tag) || FRAG_LINK.equals(tag)) {
				for (int i = manager.getBackStackEntryCount() - 1; i >= 0; i--) {
					String name = manager.getBackStackEntryAt(i).getName();
					if (!FRAG_THING_LIST.equals(name)) {
						manager.popBackStack();
					} else {
						break;
					}
				}
			} else if (singleContainer != null && FRAG_THING_LIST.equals(tag)) {
				manager.popBackStack(FRAG_THING_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

	public void onSubredditAdded(String name) {
		RedditProvider.addSubredditInBackground(this, name);
	}
}