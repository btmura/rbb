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
import com.btmura.android.reddit.database.Replies;
import com.btmura.android.reddit.util.ArrayUtils;

public class ReplyProvider extends BaseProvider {

    public static final String TAG = "ReplyProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.replies";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

    public static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_REPLIES = 1;
    static {
        MATCHER.addURI(AUTHORITY, Replies.TABLE_NAME, MATCH_ALL_REPLIES);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query: " + uri.getQuery());
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(Replies.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Replies.TABLE_NAME, null, values);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "insert id: " + id);
        }
        if (id != -1) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Replies.TABLE_NAME, values, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "update count: " + count);
        }
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Replies.TABLE_NAME, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete count: " + count);
        }
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(uri, null);
            if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
                cr.notifyChange(CommentProvider.CONTENT_URI, null);
            }
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static void replyInBackground(final Context context, final String accountName,
            final String parentThingId, final String thingId, final String text) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();

                ContentValues v = new ContentValues(4);
                v.put(Replies.COLUMN_ACCOUNT, accountName);
                v.put(Replies.COLUMN_PARENT_THING_ID, parentThingId);
                v.put(Replies.COLUMN_THING_ID, thingId);
                v.put(Replies.COLUMN_TEXT, text);

                Uri uri = CONTENT_URI.buildUpon()
                        .appendQueryParameter(PARAM_NOTIFY_OTHERS, Boolean.toString(true))
                        .build();
                int count = cr.update(uri, v, Replies.SELECTION_BY_ACCOUNT_AND_THING_ID,
                        ArrayUtils.toArray(accountName, thingId));
                if (count == 0) {
                    cr.insert(uri, v);
                }
            }
        });
    }
}