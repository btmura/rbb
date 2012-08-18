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
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.ArrayUtils;

public class VoteProvider extends BaseProvider {

    public static final String TAG = "VoteProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.votes";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri CONTENT_URI = Uri.parse(VoteProvider.BASE_AUTHORITY_URI
            + Votes.TABLE_NAME);

    static final String MIME_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
            + VoteProvider.AUTHORITY + "." + Votes.TABLE_NAME;
    static final String MIME_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
            + VoteProvider.AUTHORITY + "." + Votes.TABLE_NAME;

    public static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_VOTES = 1;
    private static final int MATCH_ONE_VOTE = 2;
    static {
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME, MATCH_ALL_VOTES);
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME + "/#", MATCH_ONE_VOTE);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query uri: " + uri);
        }
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(Votes.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "insert uri: " + uri);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Votes.TABLE_NAME, null, values);
        if (id != -1) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                Log.d(TAG, "notifying others!");
                cr.notifyChange(ThingProvider.CONTENT_URI, null);
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "update uri: " + uri);
        }
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
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(ThingProvider.CONTENT_URI, null);
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete uri: " + uri);
        }
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
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(ThingProvider.CONTENT_URI, null);
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_VOTES:
                return MIME_TYPE_DIR;

            case MATCH_ONE_VOTE:
                return MIME_TYPE_ITEM;

            default:
                return null;
        }
    }

    public static void voteInBackground(final Context context, final String accountName,
            final String thingId, final int likes) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                String[] selectionArgs = ArrayUtils.toArray(accountName, thingId);

                ContentValues values = new ContentValues(3);
                values.put(Votes.COLUMN_ACCOUNT, accountName);
                values.put(Votes.COLUMN_THING_ID, thingId);
                values.put(Votes.COLUMN_VOTE, likes);

                Uri uri = CONTENT_URI.buildUpon()
                        .appendQueryParameter(VoteProvider.PARAM_NOTIFY_OTHERS,
                                Boolean.toString(true))
                        .build();
                int count = cr.update(uri, values, Votes.SELECTION_BY_ACCOUNT_AND_THING_ID,
                        selectionArgs);
                if (count == 0) {
                    Uri insertUri = cr.insert(uri, values);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "inserted: " + insertUri);
                    }
                } else if (BuildConfig.DEBUG) {
                    Log.d(TAG, "updated: " + count);
                }
            }
        });
    }
}
