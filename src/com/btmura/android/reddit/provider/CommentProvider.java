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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.ArrayUtils;

public class CommentProvider extends BaseProvider {

    public static final String TAG = "CommentProvider";
    public static final boolean DEBUG = Debug.DEBUG;

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.comments";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri CONTENT_URI = Uri.parse(CommentProvider.BASE_AUTHORITY_URI);

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_ACCOUNT_NAME = "accountName";
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
        if (DEBUG) {
            Log.d(TAG, "query uri: " + uri);
        }

        if (uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            sync(uri);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(COMMENTS_WITH_VOTES, projection, selection, selectionArgs,
                null, null, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri) {
        Cursor c = null;
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
            String cookie = AccountUtils.getCookie(context, accountName);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);

            long t1 = SystemClock.currentThreadTimeMillis();
            CommentListing listing = new CommentListing(context, accountName, cookie, thingId);
            listing.process();

            long t2 = SystemClock.currentThreadTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            try {
                db.beginTransaction();
                db.delete(Comments.TABLE_NAME, Comments.SELECTION_BY_SESSION_ID,
                        ArrayUtils.toArray(thingId));

                InsertHelper insertHelper = new InsertHelper(db, Comments.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();

                if (DEBUG) {
                    long t3 = SystemClock.currentThreadTimeMillis();
                    Log.d(TAG, "c: " + count + " p: " + (t2 - t1) + " db: " + (t3 - t2));
                }
            } finally {
                db.endTransaction();
                db.close();
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
        if (DEBUG) {
            Log.d(TAG, "insert");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Comments.TABLE_NAME, null, values);
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (DEBUG) {
            Log.d(TAG, "update");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Comments.TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DEBUG) {
            Log.d(TAG, "delete");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Comments.TABLE_NAME, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}