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
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

public class CommentAdapter extends BaseCursorAdapter {

    private static final String[] PROJECTION = {
            Things._ID,
            Things.COLUMN_AUTHOR,
            Things.COLUMN_BODY,
            Things.COLUMN_CREATED_UTC,
            Things.COLUMN_DOWNS,
            Things.COLUMN_EXPANDED,
            Things.COLUMN_KIND,
            Things.COLUMN_LIKES,
            Things.COLUMN_NESTING,
            Things.COLUMN_NUM_COMMENTS,
            Things.COLUMN_PERMA_LINK,
            Things.COLUMN_SELF,
            Things.COLUMN_SEQUENCE,
            Things.COLUMN_SESSION_ID,
            Things.COLUMN_SESSION_TIMESTAMP,
            Things.COLUMN_TITLE,
            Things.COLUMN_THING_ID,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,
            Things.COLUMN_VOTE,
    };

    public static int INDEX_ID = 0;
    public static int INDEX_AUTHOR = 1;
    public static int INDEX_BODY = 2;
    public static int INDEX_CREATED_UTC = 3;
    public static int INDEX_DOWNS = 4;
    public static int INDEX_EXPANDED = 5;
    public static int INDEX_KIND = 6;
    public static int INDEX_LIKES = 7;
    public static int INDEX_NESTING = 8;
    public static int INDEX_NUM_COMMENTS = 9;
    public static int INDEX_PERMA_LINK = 10;
    public static int INDEX_SELF = 11;
    public static int INDEX_SEQUENCE = 12;
    public static int INDEX_SESSION_ID = 13;
    public static int INDEX_SESSION_CREATION_TIME = 14;
    public static int INDEX_TITLE = 15;
    public static int INDEX_THING_ID = 16;
    public static int INDEX_UPS = 17;
    public static int INDEX_URL = 18;
    public static int INDEX_VOTE = 19;

    private final long nowTimeMs = System.currentTimeMillis();
    private final String accountName;
    private final OnVoteListener listener;

    public static Loader<Cursor> getLoader(Context context, String accountName, String sessionId,
            String thingId, String linkId, boolean sync) {
        Uri uri = getUri(accountName, sessionId, thingId, linkId, sync);
        return new CursorLoader(context, uri, PROJECTION, Things.SELECT_VISIBLE_BY_SESSION_ID,
                Array.of(sessionId), Things.SORT_BY_SEQUENCE_AND_ID);
    }

    public static void updateLoader(Context context, Loader<Cursor> loader, String accountName,
            String sessionId, String thingId, String linkId, boolean sync) {
        if (loader instanceof CursorLoader) {
            CursorLoader cl = (CursorLoader) loader;
            cl.setUri(getUri(accountName, sessionId, thingId, linkId, sync));
        }
    }

    public static void deleteSessionData(final Context context, final String sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentResolver cr = appContext.getContentResolver();
                cr.delete(ThingProvider.THINGS_URI, Things.SELECT_BY_SESSION_ID,
                        Array.of(sessionId));
            }
        });
    }

    private static Uri getUri(String accountName, String sessionId, String thingId, String linkId,
            boolean fetch) {
        Uri.Builder b = ThingProvider.THINGS_URI.buildUpon()
                .appendQueryParameter(ThingProvider.PARAM_FETCH_COMMENTS, Boolean.toString(fetch))
                .appendQueryParameter(ThingProvider.PARAM_ACCOUNT, accountName)
                .appendQueryParameter(ThingProvider.PARAM_SESSION_ID, sessionId)
                .appendQueryParameter(ThingProvider.PARAM_THING_ID, thingId)
                .appendQueryParameter(ThingProvider.PARAM_JOIN_VOTES, Boolean.toString(true));
        if (!TextUtils.isEmpty(linkId)) {
            b.appendQueryParameter(ThingProvider.PARAM_LINK_ID, linkId);
        }
        return b.build();
    }

    public CommentAdapter(Context context, String accountName, OnVoteListener listener) {
        super(context, null, 0);
        this.accountName = accountName;
        this.listener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String author = cursor.getString(INDEX_AUTHOR);
        String body = cursor.getString(INDEX_BODY);
        long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int downs = cursor.getInt(INDEX_DOWNS);
        boolean expanded = cursor.getInt(INDEX_EXPANDED) == 1;
        int kind = cursor.getInt(INDEX_KIND);
        int nesting = cursor.getInt(INDEX_NESTING);
        int numComments = cursor.getInt(INDEX_NUM_COMMENTS);
        String title = cursor.getString(INDEX_TITLE);
        String thingId = cursor.getString(INDEX_THING_ID);
        int ups = cursor.getInt(INDEX_UPS);

        // Comments don't have a score so calculate our own.
        int score = ups - downs;

        // TODO: Remove code duplication with ThingAdapter.
        // Local votes take precedence over those from reddit.
        int likes = cursor.getInt(INDEX_LIKES);
        if (!cursor.isNull(INDEX_VOTE)) {
            // Local votes take precedence over those from reddit.
            likes = cursor.getInt(INDEX_VOTE);

            score += likes;
        }

        ThingView tv = (ThingView) view;
        tv.setData(accountName, author, body, createdUtc, null, downs, expanded, kind, likes, null,
                nesting, nowTimeMs, numComments, false, null, score, null, 0, thingId, null, title,
                ups);
        tv.setOnVoteListener(listener);
    }
}
