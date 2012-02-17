package com.btmura.android.reddit.addsubreddits;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.addsubreddits.SubredditListFragment.OnSubredditAddedListener;

public class AddSubredditsActivity extends Activity implements OnQueryTextListener, OnSubredditAddedListener {

	public static final String EXTRA_QUERY = "query";
	
	private static final String FRAG_SUBREDDITS = "s";
	private static final String FRAG_DETAILS = "d";
	
	private FragmentManager manager;
	private SearchView sv;
	private View singleContainer;
	private View detailsContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_subreddits);
	
		manager = getFragmentManager();
		
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setCustomView(R.layout.add_subreddits_search);
		
		sv = (SearchView) bar.getCustomView();
		sv.setOnQueryTextListener(this);
		
		singleContainer = findViewById(R.id.single_container);
		detailsContainer = findViewById(R.id.details_container);
		
		if (savedInstanceState == null) {
			String q = getIntent().getStringExtra(EXTRA_QUERY);
			if (q != null && !q.trim().isEmpty()) {
				sv.setQuery(q.trim(), true);
				sv.setFocusable(false);
			}
		}
	}
	
	public boolean onQueryTextChange(String newText) {
		return false;
	}
	
	public boolean onQueryTextSubmit(String query) {
		sv.clearFocus();
		FragmentTransaction ft = manager.beginTransaction();
		if (singleContainer != null) {
			ft.replace(R.id.single_container, SubredditListFragment.newInstance(query), FRAG_SUBREDDITS);
		} else {
			ft.replace(R.id.subreddits_container, SubredditListFragment.newInstance(query), FRAG_SUBREDDITS);
			Fragment details = manager.findFragmentByTag(FRAG_DETAILS);
			if (details != null) {
				ft.remove(details);
			}
		}
		ft.commit();
		return true;
	}
	
	public void onSubredditsAdded(List<SubredditInfo> added, int event) {
		switch (event) {
		case OnSubredditAddedListener.EVENT_LIST_ITEM_CLICKED:
			handleListItemClicked(added);
			break;
			
		case OnSubredditAddedListener.EVENT_ACTION_ITEM_CLICKED:
			handleActionItemClicked(added);
			break;
			
		default:
			throw new IllegalArgumentException("Unexpected event: " + event);
		}
	}
	
	private void handleListItemClicked(List<SubredditInfo> added) {
		FragmentTransaction ft = manager.beginTransaction();
		ft.replace(singleContainer != null ? R.id.single_container : R.id.details_container, 
				DetailsFragment.newInstance(added.get(0)), FRAG_DETAILS);
		ft.addToBackStack(null);
		ft.commit();	
	}
	
	private void handleActionItemClicked(List<SubredditInfo> added) {
		int size = added.size();
		ContentValues[] values = new ContentValues[size];
		for (int i = 0; i < size; i++) {
			values[i] = new ContentValues(1);
			values[i].put(Subreddits.COLUMN_NAME, added.get(i).displayName);
		}
		
		Provider.addSubredditsInBackground(getApplicationContext(), values);
		Toast.makeText(getApplicationContext(), getString(R.string.x_subreddits_added, added.size()), 
				Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
			
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}
}
