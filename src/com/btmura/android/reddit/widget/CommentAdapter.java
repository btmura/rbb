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

import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.provider.CommentProvider;
import com.btmura.android.reddit.util.ArrayUtils;

public class CommentAdapter extends CursorAdapter {

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
            Comments.COLUMN_SESSION_ID,
            Comments.COLUMN_TITLE,
            Comments.COLUMN_THING_ID,
            Comments.COLUMN_UPS,
            Comments.COLUMN_VOTE,
    };

    private static int INDEX_ID = -1;
    private static int INDEX_AUTHOR;
    private static int INDEX_BODY;
    private static int INDEX_CREATED_UTC;
    private static int INDEX_DOWNS;
    private static int INDEX_KIND;
    private static int INDEX_LIKES;
    private static int INDEX_NESTING;
    private static int INDEX_NUM_COMMENTS;
    private static int INDEX_SEQUENCE;
    private static int INDEX_SESSION_ID;
    private static int INDEX_TITLE;
    private static int INDEX_THING_ID;
    private static int INDEX_UPS;
    private static int INDEX_VOTE;

    private final long nowTimeMs = System.currentTimeMillis();
    private final OnVoteListener listener;

    public static Uri createUri(String accountName, String thingId, boolean sync) {
        return CommentProvider.CONTENT_URI.buildUpon()
                .appendQueryParameter(CommentProvider.PARAM_SYNC, Boolean.toString(sync))
                .appendQueryParameter(CommentProvider.PARAM_ACCOUNT_NAME, accountName)
                .appendQueryParameter(CommentProvider.PARAM_THING_ID, thingId)
                .build();
    }

    public static CursorLoader createLoader(Context context, Uri uri, String thingId) {
        return new CursorLoader(context, uri, PROJECTION, Comments.SELECTION_BY_SESSION_ID,
                ArrayUtils.toArray(thingId), Comments.SORT_BY_SEQUENCE_AND_ID);
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
        initColumnIndices(cursor);

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

    public Bundle getCommentBundle(int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            initColumnIndices(c);
            return makeBundle(c);
        }
        return null;
    }

    private static void initColumnIndices(Cursor c) {
        if (INDEX_ID == -1) {
            INDEX_ID = c.getColumnIndexOrThrow(Comments._ID);
            INDEX_AUTHOR = c.getColumnIndexOrThrow(Comments.COLUMN_AUTHOR);
            INDEX_BODY = c.getColumnIndexOrThrow(Comments.COLUMN_BODY);
            INDEX_CREATED_UTC = c.getColumnIndexOrThrow(Comments.COLUMN_CREATED_UTC);
            INDEX_DOWNS = c.getColumnIndexOrThrow(Comments.COLUMN_DOWNS);
            INDEX_KIND = c.getColumnIndexOrThrow(Comments.COLUMN_KIND);
            INDEX_LIKES = c.getColumnIndexOrThrow(Comments.COLUMN_LIKES);
            INDEX_NESTING = c.getColumnIndexOrThrow(Comments.COLUMN_NESTING);
            INDEX_NUM_COMMENTS = c.getColumnIndexOrThrow(Comments.COLUMN_NUM_COMMENTS);
            INDEX_SEQUENCE = c.getColumnIndexOrThrow(Comments.COLUMN_SEQUENCE);
            INDEX_SESSION_ID = c.getColumnIndexOrThrow(Comments.COLUMN_SESSION_ID);
            INDEX_TITLE = c.getColumnIndexOrThrow(Comments.COLUMN_TITLE);
            INDEX_THING_ID = c.getColumnIndexOrThrow(Comments.COLUMN_THING_ID);
            INDEX_UPS = c.getColumnIndexOrThrow(Comments.COLUMN_UPS);
            INDEX_VOTE = c.getColumnIndexOrThrow(Comments.COLUMN_VOTE);
        }
    }

    private static Bundle makeBundle(Cursor c) {
        Bundle b = new Bundle(PROJECTION.length);
        b.putLong(Comments._ID, c.getLong(INDEX_ID));
        b.putString(Comments.COLUMN_AUTHOR, c.getString(INDEX_AUTHOR));
        b.putString(Comments.COLUMN_BODY, c.getString(INDEX_BODY));
        b.putLong(Comments.COLUMN_CREATED_UTC, c.getLong(INDEX_CREATED_UTC));
        b.putInt(Comments.COLUMN_DOWNS, c.getInt(INDEX_DOWNS));
        b.putInt(Comments.COLUMN_KIND, c.getInt(INDEX_KIND));
        b.putInt(Comments.COLUMN_LIKES, c.getInt(INDEX_LIKES));
        b.putInt(Comments.COLUMN_NESTING, c.getInt(INDEX_NESTING));
        b.putInt(Comments.COLUMN_NUM_COMMENTS, c.getInt(INDEX_NUM_COMMENTS));
        b.putInt(Comments.COLUMN_SEQUENCE, c.getInt(INDEX_SEQUENCE));
        b.putString(Comments.COLUMN_SESSION_ID, c.getString(INDEX_SESSION_ID));
        b.putString(Comments.COLUMN_TITLE, c.getString(INDEX_TITLE));
        b.putString(Comments.COLUMN_THING_ID, c.getString(INDEX_THING_ID));
        b.putInt(Comments.COLUMN_UPS, c.getInt(INDEX_UPS));
        b.putInt(Comments.COLUMN_VOTE, c.getInt(INDEX_VOTE));
        return b;
    }
}
