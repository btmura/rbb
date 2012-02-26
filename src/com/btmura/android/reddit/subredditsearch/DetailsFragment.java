package com.btmura.android.reddit.subredditsearch;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.subredditsearch.SubredditInfoListFragment.OnSelectedListener;

public class DetailsFragment extends ListFragment {
	
	private static final String ARGS_SUBREDDIT_INFO = "s";
	private static final String ARGS_POSITION = "p";

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
		SubredditInfo info = getSubredditInfo();
		setHasOptionsMenu(info != null);
		setEmptyText(getString(R.string.sr_search_instructions));
		setListAdapter(new DetailsAdapter(info));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add:
			handleAddSubreddit();
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void handleAddSubreddit() {
		List<SubredditInfo> added = new ArrayList<SubredditInfo>(1);
		added.add(getSubredditInfo());
		getListener().onSelected(added, -1, OnSelectedListener.EVENT_ACTION_ITEM_CLICKED);
	}
	
	class DetailsAdapter extends BaseAdapter {

		private SubredditInfo info;
		
		DetailsAdapter(SubredditInfo info) {
			this.info = info;
		}
		
		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return false;
		}
		
		public int getCount() {
			return info != null ? 1 : 0;
		}

		public SubredditInfo getItem(int position) {
			return info;
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
	
	public SubredditInfo getSubredditInfo() {
		return getArguments().getParcelable(ARGS_SUBREDDIT_INFO);
	}
	
	public int getPosition() {
		return getArguments().getInt(ARGS_POSITION);
	}
	
	private OnSelectedListener getListener() {
		return (OnSelectedListener) getActivity();
	}
}
