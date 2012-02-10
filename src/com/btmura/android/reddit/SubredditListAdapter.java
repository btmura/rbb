package com.btmura.android.reddit;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SubredditListAdapter extends ArrayAdapter<Subreddit> {

	public SubredditListAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1);
		add(Subreddit.newInstance("all"));
		add(Subreddit.newInstance("AskReddit"));
		add(Subreddit.newInstance("android"));
		add(Subreddit.newInstance("askscience"));
		add(Subreddit.newInstance("atheism"));
		add(Subreddit.newInstance("aww"));
		add(Subreddit.newInstance("funny"));
		add(Subreddit.newInstance("fitness"));
		add(Subreddit.newInstance("gaming"));
		add(Subreddit.newInstance("health"));
		add(Subreddit.newInstance("humor"));
		add(Subreddit.newInstance("IAmA"));
		add(Subreddit.newInstance("pics"));
		add(Subreddit.newInstance("politics"));
		add(Subreddit.newInstance("science"));
		add(Subreddit.newInstance("technology"));
		add(Subreddit.newInstance("todayilearned"));
		add(Subreddit.newInstance("videos"));
		add(Subreddit.newInstance("worldnews"));
		add(Subreddit.newInstance("WTF"));
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = (TextView) super.getView(position, convertView, parent);
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		return tv;
	}
}
