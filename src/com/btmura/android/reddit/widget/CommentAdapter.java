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
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.provider.CommentProvider;
import com.btmura.android.reddit.util.Array;

public class CommentAdapter extends BaseCursorAdapter {

    private static final String[] PROJECTION = {
            Comments._ID,
            Comments.COLUMN_AUTHOR,
            Comments.COLUMN_BODY,
            Comments.COLUMN_CREATED_UTC,
            Comments.COLUMN_DOWNS,
            Comments.COLUMN_KIND,
            Comments.COLUMN_LIKES,
            Comments.COLUMN_NESTING,
            Comments.COLUMN_NUM_COMMENTS,
            Comments.COLUMN_SEQUENCE,
            Comments.COLUMN_SESSION_CREATION_TIME,
            Comments.COLUMN_SESSION_ID,
            Comments.COLUMN_TITLE,
            Comments.COLUMN_THING_ID,
            Comments.COLUMN_UPS,
            Comments.COLUMN_VOTE,
    };

    public static int INDEX_ID = 0;
    public static int INDEX_AUTHOR = 1;
    public static int INDEX_BODY = 2;
    public static int INDEX_CREATED_UTC = 3;
    public static int INDEX_DOWNS = 4;
    public static int INDEX_KIND = 5;
    public static int INDEX_LIKES = 6;
    public static int INDEX_NESTING = 7;
    public static int INDEX_NUM_COMMENTS = 8;
    public static int INDEX_SEQUENCE = 9;
    public static int INDEX_SESSION_CREATION_TIME = 10;
    public static int INDEX_SESSION_ID = 11;
    public static int INDEX_TITLE = 12;
    public static int INDEX_THING_ID = 13;
    public static int INDEX_UPS = 14;
    public static int INDEX_VOTE = 15;

    private final long nowTimeMs = System.currentTimeMillis();
    private final OnVoteListener listener;

    public static Loader<Cursor> getLoader(Context context, String accountName, String sessionId,
            String thingId, boolean sync) {
        Uri uri = getUri(accountName, sessionId, thingId, sync);
        return new CursorLoader(context, uri, PROJECTION, Comments.SELECTION_BY_SESSION_ID,
                Array.of(sessionId), Comments.SORT_BY_SEQUENCE_AND_ID);
    }

    public static void updateLoader(Context context, Loader<Cursor> loader, String accountName,
            String sessionId, String thingId, boolean sync) {
        if (loader instanceof CursorLoader) {
            CursorLoader cl = (CursorLoader) loader;
            cl.setUri(getUri(accountName, sessionId, thingId, sync));
        }
    }

    public static void deleteSessionData(final Context context, final String sessionId) {
        // Use application context to allow activity to be collected and
        // schedule the session deletion in the background thread pool rather
        // than serial pool.
        final Context appContext = context.getApplicationContext();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentResolver cr = appContext.getContentResolver();
                cr.delete(CommentProvider.CONTENT_URI, Comments.SELECTION_BY_SESSION_ID,
                        Array.of(sessionId));
            }
        });
    }

    private static Uri getUri(String accountName, String sessionId, String thingId, boolean sync) {
        return CommentProvider.CONTENT_URI.buildUpon()
                .appendQueryParameter(CommentProvider.PARAM_SYNC, Boolean.toString(sync))
                .appendQueryParameter(CommentProvider.PARAM_ACCOUNT_NAME, accountName)
                .appendQueryParameter(CommentProvider.PARAM_SESSION_ID, sessionId)
                .appendQueryParameter(CommentProvider.PARAM_THING_ID, thingId)
                .build();
    }

    public CommentAdapter(Context context, OnVoteListener listener) {
        super(context, null, 0);
        this.listener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new CommentView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String author = cursor.getString(INDEX_AUTHOR);
        String body = cursor.getString(INDEX_BODY);
        long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int downs = cursor.getInt(INDEX_DOWNS);
        int kind = cursor.getInt(INDEX_KIND);
        int nesting = cursor.getInt(INDEX_NESTING);
        int numComments = cursor.getInt(INDEX_NUM_COMMENTS);
        String title = cursor.getString(INDEX_TITLE);
        String thingId = cursor.getString(INDEX_THING_ID);
        int ups = cursor.getInt(INDEX_UPS);

        int likes = cursor.getInt(INDEX_LIKES);
        int vote = cursor.getInt(INDEX_VOTE);
        if (likes != vote) {
            likes = vote;
        }

        CommentView cv = (CommentView) view;
        cv.setOnVoteListener(listener);
        cv.setData(author, body, createdUtc, downs, kind, likes, nesting, nowTimeMs, numComments,
                title, thingId, ups);
    }
}
