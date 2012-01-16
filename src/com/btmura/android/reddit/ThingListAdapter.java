package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class ThingListAdapter extends ArrayAdapter<Thing> {

	public ThingListAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1);
	}
}
