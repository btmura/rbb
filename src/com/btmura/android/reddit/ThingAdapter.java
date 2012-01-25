package com.btmura.android.reddit;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ThingAdapter extends ArrayAdapter<Thing> {

	public ThingAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView text = (TextView) super.getView(position, convertView, parent);
		text.setSingleLine();
		text.setEllipsize(TruncateAt.END);
		return text;
	}
}
