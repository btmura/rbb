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
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = (TextView) super.getView(position, convertView, parent);
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		return tv;
	}
}
