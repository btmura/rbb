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

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.ArrayUtils;

public class ThingAdapter extends BaseCursorAdapter {

    private static final String[] PROJECTION = {
            Things._ID,
            Things.COLUMN_AUTHOR,
            Things.COLUMN_CREATED_UTC,
            Things.COLUMN_DOMAIN,
            Things.COLUMN_DOWNS,
            Things.COLUMN_KIND,
            Things.COLUMN_LIKES,
            Things.COLUMN_NUM_COMMENTS,
            Things.COLUMN_OVER_18,
            Things.COLUMN_PERMA_LINK,
            Things.COLUMN_SCORE,
            Things.COLUMN_SELF,
            Things.COLUMN_SUBREDDIT,
            Things.COLUMN_TITLE,
            Things.COLUMN_THING_ID,
            Things.COLUMN_THUMBNAIL_URL,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,
            Things.COLUMN_VOTE,
    };

    public static int INDEX_ID = 0;
    public static int INDEX_AUTHOR = 1;
    public static int INDEX_CREATED_UTC = 2;
    public static int INDEX_DOMAIN = 3;
    public static int INDEX_DOWNS = 4;
    public static int INDEX_KIND = 5;
    public static int INDEX_LIKES = 6;
    public static int INDEX_NUM_COMMENTS = 7;
    public static int INDEX_OVER_18 = 8;
    public static int INDEX_PERMA_LINK = 9;
    public static int INDEX_SCORE = 10;
    public static int INDEX_SELF = 11;
    public static int INDEX_SUBREDDIT = 12;
    public static int INDEX_TITLE = 13;
    public static int INDEX_THING_ID = 14;
    public static int INDEX_THUMBNAIL_URL = 15;
    public static int INDEX_UPS = 16;
    public static int INDEX_URL = 17;
    public static int INDEX_VOTE = 18;

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final long nowTimeMs = System.currentTimeMillis();
    private final LayoutInflater inflater;
    private final String parentSubreddit;
    private final OnVoteListener listener;
    private int thingBodyWidth;

    public static Uri createUri(String accountName, String sessionId, String subredditName,
            int filter, String more, String query, boolean sync) {
        Uri.Builder b = ThingProvider.CONTENT_URI.buildUpon()
                .appendQueryParameter(ThingProvider.PARAM_SYNC, Boolean.toString(sync))
                .appendQueryParameter(ThingProvider.PARAM_ACCOUNT, accountName)
                .appendQueryParameter(ThingProvider.PARAM_SESSION_ID, sessionId)
                .appendQueryParameter(ThingProvider.PARAM_SUBREDDIT, subredditName)
                .appendQueryParameter(ThingProvider.PARAM_FILTER, Integer.toString(filter));
        if (!TextUtils.isEmpty(query)) {
            b.appendQueryParameter(ThingProvider.PARAM_QUERY, query);
        }
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(ThingProvider.PARAM_MORE, more);
        }
        return b.build();
    }

    public static CursorLoader createLoader(Context context, Uri uri, String sessionId) {
        return new CursorLoader(context, uri, PROJECTION, Things.SELECTION_BY_SESSION_ID,
                ArrayUtils.toArray(sessionId), null);
    }

    public static void deleteSessionData(final Context context, final String sessionId) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                cr.delete(ThingProvider.CONTENT_URI, Things.SELECTION_BY_SESSION_ID,
                        ArrayUtils.toArray(sessionId));
            }
        });
    }

    public ThingAdapter(Context context, String parentSubreddit, OnVoteListener listener) {
        super(context, null, 0);
        this.inflater = LayoutInflater.from(context);
        this.parentSubreddit = parentSubreddit;
        this.listener = listener;
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getInt(position, INDEX_KIND);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int kind = cursor.getInt(INDEX_KIND);
        switch (kind) {
            case Things.KIND_THING:
                return new ThingView(context);

            case Things.KIND_MORE:
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            String author = cursor.getString(INDEX_AUTHOR);
            long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
            String domain = cursor.getString(INDEX_DOMAIN);
            int downs = cursor.getInt(INDEX_DOWNS);
            int numComments = cursor.getInt(INDEX_NUM_COMMENTS);
            boolean over18 = cursor.getInt(INDEX_OVER_18) == 1;
            int score = cursor.getInt(INDEX_SCORE);
            String subreddit = cursor.getString(INDEX_SUBREDDIT);
            String thingId = cursor.getString(INDEX_THING_ID);
            String thumbnailUrl = cursor.getString(INDEX_THUMBNAIL_URL);
            String title = cursor.getString(INDEX_TITLE);
            int ups = cursor.getInt(INDEX_UPS);

            int likes = cursor.getInt(INDEX_LIKES);
            int vote = cursor.getInt(INDEX_VOTE);
            if (likes != vote) {
                likes = vote;
            }

            ThingView tv = (ThingView) view;
            tv.setOnVoteListener(listener);
            tv.setData(author, createdUtc, domain, downs, likes, nowTimeMs, numComments, over18,
                    parentSubreddit, score, subreddit, thingBodyWidth, thingId, thumbnailUrl,
                    title, ups);
            thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
        }
    }

    public String getMoreThingId() {
        Cursor c = getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Things.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
    }

    public Bundle getThingBundle(int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeBundle(c);
        }
        return null;
    }

    private static Bundle makeBundle(Cursor c) {
        Bundle b = new Bundle(PROJECTION.length);
        b.putLong(Things._ID, c.getLong(INDEX_THING_ID));
        b.putString(Things.COLUMN_AUTHOR, c.getString(INDEX_AUTHOR));
        b.putLong(Things.COLUMN_CREATED_UTC, c.getLong(INDEX_CREATED_UTC));
        b.putString(Things.COLUMN_DOMAIN, c.getString(INDEX_DOMAIN));
        b.putInt(Things.COLUMN_DOWNS, c.getInt(INDEX_DOWNS));
        b.putInt(Things.COLUMN_LIKES, c.getInt(INDEX_LIKES));
        b.putInt(Things.COLUMN_NUM_COMMENTS, c.getInt(INDEX_NUM_COMMENTS));
        b.putBoolean(Things.COLUMN_OVER_18, c.getInt(INDEX_OVER_18) == 1);
        b.putString(Things.COLUMN_PERMA_LINK, c.getString(INDEX_PERMA_LINK));
        b.putInt(Things.COLUMN_SCORE, c.getInt(INDEX_SCORE));
        b.putBoolean(Things.COLUMN_SELF, c.getInt(INDEX_SELF) == 1);
        b.putString(Things.COLUMN_SUBREDDIT, c.getString(INDEX_SUBREDDIT));
        b.putString(Things.COLUMN_TITLE, c.getString(INDEX_TITLE));
        b.putString(Things.COLUMN_THING_ID, c.getString(INDEX_THING_ID));
        b.putString(Things.COLUMN_THUMBNAIL_URL, c.getString(INDEX_THUMBNAIL_URL));
        b.putInt(Things.COLUMN_UPS, c.getInt(INDEX_UPS));
        b.putString(Things.COLUMN_URL, c.getString(INDEX_URL));
        return b;
    }
}
