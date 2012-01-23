package com.btmura.android.reddit;

import android.content.Context;
import android.widget.ArrayAdapter;

public class CommentAdapter extends ArrayAdapter<String> {

	public CommentAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}
}
