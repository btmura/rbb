/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.SubredditSearchLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.SubredditProvider;

public class SubredditAdapter extends SimpleCursorAdapter {

    private static final String[] PROJECTION = {Subreddits._ID, Subreddits.COLUMN_NAME};

    private static final String[] FROM = {};
    private static final int[] TO = {};

    public static Loader<Cursor> createLoader(Context context, String accountName, String query) {
        if (query != null) {
            return new SubredditSearchLoader(context, Urls.subredditSearchUrl(query, null));
        } else {
            return new CursorLoader(context,
                    SubredditProvider.CONTENT_URI,
                    PROJECTION,
                    SubredditProvider.SELECTION_ACCOUNT_NOT_DELETED,
                    new String[] {accountName},
                    Subreddits.SORT_BY_NAME);
        }
    }

    private String query;
    private boolean singleChoice;
    private Subreddit selected;

    public SubredditAdapter(Context context, String query, boolean singleChoice) {
        super(context, getLayout(query), null, FROM, TO, 0);
        this.query = query;
        this.singleChoice = singleChoice;
    }

    private static int getLayout(String query) {
        return query != null ? R.layout.sr_search_row : R.layout.sr_db_row;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder h = (ViewHolder) view.getTag();
        if (h == null) {
            h = new ViewHolder();
            h.title = (TextView) view.findViewById(R.id.title);
            h.status = (TextView) view.findViewById(R.id.status);
            view.setTag(h);
        }

        String name = cursor.getString(1);
        if (TextUtils.isEmpty(name)) {
            h.title.setText(R.string.front_page);
        } else {
            h.title.setText(name);
        }

        if (query != null) {
            int subscribers = cursor.getInt(2);
            String status = context.getResources().getQuantityString(R.plurals.subscribers,
                    subscribers, subscribers);
            h.status.setText(status);
        }

        if (singleChoice && selected != null && name.equalsIgnoreCase(selected.name)) {
            view.setBackgroundResource(R.drawable.selector_chosen);
        } else {
            view.setBackgroundResource(R.drawable.selector_normal);
        }
    }

    static class ViewHolder {
        TextView title;
        TextView status;
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
