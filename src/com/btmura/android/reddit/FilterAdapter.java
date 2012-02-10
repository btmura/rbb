package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class FilterAdapter extends ArrayAdapter<String> {
	
	public static final int FILTER_HOT = 0;
	public static final int FILTER_NEW = 1;
	public static final int FILTER_CONTROVERSIAL = 2;
	public static final int FILTER_TOP = 3;

	public FilterAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
		add(context.getString(R.string.filter_hot));
		add(context.getString(R.string.filter_new));
		add(context.getString(R.string.filter_controversial));
		add(context.getString(R.string.filter_top));
	}
}
