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

package com.btmura.android.reddit.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;

public class VoteProvider extends BaseProvider {

    public static final String TAG = "VoteProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.votes";

    private static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_VOTES = 1;
    private static final int MATCH_ONE_VOTE = 2;
    static {
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME, MATCH_ALL_VOTES);
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME + "/#", MATCH_ONE_VOTE);
    }

    public static class Votes implements BaseColumns {
        static final String TABLE_NAME = "votes";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + TABLE_NAME);

        public static final String COLUMN_ACCOUNT = "account";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_VOTE = "vote";
        public static final String COLUMN_STATE = "state";

        public static final int VOTE_UP = 1;
        public static final int VOTE_DOWN = -1;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(Votes.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Votes.TABLE_NAME, null, values);
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Votes.TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Votes.TABLE_NAME, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static void insertMultipleVotesInBackground(final Context context, final ContentValues[] values) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = context.getContentResolver();
                cr.bulkInsert(Votes.CONTENT_URI, values);
                return null;
            }
        }.execute();
    }
}
