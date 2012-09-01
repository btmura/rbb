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
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class ThingProvider extends SessionProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI);

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_SUBREDDIT = "subreddit";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_MORE = "more";
    public static final String PARAM_QUERY = "query";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_THINGS = 1;
    private static final int MATCH_ONE_THING = 2;
    static {
        MATCHER.addURI(AUTHORITY, Things.TABLE_NAME, MATCH_ALL_THINGS);
        MATCHER.addURI(AUTHORITY, Things.TABLE_NAME + "/#", MATCH_ONE_THING);
    }

    private static final String TABLE_NAME_WITH_VOTES = Things.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")";

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query uri: " + uri.getQuery());
        }

        if (uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            sync(uri);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME_WITH_VOTES, projection, selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri) {
        try {
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
            String subredditName = uri.getQueryParameter(PARAM_SUBREDDIT);
            int filter = Integer.parseInt(uri.getQueryParameter(PARAM_FILTER));
            String more = uri.getQueryParameter(PARAM_MORE);
            String query = uri.getQueryParameter(PARAM_QUERY);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            ThingListing listing = ThingListing.get(context, accountName, sessionId, subredditName,
                    filter, more, query, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old things that can't possibly be viewed anymore.
                cleaned = db.delete(Things.TABLE_NAME, Things.SELECTION_BEFORE_CREATION_TIME,
                        Array.of(getCreationTimeCutoff()));

                // Delete the loading more element before appending more.
                db.delete(Things.TABLE_NAME, Things.SELECTION_BY_SESSION_ID_AND_MORE,
                        Array.of(sessionId));

                InsertHelper insertHelper = new InsertHelper(db, Things.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                long t2 = System.currentTimeMillis();
                Log.d(TAG, "sync network: " + listing.networkTimeMs
                        + " parse: " + listing.parseTimeMs
                        + " db: " + (t2 - t1)
                        + " cleaned: " + cleaned);
            }
        } catch (IOException e) {
            Log.e(TAG, "sync", e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "sync", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "sync", e);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Things.TABLE_NAME, null, values);
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
        int count = db.update(Things.TABLE_NAME, values, selection, selectionArgs);
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
        int count = db.delete(Things.TABLE_NAME, selection, selectionArgs);
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
}
