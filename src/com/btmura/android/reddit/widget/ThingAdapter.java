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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.btmura.android.reddit.provider.ThingProvider.Things;

public class ThingAdapter extends CursorAdapter {

    private static final String[] PROJECTION = {
            Things._ID,
            Things.COLUMN_AUTHOR,
            Things.COLUMN_CREATED_UTC,
            Things.COLUMN_DOMAIN,
            Things.COLUMN_DOWNS,
            Things.COLUMN_LIKES,
            Things.COLUMN_NAME,
            Things.COLUMN_NUM_COMMENTS,
            Things.COLUMN_OVER_18,
            Things.COLUMN_PERMA_LINK,
            Things.COLUMN_SCORE,
            Things.COLUMN_SELF,
            Things.COLUMN_SUBREDDIT,
            Things.COLUMN_TITLE,
            Things.COLUMN_THUMBNAIL_URL,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,
    };

    public static final int INDEX_AUTHOR = 1;
    public static final int INDEX_CREATED_UTC = 2;
    public static final int INDEX_DOMAIN = 3;
    public static final int INDEX_DOWNS = 4;
    public static final int INDEX_LIKES = 5;
    public static final int INDEX_NAME = 6;
    public static final int INDEX_NUM_COMMENTS = 7;
    public static final int INDEX_OVER_18 = 8;
    public static final int INDEX_PERMA_LINK = 9;
    public static final int INDEX_SCORE = 10;
    public static final int INDEX_SELF = 11;
    public static final int INDEX_SUBREDDIT = 12;
    public static final int INDEX_TITLE = 13;
    public static final int INDEX_THUMBNAIL_URL = 14;
    public static final int INDEX_UPS = 15;
    public static final int INDEX_URL = 16;

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private int thingBodyWidth;

    public static CursorLoader createLoader(Context context, String account, String subreddit,
            int filter) {
        Uri uri = Things.CONTENT_URI.buildUpon()
                .appendQueryParameter(Things.QUERY_PARAM_SYNC, Boolean.toString(true))
                .appendQueryParameter(Things.QUERY_PARAM_ACCOUNT, account)
                .appendQueryParameter(Things.QUERY_PARAM_SUBREDDIT, subreddit)
                .appendQueryParameter(Things.QUERY_PARAM_FILTER, Integer.toString(filter))
                .build();
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    public ThingAdapter(Context context) {
        super(context, null, 0);
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String domain = cursor.getString(INDEX_DOMAIN);
        int likes = cursor.getInt(INDEX_LIKES);
        int score = cursor.getInt(INDEX_SCORE);
        String thumbnailUrl = cursor.getString(INDEX_THUMBNAIL_URL);
        String title = cursor.getString(INDEX_TITLE);

        ThingView tv = (ThingView) view;
        tv.setData(domain, likes, score, thumbnailUrl, title);
        tv.setThingBodyWidth(thingBodyWidth);
        thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
    }

    public Bundle getThingBundle(int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeBundle(c);
        }
        return null;
    }

    private Bundle makeBundle(Cursor c) {
        Bundle b = new Bundle(PROJECTION.length - 1);
        b.putString(Things.COLUMN_AUTHOR, c.getString(INDEX_AUTHOR));
        b.putLong(Things.COLUMN_CREATED_UTC, c.getLong(INDEX_CREATED_UTC));
        b.putString(Things.COLUMN_DOMAIN, c.getString(INDEX_DOMAIN));
        b.putInt(Things.COLUMN_DOWNS, c.getInt(INDEX_DOWNS));
        b.putInt(Things.COLUMN_LIKES, c.getInt(INDEX_LIKES));
        b.putString(Things.COLUMN_NAME, c.getString(INDEX_NAME));
        b.putInt(Things.COLUMN_NUM_COMMENTS, c.getInt(INDEX_NUM_COMMENTS));
        b.putBoolean(Things.COLUMN_OVER_18, c.getInt(INDEX_OVER_18) == 1);
        b.putString(Things.COLUMN_PERMA_LINK, c.getString(INDEX_PERMA_LINK));
        b.putInt(Things.COLUMN_SCORE, c.getInt(INDEX_SCORE));
        b.putBoolean(Things.COLUMN_SELF, c.getInt(INDEX_SELF) == 1);
        b.putString(Things.COLUMN_SUBREDDIT, c.getString(INDEX_SUBREDDIT));
        b.putString(Things.COLUMN_TITLE, c.getString(INDEX_TITLE));
        b.putString(Things.COLUMN_THUMBNAIL_URL, c.getString(INDEX_THUMBNAIL_URL));
        b.putInt(Things.COLUMN_UPS, c.getInt(INDEX_UPS));
        b.putString(Things.COLUMN_URL, c.getString(INDEX_URL));
        return b;
    }
}
