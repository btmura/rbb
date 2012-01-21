package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class TopicAdapter extends ArrayAdapter<Topic> {

	public TopicAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1);
		add(Topic.frontPage());
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
}
