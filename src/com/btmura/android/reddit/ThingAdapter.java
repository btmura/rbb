package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class ThingAdapter extends ArrayAdapter<String> {

	public ThingAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}
}
