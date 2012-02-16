package com.btmura.android.reddit.addsubreddits;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.addsubreddits.SubredditListFragment.OnSubredditAddedListener;

public class AddSubredditsActivity extends Activity implements OnQueryTextListener, OnSubredditAddedListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_subreddits);
	
		ActionBar bar = getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setDisplayShowTitleEnabled(false);
		bar.setDisplayShowCustomEnabled(true);
		bar.setCustomView(R.layout.add_subreddits_search);
		
		SearchView sv = (SearchView) bar.getCustomView();
		sv.setOnQueryTextListener(this);
	}
	
	public boolean onQueryTextChange(String newText) {
		return false;
	}
	
	public boolean onQueryTextSubmit(String query) {
		FragmentManager manager = getFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
		ft.replace(R.id.single_container, SubredditListFragment.newInstance(query));
		ft.commit();
		return true;
	}
	
	public void onSubredditsAdded(ArrayList<String> subreddits) {
		Provider.addSubredditsInBackground(getApplicationContext(), subreddits);
		finish();
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
