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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
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
        public static final String COLUMN_THUMBNAIL_URL = "thumbnailUrl";
        public static final String COLUMN_UPS = "ups";
        public static final String COLUMN_URL = "url";
    }

    private static final String[] SYNC_PROJECTION = {
            Things._ID,
            Things.COLUMN_NAME,
            Things.COLUMN_LIKES,
    };
    private static final int SYNC_INDEX_NAME = 1;
    private static final int SYNC_INDEX_LIKES = 2;

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (DEBUG) {
            Log.d(TAG, "query uri: " + uri);
        }
        if (uri.getBooleanQueryParameter(Things.QUERY_PARAM_SYNC, false)) {
            syncThings(uri, projection, selection, selectionArgs, sortOrder);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(Things.TABLE_NAME, projection, selection, selectionArgs,
                null, null, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void syncThings(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor c = null;
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(Things.QUERY_PARAM_ACCOUNT);
            String cookie = AccountUtils.getCookie(context, accountName);

            String subreddit = uri.getQueryParameter(Things.QUERY_PARAM_SUBREDDIT);
            int filter = Integer.parseInt(uri.getQueryParameter(Things.QUERY_PARAM_FILTER));
            String more = uri.getQueryParameter(Things.QUERY_PARAM_MORE);
            URL url = Urls.subredditUrl(subreddit, filter, more);

            ThingListing listing = NetApi.queryThings(context, url, cookie);
            int count = listing.values.size();
            if (count > 0) {
                SQLiteDatabase db = helper.getReadableDatabase();
                c = db.query(Things.TABLE_NAME, SYNC_PROJECTION, selection, selectionArgs,
                        null, null, null);
                while (c.moveToNext()) {
                    String name = c.getString(SYNC_INDEX_NAME);
                    ContentValues v = listing.valueMap.get(name);
                    if (v != null) {
                        int likes = c.getInt(SYNC_INDEX_LIKES);
                        v.put(Things.COLUMN_LIKES, likes);
                    }
                }

                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count + 1);
                ops.add(ContentProviderOperation.newDelete(Things.CONTENT_URI).build());
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newInsert(Things.CONTENT_URI)
                            .withValues(listing.values.get(i))
                            .build());
                }
                applyBatch(ops);
            }
        } catch (OperationApplicationException e) {
            Log.e(TAG, "syncThings", e);
        } catch (IOException e) {
            Log.e(TAG, "syncThings", e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "syncThings", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "syncThings", e);
        } finally {
            c.close();
        }
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
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Things.TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
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

    public static void updateLikesInBackground(final Context context, final long id, final int likes) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentValues values = new ContentValues(1);
                values.put(Things.COLUMN_LIKES, likes);

                ContentResolver cr = context.getContentResolver();
                cr.update(Things.CONTENT_URI, values, ID_SELECTION, idSelectionArg(id));
                return null;
            }
        }.execute();
    }
}
