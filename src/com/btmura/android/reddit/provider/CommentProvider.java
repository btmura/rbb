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
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class CommentProvider extends SessionProvider {

    public static final String TAG = "CommentProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.comments";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_COMMENTS = "comments";
    static final String PATH_COMMENT_ACTIONS = "actions";
    public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_COMMENTS);
    public static final Uri ACTIONS_CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI
            + PATH_COMMENT_ACTIONS);

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_REPLY = "reply";
    public static final String PARAM_DELETE = "delete";

    public static final String PARAM_ACCOUNT_NAME = "accountName";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_PARENT_THING_ID = "parentThingId";
    public static final String PARAM_THING_ID = "thingId";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_COMMENTS = 1;
    private static final int MATCH_COMMENT_ACTIONS = 2;
    static {
        MATCHER.addURI(AUTHORITY, PATH_COMMENTS, MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY, PATH_COMMENT_ACTIONS, MATCH_COMMENT_ACTIONS);
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
        processQueryUri(uri);
        String tableName = getTableName(uri, true);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query tableName: " + tableName);
        }

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private String getTableName(Uri uri, boolean joinVotes) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_COMMENTS:
                return joinVotes ? COMMENTS_WITH_VOTES : Comments.TABLE_NAME;

            case MATCH_COMMENT_ACTIONS:
                return CommentActions.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;
        boolean syncToNetwork = false;
        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            id = db.insert(tableName, null, values);
            syncToNetwork = processInsertUri(uri, db, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "insert tableName: " + tableName + " id: " + id
                    + " syncToNetwork: " + syncToNetwork);
        }
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        int count = db.update(tableName, values, selection, selectionArgs);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "update tableName: " + tableName + " count: " + count);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        boolean syncToNetwork = false;
        String tableName = getTableName(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = db.delete(tableName, selection, selectionArgs);
            syncToNetwork = processDeleteUri(uri, db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete tableName: " + tableName + " count: " + count
                    + " syncToNetwork: " + syncToNetwork);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null, syncToNetwork);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void insertPlaceholderInBackground(Context context, final String accountName,
            final String body, final int nesting, final String parentThingId, final int sequence,
            final String sessionId, final long sessionCreationTime, final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Uri uri = CONTENT_URI.buildUpon()
                        .appendQueryParameter(PARAM_REPLY, Boolean.toString(true))
                        .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(PARAM_THING_ID, thingId)
                        .build();

                ContentValues v = new ContentValues(8);
                v.put(Comments.COLUMN_ACCOUNT, accountName);
                v.put(Comments.COLUMN_AUTHOR, accountName);
                v.put(Comments.COLUMN_BODY, body);
                v.put(Comments.COLUMN_KIND, Comments.KIND_COMMENT);
                v.put(Comments.COLUMN_NESTING, nesting);
                v.put(Comments.COLUMN_SEQUENCE, sequence);
                v.put(Comments.COLUMN_SESSION_ID, sessionId);
                v.put(Comments.COLUMN_SESSION_TIMESTAMP, sessionCreationTime);

                ContentResolver cr = appContext.getContentResolver();
                cr.insert(uri, v);
            }
        });
    }

    public static void deleteInBackground(Context context, final String accountName,
            final String parentThingId, final long[] ids, final String[] thingIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                int count = ids.length;
                for (int i = 0; i < count; i++) {
                    Uri uri = CONTENT_URI.buildUpon()
                            .appendQueryParameter(PARAM_DELETE, Boolean.toString(true))
                            .appendQueryParameter(PARAM_ACCOUNT_NAME, accountName)
                            .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                            .appendQueryParameter(PARAM_THING_ID, thingIds[i])
                            .build();
                    ops.add(ContentProviderOperation.newDelete(uri)
                            .withSelection(ID_SELECTION, Array.of(ids[i]))
                            .build());
                }

                ContentResolver cr = appContext.getContentResolver();
                try {
                    cr.applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    /** Sync comments for a thing if specified in the uri. */
    private void processQueryUri(Uri uri) {
        if (!uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            return;
        }
        try {
            // Determine the cutoff first to avoid deleting synced data.
            long timestampCutoff = getSessionTimestampCutoff();
            long sessionTimestamp = System.currentTimeMillis();

            String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            CommentListing listing = CommentListing.get(context, helper, accountName, sessionId,
                    sessionTimestamp, thingId, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old comments that can't possibly be viewed anymore.
                cleaned = db.delete(Comments.TABLE_NAME, Comments.SELECTION_BEFORE_TIMESTAMP,
                        Array.of(timestampCutoff));

                InsertHelper insertHelper = new InsertHelper(db, Comments.TABLE_NAME);
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
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /** Schedule insertion of a comment if the URI specifies it. */
    private boolean processInsertUri(Uri uri, SQLiteDatabase db, ContentValues values) {
        if (!uri.getBooleanQueryParameter(PARAM_REPLY, false)) {
            return false;
        }

        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(5);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT);
        v.put(CommentActions.COLUMN_ACCOUNT, values.getAsString(Comments.COLUMN_ACCOUNT));
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        v.put(CommentActions.COLUMN_TEXT, values.getAsString(Comments.COLUMN_BODY));
        db.insert(CommentActions.TABLE_NAME, null, values);
        return true;
    }

    /** Schedule deletion of a comment if the URI specifies it. */
    private boolean processDeleteUri(Uri uri, SQLiteDatabase db) {
        if (!uri.getBooleanQueryParameter(PARAM_DELETE, false)) {
            return false;
        }

        String accountName = uri.getQueryParameter(PARAM_ACCOUNT_NAME);
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(4);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE);
        v.put(CommentActions.COLUMN_ACCOUNT, accountName);
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        db.insert(CommentActions.TABLE_NAME, null, v);
        return true;
    }
}
