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
import java.net.URL;
import java.util.ArrayList;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.data.Urls;

public class ThingProvider extends BaseProvider {

    public static final String TAG = "ThingProvider";
    public static boolean DEBUG = Debug.DEBUG;

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";

    private static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDIT_THINGS = 1;
    static {
        MATCHER.addURI(AUTHORITY, Things.TABLE_NAME + "/*", MATCH_SUBREDDIT_THINGS);
    }

    public static class Things implements BaseColumns, SyncColumns {
        static final String TABLE_NAME = "things";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI);

        public static final String QUERY_PARAM_SYNC = "sync";
        public static final String QUERY_PARAM_ACCOUNT = "account";
        public static final String QUERY_PARAM_SUBREDDIT = "subreddit";
        public static final String QUERY_PARAM_FILTER = "filter";
        public static final String QUERY_PARAM_MORE = "more";

        public static final int NUM_COLUMNS = 16;
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_CREATED_UTC = "createdUtc";
        public static final String COLUMN_DOMAIN = "domain";
        public static final String COLUMN_DOWNS = "downs";
        public static final String COLUMN_LIKES = "likes";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_NUM_COMMENTS = "numComments";
        public static final String COLUMN_OVER_18 = "over18";
        public static final String COLUMN_PERMA_LINK = "permaLink";
        public static final String COLUMN_SCORE = "score";
        public static final String COLUMN_SELF = "self";
        public static final String COLUMN_SUBREDDIT = "subreddit";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_THUMBNAIL = "thumbnail";
        public static final String COLUMN_UPS = "ups";
        public static final String COLUMN_URL = "url";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (DEBUG) {
            Log.d(TAG, "query uri: " + uri);
        }
        boolean sync = uri.getBooleanQueryParameter(Things.QUERY_PARAM_SYNC, false);
        if (sync) {
            try {
                syncThings(uri);
            } catch (OperationCanceledException e) {
                Log.e(TAG, "query", e);
            } catch (AuthenticatorException e) {
                Log.e(TAG, "query", e);
            } catch (IOException e) {
                Log.e(TAG, "query", e);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "query", e);
            }
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(Things.TABLE_NAME, projection, selection, selectionArgs, null, null,
                null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void syncThings(Uri uri) throws OperationCanceledException, AuthenticatorException,
            IOException, OperationApplicationException {
        Context context = getContext();
        String accountName = uri.getQueryParameter(Things.QUERY_PARAM_ACCOUNT);
        String cookie = AccountUtils.getCookie(context, accountName);

        String subreddit = uri.getQueryParameter(Things.QUERY_PARAM_SUBREDDIT);
        int filter = Integer.parseInt(uri.getQueryParameter(Things.QUERY_PARAM_FILTER));
        String more = uri.getQueryParameter(Things.QUERY_PARAM_MORE);
        URL url = Urls.subredditUrl(subreddit, filter, more);

        ArrayList<ContentValues> values = NetApi.queryThings(context, url, cookie);
        int count = values.size();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(count + 1);
        ops.add(ContentProviderOperation.newDelete(Things.CONTENT_URI).build());
        for (int i = 0; i < count; i++) {
            ops.add(ContentProviderOperation.newInsert(Things.CONTENT_URI)
                    .withValues(values.get(i))
                    .build());
        }

        applyBatch(ops);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (DEBUG) {
            Log.d(TAG, "insert");
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Things.TABLE_NAME, null, values);
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Things.TABLE_NAME, selection, selectionArgs);
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
