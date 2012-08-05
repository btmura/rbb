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
import com.btmura.android.reddit.util.ArrayUtils;

public class CommentProvider extends BaseProvider {

    public static final String TAG = "CommentProvider";
    public static final boolean DEBUG = Debug.DEBUG;

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.comments";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_COMMENTS = 1;
    static {
        MATCHER.addURI(AUTHORITY, Comments.TABLE_NAME, MATCH_ALL_COMMENTS);
    }

    private static final String[] SYNC_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_ID,
            Comments.COLUMN_LIKES,
    };
    private static final int SYNC_INDEX_ID = 1;
    private static final int SYNC_INDEX_LIKES = 2;

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (DEBUG) {
            Log.d(TAG, "query uri: " + uri);
        }

        if (uri.getBooleanQueryParameter(Comments.PARAM_SYNC, false)) {
            sync(uri);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(Comments.TABLE_NAME, projection, selection, selectionArgs,
                null, null, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void sync(Uri uri) {
        Cursor c = null;
        try {
            Context context = getContext();
            String accountName = uri.getQueryParameter(Comments.PARAM_ACCOUNT_NAME);
            String thingId = uri.getQueryParameter(Comments.PARAM_THING_ID);

            String cookie = AccountUtils.getCookie(context, accountName);
            CommentListing listing = new CommentListing(context, cookie, thingId);
            listing.process();

            ArrayList<ContentValues> values = listing.getValues();
            HashMap<String, ContentValues> valueMap = listing.getValueMap();
            int count = values.size();
            if (count > 0) {
                SQLiteDatabase db = helper.getReadableDatabase();
                String[] selectionArgs = ArrayUtils.toArray(thingId);
                c = db.query(Comments.TABLE_NAME, SYNC_PROJECTION,
                        Comments.PARENT_ID_SELECTION, selectionArgs,
                        null, null, null);
                while (c.moveToNext()) {
                    String id = c.getString(SYNC_INDEX_ID);
                    ContentValues v = valueMap.get(id);
                    if (values != null) {
                        int likes = c.getInt(SYNC_INDEX_LIKES);
                        v.put(Comments.COLUMN_LIKES, likes);
                    }
                }

                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count + 1);
                ops.add(ContentProviderOperation.newDelete(Comments.CONTENT_URI)
                        .withSelection(Comments.PARENT_ID_SELECTION, selectionArgs)
                        .build());
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newInsert(Comments.CONTENT_URI)
                            .withValues(values.get(i))
                            .build());
                }
                applyBatch(ops);
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, "sync", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "sync", e);
        } catch (IOException e) {
            Log.e(TAG, "sync", e);
        } catch (OperationApplicationException e) {
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
