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
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.SubredditSearches;
import com.btmura.android.reddit.util.Array;

public class SubredditSearchProvider extends SessionProvider {

    public static final String TAG = "SubredditSearchProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subredditsearches";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/");

    // Query parameters related to fetching search results before querying.
    public static final String SYNC_ENABLE = "sync";
    public static final String SYNC_ACCOUNT = "accountName";
    public static final String SYNC_SESSION_ID = "sessionId";
    public static final String SYNC_QUERY = "query";

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query: " + uri.getQuery());
        }

        if (uri.getBooleanQueryParameter(SYNC_ENABLE, false)) {
            sync(uri);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(SubredditSearches.TABLE_NAME, projection, selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri) {
        try {
            String accountName = uri.getQueryParameter(SYNC_ACCOUNT);
            String sessionId = uri.getQueryParameter(SYNC_SESSION_ID);
            String query = uri.getQueryParameter(SYNC_QUERY);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            SubredditSearchListing listing = SubredditSearchListing.get(context, accountName,
                    sessionId, query, cookie);

            long cleaned;
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old results that can't be possibly viewed anymore.
                cleaned = db.delete(SubredditSearches.TABLE_NAME,
                        SubredditSearches.SELECTION_BEFORE_TIMESTAMP,
                        Array.of(getCreationTimeCutoff()));
                InsertHelper insertHelper = new InsertHelper(db, SubredditSearches.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "cleaned: " + cleaned);
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, "sync", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "sync", e);
        } catch (IOException e) {
            Log.e(TAG, "sync", e);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(SubredditSearches.TABLE_NAME, null, values);
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
        int count = db.update(SubredditSearches.TABLE_NAME, values, selection, selectionArgs);
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
        int count = db.delete(SubredditSearches.TABLE_NAME, selection, selectionArgs);
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
