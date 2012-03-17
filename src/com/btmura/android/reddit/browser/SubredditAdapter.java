package com.btmura.android.reddit.browser;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.Provider.Subreddits;

public class SubredditAdapter extends SimpleCursorAdapter {

    private static final String[] PROJECTION = { Subreddits._ID, Subreddits.COLUMN_NAME, };
    private static final String[] FROM = {};
    private static final int[] TO = {};

    public static CursorLoader createLoader(Context context) {
        return new CursorLoader(context, Subreddits.CONTENT_URI, PROJECTION, null, null, Subreddits.SORT);
    }

    private boolean singleChoice;
    private Subreddit selected;

    public SubredditAdapter(Context context, boolean singleChoice) {
        super(context, R.layout.sr_row, null, FROM, TO, 0);
        this.singleChoice = singleChoice;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tv = (TextView) view;
        tv.setSingleLine();
        tv.setEllipsize(TruncateAt.END);

        String name = cursor.getString(1);
        if (TextUtils.isEmpty(name)) {
            tv.setText(R.string.front_page);
        } else {
            tv.setText(name);
        }

        if (singleChoice && selected != null && name.equalsIgnoreCase(selected.name)) {
            tv.setBackgroundResource(R.drawable.selector_chosen);
        } else {
            tv.setBackgroundResource(R.drawable.selector_normal);
        }
    }

    public void setSelectedSubreddit(Subreddit sr) {
        selected = sr;
    }

    public String getName(Context context, int position) {
        Cursor c = getCursor();
        if (!c.moveToPosition(position)) {
            throw new IllegalStateException();
        }
        return c.getString(1);
    }
}
