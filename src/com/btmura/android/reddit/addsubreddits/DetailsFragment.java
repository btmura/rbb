package com.btmura.android.reddit.addsubreddits;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.addsubreddits.SubredditListFragment.OnSelectedListener;
import com.btmura.android.reddit.common.Formatter;

public class DetailsFragment extends ListFragment {
	
	static final String ARGS_SUBREDDIT_INFO = "s";
	static final String ARGS_POSITION = "p";

	public static DetailsFragment newInstance(SubredditInfo info, int position) {
		DetailsFragment f = new DetailsFragment();
		Bundle b = new Bundle(2);
		b.putParcelable(ARGS_SUBREDDIT_INFO, info);
		b.putInt(ARGS_POSITION, position);
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
		SubredditInfo sr = getArguments().getParcelable(ARGS_SUBREDDIT_INFO);
		added.add(sr);
		getListener().onSelected(added, -1, OnSelectedListener.EVENT_ACTION_ITEM_CLICKED);
	}
	
	private OnSelectedListener getListener() {
		return (OnSelectedListener) getActivity();
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
			return getArguments().getParcelable(ARGS_SUBREDDIT_INFO);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = getActivity().getLayoutInflater().inflate(R.layout.details_row, parent, false);
			
			SubredditInfo info = getItem(position);
			
			TextView title = (TextView) v.findViewById(R.id.title);
			title.setText(info.title);
			
			TextView desc = (TextView) v.findViewById(R.id.description);
			desc.setText(Formatter.format(info.description));
			desc.setMovementMethod(LinkMovementMethod.getInstance());
			
			return v;
		}
	}
}
