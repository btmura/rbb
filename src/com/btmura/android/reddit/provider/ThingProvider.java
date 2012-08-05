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
import java.util.ArrayList;
import java.util.HashMap;

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
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountUtils;

public class ThingProvider extends BaseProvider {

    public static final String TAG = "ThingProvider";
    public static boolean DEBUG = Debug.DEBUG;

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_THINGS = 1;
    private static final int MATCH_ONE_THING = 2;
    static {
        MATCHER.addURI(AUTHORITY, Things.TABLE_NAME, MATCH_ALL_THINGS);
        MATCHER.addURI(AUTHORITY, Things.TABLE_NAME + "/#", MATCH_ONE_THING);
    }

    private static final String[] SYNC_PROJECTION = {
            Things._ID,
            Things.COLUMN_THING_ID,
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
        if (uri.getBooleanQueryParameter(Things.QUERY_SYNC, false)) {
            sync(uri, projection, selection, selectionArgs, sortOrder);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(Things.TABLE_NAME, projection, selection, selectionArgs,
                null, null, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor c = null;
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(Things.QUERY_ACCOUNT_NAME);
            String cookie = AccountUtils.getCookie(context, accountName);

            Listing listing;
            String subredditName = uri.getQueryParameter(Things.QUERY_SUBREDDIT_NAME);
            if (subredditName != null) {
                int filter = Integer.parseInt(uri.getQueryParameter(Things.QUERY_FILTER));
                String more = uri.getQueryParameter(Things.QUERY_MORE);
                listing = new ThingListing(context, cookie, subredditName, filter, more);
            } else {
                String thingId = uri.getQueryParameter(Things.QUERY_THING_ID);
                listing = new CommentListing(context, cookie, thingId);
            }

            listing.process();
            ArrayList<ContentValues> values = listing.getValues();
            HashMap<String, ContentValues> valueMap = listing.getValueMap();

            int count = values.size();
            if (count > 0) {
                SQLiteDatabase db = helper.getReadableDatabase();
                c = db.query(Things.TABLE_NAME, SYNC_PROJECTION, selection, selectionArgs,
                        null, null, null);
                while (c.moveToNext()) {
                    String name = c.getString(SYNC_INDEX_NAME);
                    ContentValues v = valueMap.get(name);
                    if (v != null) {
                        int likes = c.getInt(SYNC_INDEX_LIKES);
                        v.put(Things.COLUMN_LIKES, likes);
                    }
                }

                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count + 1);
                ops.add(ContentProviderOperation
                        .newDelete(Things.CONTENT_URI)
                        .withSelection(Things.PARENT_SELECTION, new String[] {listing.getParent()})
                        .build());
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newInsert(Things.CONTENT_URI)
                            .withValues(values.get(i))
                            .build());
                }
                applyBatch(ops);
            }
        } catch (OperationApplicationException e) {
            Log.e(TAG, "sync", e);
        } catch (IOException e) {
            Log.e(TAG, "sync", e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "sync", e);
        } catch (AuthenticatorException e) {
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
}
