package com.btmura.android.reddit;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TopicAdapter extends ArrayAdapter<Topic> {

	public TopicAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1);
		add(Topic.newTopic("all"));
		add(Topic.newTopic("AskReddit"));
		add(Topic.newTopic("android"));
		add(Topic.newTopic("askscience"));
		add(Topic.newTopic("atheism"));
		add(Topic.newTopic("aww"));
		add(Topic.newTopic("funny"));
		add(Topic.newTopic("fitness"));
		add(Topic.newTopic("gaming"));
		add(Topic.newTopic("health"));
		add(Topic.newTopic("humor"));
		add(Topic.newTopic("IAmA"));
		add(Topic.newTopic("pics"));
		add(Topic.newTopic("politics"));
		add(Topic.newTopic("science"));
		add(Topic.newTopic("technology"));
		add(Topic.newTopic("todayilearned"));
		add(Topic.newTopic("videos"));
		add(Topic.newTopic("worldnews"));
		add(Topic.newTopic("WTF"));
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = (TextView) super.getView(position, convertView, parent);
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		return tv;
	}
}
