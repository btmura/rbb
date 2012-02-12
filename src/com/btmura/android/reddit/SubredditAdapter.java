package com.btmura.android.reddit;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.RedditProvider.Subreddits;

public class SubredditAdapter extends SimpleCursorAdapter {

	private static final String[] PROJECTION = {
		Subreddits._ID,
		Subreddits.COLUMN_NAME,
	};
	private static final String[] FROM = {};
	private static final int[] TO = {};
	private static final String SORT = Subreddits.COLUMN_NAME + " ASC";
	
	public static CursorLoader createLoader(Context context) {
		return new CursorLoader(context, Subreddits.CONTENT_URI, PROJECTION, null, null, SORT);
	}
	
	public SubredditAdapter(Context context) {
		super(context, android.R.layout.simple_list_item_activated_1, null, FROM, TO, 0);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String name = cursor.getString(1);
		TextView tv = (TextView) view;
		tv.setSingleLine();
		tv.setEllipsize(TruncateAt.END);
		tv.setText(name);
	}
	
	public Subreddit getSubreddit(int position) {
		Cursor c = getCursor();
		if (!c.moveToPosition(position)) {
			throw new IllegalStateException();
		}
		String name = c.getString(1);
		return Subreddit.newInstance(name);
	}
}
