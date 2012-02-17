package com.btmura.android.reddit.addsubreddits;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.addsubreddits.SubredditListFragment.OnSubredditAddedListener;
import com.btmura.android.reddit.common.Formatter;

public class DetailsFragment extends ListFragment {
	
	private static final String ARGS_SUBREDDIT = "sr";

	public static DetailsFragment newInstance(SubredditInfo sr) {
		DetailsFragment f = new DetailsFragment();
		Bundle b = new Bundle(1);
		b.putParcelable(ARGS_SUBREDDIT, sr);
		f.setArguments(b);
		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		setListAdapter(new DetailsAdapter());
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.details, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_subreddit:
			handleAddSubreddit();
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void handleAddSubreddit() {
		List<SubredditInfo> added = new ArrayList<SubredditInfo>(1);
		SubredditInfo sr = getArguments().getParcelable(ARGS_SUBREDDIT);
		added.add(sr);
		getListener().onSubredditsAdded(added, OnSubredditAddedListener.EVENT_ACTION_ITEM_CLICKED);
	}
	
	private OnSubredditAddedListener getListener() {
		return (OnSubredditAddedListener) getActivity();
	}
	
	class DetailsAdapter extends BaseAdapter {

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return false;
		}
		
		public int getCount() {
			return 1;
		}

		public SubredditInfo getItem(int position) {
			return getArguments().getParcelable(ARGS_SUBREDDIT);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = getActivity().getLayoutInflater().inflate(R.layout.details_row, parent, false);
			
			SubredditInfo sr = getItem(position);
			
			TextView title = (TextView) v.findViewById(R.id.title);
			title.setText(sr.title);
			
			TextView desc = (TextView) v.findViewById(R.id.description);
			desc.setText(Formatter.format(sr.description));
			
			return v;
		}
	}
}
