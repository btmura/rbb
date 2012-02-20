package com.btmura.android.reddit;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.btmura.android.reddit.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.common.Formatter;
import com.btmura.android.reddit.subredditsearch.SubredditSearchActivity;

public class MainActivity extends Activity implements OnBackStackChangedListener, OnNavigationListener, 
		OnQueryTextListener, OnFocusChangeListener, OnSubredditSelectedListener, OnThingSelectedListener {

	private static final String FRAG_CONTROL = "control";
	private static final String FRAG_SUBREDDIT_LIST = "subredditList";
	private static final String FRAG_THING_LIST = "thingList";
	private static final String FRAG_LINK = "link";
	private static final String FRAG_COMMENT = "comment";
	
	private static final int REQUEST_ADD_SUBREDDITS = 0;

	private static final String STATE_LAST_SELECTED_FILTER = "lastSelectedFilter";

	private FragmentManager manager;
	private ActionBar bar;
	private SearchView searchView;
	private FilterAdapter filterSpinner;
	private int lastSelectedFilter;
	
	private View singleContainer;
	private View thingContainer;
	
	private ShareActionProvider shareProvider;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(R.layout.subreddits_search);
        
        searchView = (SearchView) bar.getCustomView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
        
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
			ControlFragment controlFrag = ControlFragment.newInstance(null, null, -1, 0);	
			SubredditListFragment srFrag = SubredditListFragment.newInstance(false);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.single_container, srFrag);
			ft.commit();
		}
		
		if (thingContainer != null) {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			ControlFragment controlFrag = ControlFragment.newInstance(null, null, -1, 0);
			SubredditListFragment srFrag = SubredditListFragment.newInstance(true);
			
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(controlFrag, FRAG_CONTROL);
			ft.replace(R.id.subreddit_list_container, srFrag, FRAG_SUBREDDIT_LIST);
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
			selectSubreddit(getSubreddit(), itemPosition, true);
		}
		return true;
	}
	
	public void onSubredditSelected(Subreddit sr, int event) {
		switch (event) {
		
		case OnSubredditSelectedListener.FLAG_ITEM_CLICKED:
			selectSubreddit(sr, lastSelectedFilter, true);
			break;
		
		case OnSubredditSelectedListener.FLAG_LOAD_FINISHED:
			if (thingContainer != null && !isVisible(FRAG_THING_LIST)) {
				setNavigationListMode(sr);
				getSubredditListFragment().setSelectedSubreddit(sr);
				selectSubreddit(sr, lastSelectedFilter, false);
			}
			break;
		}		
	}
	
	private void selectSubreddit(Subreddit sr, int filter, boolean addToBackStack) {
		ControlFragment controlFrag = ControlFragment.newInstance(sr, null, -1, filter);
		
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
	
	public void onThingSelected(Thing thing, int position) {
		selectThing(thing, thing.isSelf ? FRAG_COMMENT : FRAG_LINK, position);
	}
	
	private void selectThing(Thing thing, String tag, int position) {
		ControlFragment controlFrag = ControlFragment.newInstance(getSubreddit(), thing, position, getFilter());
		Fragment thingFrag;
		if (FRAG_LINK.equalsIgnoreCase(tag)) {
			thingFrag = LinkFragment.newInstance(thing);
		} else if (FRAG_COMMENT.equalsIgnoreCase(tag)) {
			thingFrag = CommentListFragment.newInstance(thing.getId());
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

	private Thing getThing() {
		return getControlFragment().getThing();
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
	
	private SubredditListFragment getSubredditListFragment() {
		return (SubredditListFragment) manager.findFragmentByTag(FRAG_SUBREDDIT_LIST);
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
		Thing thing = getThing();
		if (thing != null && !isVisible(FRAG_SUBREDDIT_LIST)) {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			bar.setDisplayShowTitleEnabled(true);
			bar.setDisplayShowCustomEnabled(false);
			bar.setTitle(thing.title);
		} else if (sr != null) {
			setNavigationListMode(sr);
		} else {
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			bar.setDisplayShowTitleEnabled(true);
			bar.setDisplayShowCustomEnabled(false);
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
		bar.setDisplayShowCustomEnabled(false);
		filterSpinner.setSubreddit(sr.getTitle(this));
	}
	
	private void refreshCheckedItems() {
		if (isVisible(FRAG_SUBREDDIT_LIST)) {
			getSubredditListFragment().setSelectedSubreddit(getSubreddit());
		}
		
		if (isVisible(FRAG_THING_LIST)) {
			getThingListFragment().setChosenPosition(getThingPosition());
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
		shareProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		Thing thing = getThing();
		boolean hasThing = thing != null;
		boolean isSelf = thing != null && thing.isSelf;
		menu.findItem(R.id.menu_link).setVisible(hasThing && !isSelf && isVisible(FRAG_COMMENT));
		menu.findItem(R.id.menu_comments).setVisible(hasThing && !isSelf && isVisible(FRAG_LINK));
		menu.findItem(R.id.menu_share).setVisible(hasThing);
		menu.findItem(R.id.menu_copy_url).setVisible(hasThing);
		menu.findItem(R.id.menu_view).setVisible(hasThing);		
		updateShareActionIntent(thing);		
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
			
		case R.id.menu_search_for_subreddits:
			handleSearchForSubreddits();
			return true;

		case R.id.menu_link:
			handleLink();
			return true;
			
		case R.id.menu_comments:
			handleComments();
			return true;
			
		case R.id.menu_copy_url:
			handleCopyUrl();
			return true;
			
		case R.id.menu_view:
			handleView();
			return true;
		}
		return false;
	}
	
	private void handleSearchForSubreddits() {
		searchView.setQuery("", false);
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		bar.setDisplayShowCustomEnabled(true);
		bar.getCustomView().requestFocus();
	}
	
	public boolean onQueryTextChange(String newText) {
		return false;
	}
	
	public boolean onQueryTextSubmit(String query) {
		Intent intent = new Intent(this, SubredditSearchActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra(SubredditSearchActivity.EXTRA_QUERY, query);
		startActivityForResult(intent, REQUEST_ADD_SUBREDDITS);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ADD_SUBREDDITS:
			refreshActionBar();
			break;
		
		default:
			throw new IllegalStateException("Unexpected request code: " + requestCode);
		}
	}
	
	public void onFocusChange(View v, boolean hasFocus) {
		if (v == searchView && !hasFocus) {
			refreshActionBar();
		}
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
	
	private void handleCopyUrl() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		String text = getLink(getThing());
		clipboard.setText(text);
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}
	
	private void handleView() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(getLink(getThing())));
		startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));	
	}
	
	private String getLink(Thing thing) {
		return isVisible(FRAG_LINK) ? thing.url : "http://www.reddit.com" + thing.permaLink;
	}

	private void updateShareActionIntent(Thing thing) {
		if (thing != null) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, Formatter.format(thing.title));
			intent.putExtra(Intent.EXTRA_TEXT, getLink(thing));
			shareProvider.setShareIntent(intent);
		}
	}
}