package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class ThingPartAdapter extends ArrayAdapter<ThingPart> {

	public ThingPartAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}
	
	@Override
	public boolean isEnabled(int position) {
		return getItem(position).type == ThingPart.TYPE_LINK;
	}
}
