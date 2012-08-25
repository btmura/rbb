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

import java.io.IOException;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Votes;

public class CommentProvider extends BaseProvider {

    public static final String TAG = "CommentProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.comments";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_ACCOUNT_NAME = "accountName";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_THING_ID = "thingId";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_COMMENTS = 1;
    static {
        MATCHER.addURI(AUTHORITY, Comments.TABLE_NAME, MATCH_ALL_COMMENTS);
    }

    private static final String COMMENTS_WITH_VOTES = Comments.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Comments.COLUMN_THING_ID + ")";

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query: " + uri.getQuery());
        }

        String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
        if (uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            sync(uri, sessionId);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(COMMENTS_WITH_VOTES, projection, selection, selectionArgs, null, null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri, String sessionId) {
        Cursor c = null;
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
            String cookie = AccountUtils.getCookie(context, accountName);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);

            CommentListing listing = new CommentListing(context, accountName, sessionId, thingId,
                    cookie);
            listing.process();

            long t1 = System.currentTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            try {
                db.beginTransaction();
                InsertHelper insertHelper = new InsertHelper(db, Comments.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                db.close();
            }
            if (BuildConfig.DEBUG) {
                long t2 = System.currentTimeMillis();
                Log.d(TAG, "sync network: " + listing.networkTimeMs + " parse: "
                        + listing.parseTimeMs + " db: " + (t2 - t1));
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, "sync", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "sync", e);
        } catch (IOException e) {
            Log.e(TAG, "sync", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Comments.TABLE_NAME, null, values);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "insert id: " + id);
        }
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Comments.TABLE_NAME, values, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "update count: " + count);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Comments.TABLE_NAME, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete count: " + count);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void insertPlaceholderInBackground(Context context,
            final String accountName, final String body, final int nesting, final int sequence,
            final String sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentValues v = new ContentValues(7);
                v.put(Comments.COLUMN_ACCOUNT, accountName);
                v.put(Comments.COLUMN_AUTHOR, accountName);
                v.put(Comments.COLUMN_BODY, body);
                v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
                v.put(Comments.COLUMN_NESTING, nesting);
                v.put(Comments.COLUMN_SEQUENCE, sequence);
                v.put(Comments.COLUMN_SESSION_ID, sessionId);

                ContentResolver cr = appContext.getContentResolver();
                cr.insert(CONTENT_URI, v);
            }
        });
    }
}
