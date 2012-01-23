package com.btmura.android.reddit;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CommentAdapter extends ArrayAdapter<Comment> {

	private static final int PADDING_NESTING = 7;
	private static final int PADDING_VERTICAL = 10;
	private static final int PADDING_HORIZONTAL = 5;

	public CommentAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_1);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = (TextView) super.getView(position, convertView, parent);
		Comment comment = getItem(position);
		view.setPadding(PADDING_HORIZONTAL + PADDING_NESTING * comment.nesting, 
				PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL);
		return view;
	}
}
