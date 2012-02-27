package com.btmura.android.reddit;

import java.util.List;

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
import com.btmura.android.reddit.data.Formatter;
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

	private ActionBar bar;
	private SearchView searchView;
	private FilterAdapter filterSpinner;
	private int lastSelectedFilter;
	
	private View singleContainer;
	private View subredditListContainer;
	private View thingContainer;
	private View navContainer;
	private View navSeparator;
	
	private Subreddit intentSubreddit;
	
	private ShareActionProvider shareProvider;
	private boolean singleChoice;
	private int tlfContainerId;
	private int slfContainerId;
	private int tContainerId;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getFragmentManager().addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(R.layout.subreddits_search);
        
        searchView = (SearchView) bar.getCustomView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
        
        filterSpinner = new FilterAdapter(this);
        bar.setListNavigationCallbacks(filterSpinner, this);

        singleContainer = findViewById(R.id.single_container);
        subredditListContainer = findViewById(R.id.subreddit_list_container);
        thingContainer = findViewById(R.id.thing_container);
        navContainer = findViewById(R.id.nav_container);
        navSeparator = findViewById(R.id.nav_separator);
     
		singleChoice = singleContainer == null;
		if (singleContainer != null) {
			tlfContainerId = slfContainerId = tContainerId = R.id.single_container;
		} else {
			tlfContainerId = R.id.thing_list_container;
			slfContainerId = R.id.subreddit_list_container;
			tContainerId = R.id.thing_container;
		}
		 
        intentSubreddit = getIntentSubreddit();
        
        if (savedInstanceState == null) {
        	setupFragments();
        }
	}
	
	private Subreddit getIntentSubreddit() {
		Intent intent = getIntent();
		Uri uri = intent.getData();
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (!segments.isEmpty()) {
				String name = segments.get(segments.size() - 1);
				return Subreddit.newInstance(name);
			}
		}
		return null;
	}
	
	private void setupFragments() {	
		refreshActionBar(intentSubreddit, null, 0);
		refreshContainers(null);
		
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ControlFragment cf = ControlFragment.newInstance(intentSubreddit, null, -1, 0);
		ft.add(cf, FRAG_CONTROL);
		
		Fragment f;
		int containerId;
		String tag;
		if (intentSubreddit != null) {
			f = ThingListFragment.newInstance(intentSubreddit, 0, singleChoice);
			containerId = tlfContainerId;
			tag = FRAG_THING_LIST;
		} else {
			f = SubredditListFragment.newInstance(singleChoice);
			containerId = slfContainerId;
			tag = FRAG_SUBREDDIT_LIST;
		}
		ft.replace(containerId, f, tag);
		ft.commit();
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
			selectSubreddit(getSubreddit(), itemPosition);
		}
		return true;
	}
	
	public void onSubredditSelected(Subreddit sr, int event) {
		switch (event) {
		
		case OnSubredditSelectedListener.FLAG_ITEM_CLICKED:
			selectSubreddit(sr, lastSelectedFilter);
			break;
		
		case OnSubredditSelectedListener.FLAG_LOAD_FINISHED:
			if (thingContainer != null && !isVisible(FRAG_THING_LIST)) {
				getSubredditListFragment().setSelectedSubreddit(sr);
				selectSubreddit(sr, lastSelectedFilter);
			}
			break;
		}		
	}
	
	private void selectSubreddit(Subreddit sr, int filter) {		
		FragmentManager manager = getFragmentManager();
		manager.removeOnBackStackChangedListener(this);
		if (singleContainer != null) {
			manager.popBackStackImmediate(FRAG_THING_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			manager.popBackStackImmediate();
		}
		manager.addOnBackStackChangedListener(this);
		
		refreshActionBar(sr, null, filter);
		refreshContainers(null);
		
		FragmentTransaction ft = manager.beginTransaction();		
		ControlFragment controlFrag = ControlFragment.newInstance(sr, null, -1, filter);
		ft.add(controlFrag, FRAG_CONTROL);
		ThingListFragment thingListFrag = ThingListFragment.newInstance(sr, filter, singleChoice);
		ft.replace(tlfContainerId, thingListFrag, FRAG_THING_LIST);
		if (singleContainer != null) {
			ft.addToBackStack(FRAG_THING_LIST);
		}
		ft.commit();
	}
	
	public void onThingSelected(Thing thing, int position) {
		selectThing(thing, thing.isSelf ? FRAG_COMMENT : FRAG_LINK, position);
	}
	
	private void selectThing(Thing thing, String tag, int position) {
		Fragment tf;
		String popTag;
		if (FRAG_LINK.equalsIgnoreCase(tag)) {
			tf = LinkFragment.newInstance(thing);
			popTag = FRAG_COMMENT;
		} else if (FRAG_COMMENT.equalsIgnoreCase(tag)) {
			tf = CommentListFragment.newInstance(thing.getId());
			popTag = FRAG_LINK;
		} else {
			throw new IllegalArgumentException(tag);
		}
		
		FragmentManager manager = getFragmentManager();
		manager.removeOnBackStackChangedListener(this);
		if (singleContainer != null) {
			manager.popBackStackImmediate(popTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			manager.popBackStackImmediate();			
		}
		manager.addOnBackStackChangedListener(this);
	
		FragmentTransaction ft = manager.beginTransaction();
		ControlFragment cf = ControlFragment.newInstance(getSubreddit(), thing, position, getFilter());
		ft.add(cf, FRAG_CONTROL);
		ft.replace(tContainerId, tf, tag);		
		ft.addToBackStack(tag);
		ft.commit();
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
		return (ControlFragment) getFragmentManager().findFragmentByTag(FRAG_CONTROL);
	}
	
	private SubredditListFragment getSubredditListFragment() {
		return (SubredditListFragment) getFragmentManager().findFragmentByTag(FRAG_SUBREDDIT_LIST);
	}
	
	private ThingListFragment getThingListFragment() {
		return (ThingListFragment) getFragmentManager().findFragmentByTag(FRAG_THING_LIST);
	}
	
	public void onBackStackChanged() {
		Subreddit sr = getSubreddit();
		Thing t = getThing();
		refreshActionBar(sr, t, getFilter());
		refreshCheckedItems();
		refreshContainers(t);
		invalidateOptionsMenu();
	}
	
	private void refreshActionBar(Subreddit sr, Thing t, int filter) {
		if (t != null && singleContainer != null) {
			setThingNavigationMode(t);
		} else if (sr != null) {
			setThingListNavigationMode(sr);
		} else {
			setSubredditListNavigationMode();
		}
		
		bar.setDisplayHomeAsUpEnabled(singleContainer != null && sr != null || t != null || intentSubreddit != null);
		if (bar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
			bar.setSelectedNavigationItem(filter);
		}
	}
	
	private void setThingNavigationMode(Thing t) {
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		bar.setDisplayShowTitleEnabled(true);
		bar.setDisplayShowCustomEnabled(false);
		bar.setTitle(t.title);
	}
	
	private void setThingListNavigationMode(Subreddit sr) {
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(false);
		filterSpinner.setSubreddit(sr.getTitle(this));
	}
	
	private void setSubredditListNavigationMode() {
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		bar.setDisplayShowTitleEnabled(true);
		bar.setDisplayShowCustomEnabled(false);
		bar.setTitle(R.string.app_name);
	}
	
	private void refreshCheckedItems() {
		if (isVisible(FRAG_SUBREDDIT_LIST)) {
			getSubredditListFragment().setSelectedSubreddit(getSubreddit());
		}
		
		if (isVisible(FRAG_THING_LIST)) {
			getThingListFragment().setChosenPosition(getThingPosition());
		}
	}
	
	private void refreshContainers(Thing t) {
		if (subredditListContainer != null) {
			int v = intentSubreddit != null ? View.GONE : View.VISIBLE;
			subredditListContainer.setVisibility(v);
			navSeparator.setVisibility(v);
		}
		if (thingContainer != null) {
			thingContainer.setVisibility(t != null ? View.VISIBLE : View.GONE);
			if (navContainer != null) {
				navContainer.setVisibility(t != null ? View.GONE : View.VISIBLE);
			}
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
		menu.findItem(R.id.menu_refresh).setVisible(singleContainer != null && isVisible(FRAG_THING_LIST));
		menu.findItem(R.id.menu_link).setVisible(hasThing && !isSelf && isVisible(FRAG_COMMENT));
		menu.findItem(R.id.menu_comments).setVisible(hasThing && !isSelf && isVisible(FRAG_LINK));
		menu.findItem(R.id.menu_share).setVisible(hasThing);
		menu.findItem(R.id.menu_copy_url).setVisible(hasThing);
		menu.findItem(R.id.menu_view).setVisible(hasThing);		
		menu.findItem(R.id.menu_search_for_subreddits).setVisible(singleContainer == null || isVisible(FRAG_SUBREDDIT_LIST));
		updateShareActionIntent(thing);		
		return true;
	}
	
	private boolean isVisible(String tag) {
		Fragment f = getFragmentManager().findFragmentByTag(tag);
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
		bar.setDisplayShowTitleEnabled(false);
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
			refreshActionBar(getSubreddit(), getThing(), getFilter());
			break;
		
		default:
			throw new IllegalStateException("Unexpected request code: " + requestCode);
		}
	}
	
	public void onFocusChange(View v, boolean hasFocus) {
		if (v == searchView && !hasFocus) {
			refreshActionBar(getSubreddit(), getThing(), getFilter());
		}
	}
	
	private void handleHome() {
		FragmentManager manager = getFragmentManager();
		int count = manager.getBackStackEntryCount();
		if (count > 0) {
			manager.popBackStack();
		} else {
			finish();
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