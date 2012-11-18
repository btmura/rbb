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
import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class CommentProvider extends SessionProvider {

    public static final String TAG = "CommentProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.comments";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_COMMENTS = "comments";
    static final String PATH_ACTIONS = "actions";
    public static final Uri COMMENTS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_COMMENTS);
    public static final Uri ACTIONS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACTIONS);

    public static final String PARAM_FETCH = "fetch";
    public static final String PARAM_REPLY = "reply";
    public static final String PARAM_DELETE = "delete";
    public static final String PARAM_SYNC = "sync";

    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_PARENT_THING_ID = "parentThingId";
    public static final String PARAM_THING_ID = "thingId";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_COMMENTS = 1;
    private static final int MATCH_ACTIONS = 2;
    static {
        MATCHER.addURI(AUTHORITY, PATH_COMMENTS, MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY, PATH_ACTIONS, MATCH_ACTIONS);
    }

    private static final String COMMENTS_WITH_VOTES = Comments.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Comments.COLUMN_THING_ID + ")";

    public CommentProvider() {
        super(TAG);
    }

    protected String getTable(Uri uri, boolean isQuery) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_COMMENTS:
                return isQuery ? COMMENTS_WITH_VOTES : Comments.TABLE_NAME;

            case MATCH_ACTIONS:
                return CommentActions.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values) {
        if (uri.getBooleanQueryParameter(PARAM_FETCH, false)) {
            handleFetch(uri, db);
        } else if (uri.getBooleanQueryParameter(PARAM_REPLY, false)) {
            handleReply(uri, db, values);
        } else if (uri.getBooleanQueryParameter(PARAM_DELETE, false)) {
            handleDelete(uri, db);
        }
    }

    private void handleFetch(Uri uri, SQLiteDatabase db) {
        try {
            long sessionTimestamp = getSessionTimestamp();

            String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String thingId = uri.getQueryParameter(PARAM_THING_ID);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            CommentListing listing = CommentListing.get(context, helper, accountName, sessionId,
                    sessionTimestamp, thingId, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            db.beginTransaction();
            try {
                // Delete old comments that can't possibly be viewed anymore.
                cleaned = db.delete(Comments.TABLE_NAME, Comments.SELECT_BEFORE_TIMESTAMP,
                        Array.of(sessionTimestamp));

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

    private void handleReply(Uri uri, SQLiteDatabase db, ContentValues values) {
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(5);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT);
        v.put(CommentActions.COLUMN_ACCOUNT, values.getAsString(Comments.COLUMN_ACCOUNT));
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        v.put(CommentActions.COLUMN_TEXT, values.getAsString(Comments.COLUMN_BODY));
        db.insert(CommentActions.TABLE_NAME, null, v);
    }

    private void handleDelete(Uri uri, SQLiteDatabase db) {
        String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
        String parentThingId = uri.getQueryParameter(PARAM_PARENT_THING_ID);
        String thingId = uri.getQueryParameter(PARAM_THING_ID);

        ContentValues v = new ContentValues(4);
        v.put(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE);
        v.put(CommentActions.COLUMN_ACCOUNT, accountName);
        v.put(CommentActions.COLUMN_PARENT_THING_ID, parentThingId);
        v.put(CommentActions.COLUMN_THING_ID, thingId);
        db.insert(CommentActions.TABLE_NAME, null, v);
    }

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void insertInBackground(Context context,
            final String accountName,
            final long headerId,
            final int headerNumComments,
            final String body,
            final int nesting,
            final String parentThingId,
            final int sequence,
            final String sessionId,
            final long sessionCreationTime,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);

                // Increment the header's number of comments.
                ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                        .withSelection(ID_SELECTION, Array.of(headerId))
                        .withValue(Comments.COLUMN_NUM_COMMENTS, headerNumComments + 1)
                        .build());

                // Insert the placeholder comment.
                Uri uri = COMMENTS_URI.buildUpon()
                        .appendQueryParameter(PARAM_REPLY, Boolean.toString(true))
                        .appendQueryParameter(PARAM_SYNC, Boolean.toString(true))
                        .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(PARAM_THING_ID, thingId)
                        .build();
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Comments.COLUMN_ACCOUNT, accountName)
                        .withValue(Comments.COLUMN_AUTHOR, accountName)
                        .withValue(Comments.COLUMN_BODY, body)
                        .withValue(Comments.COLUMN_KIND, Comments.KIND_COMMENT)
                        .withValue(Comments.COLUMN_NESTING, nesting)
                        .withValue(Comments.COLUMN_SEQUENCE, sequence)
                        .withValue(Comments.COLUMN_SESSION_ID, sessionId)
                        .withValue(Comments.COLUMN_SESSION_TIMESTAMP, sessionCreationTime)
                        .build());

                try {
                    appContext.getContentResolver().applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    public static void deleteInBackground(Context context,
            final String accountName,
            final long headerId,
            final int headerNumComments,
            final String parentThingId,
            final long[] ids,
            final String[] thingIds,
            final boolean[] hasChildren) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int count = ids.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count + 1);

                int numDeletes = 0;
                for (int i = 0; i < count; i++) {
                    Uri uri = COMMENTS_URI.buildUpon()
                            .appendQueryParameter(PARAM_DELETE, Boolean.toString(true))
                            .appendQueryParameter(PARAM_SYNC, Boolean.toString(true))
                            .appendQueryParameter(PARAM_ACCOUNT, accountName)
                            .appendQueryParameter(PARAM_PARENT_THING_ID, parentThingId)
                            .appendQueryParameter(PARAM_THING_ID, thingIds[i])
                            .build();
                    if (hasChildren[i]) {
                        ops.add(ContentProviderOperation.newUpdate(uri)
                                .withValue(Comments.COLUMN_AUTHOR, Comments.DELETED)
                                .withValue(Comments.COLUMN_BODY, Comments.DELETED)
                                .withSelection(ID_SELECTION, Array.of(ids[i]))
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(uri)
                                .withSelection(ID_SELECTION, Array.of(ids[i]))
                                .build());
                        numDeletes++;
                    }
                }

                // Update the header comment by how comments were truly deleted.
                ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                        .withSelection(ID_SELECTION, Array.of(headerId))
                        .withValue(Comments.COLUMN_NUM_COMMENTS, headerNumComments - numDeletes)
                        .build());

                try {
                    appContext.getContentResolver().applyBatch(AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_EXPANDED,
            Comments.COLUMN_NESTING,
    };

    public static void expandInBackground(Context context, final String sessionId, final long id) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Cursor c = cr.query(COMMENTS_URI, EXPAND_PROJECTION, Comments.SELECT_BY_SESSION_ID,
                        Array.of(sessionId), Comments.SORT_BY_SEQUENCE_AND_ID);
                try {
                    long[] childIds = null;
                    CursorCommentList cl = new CursorCommentList(c, 0, 2, -1);
                    int count = cl.getCommentCount();
                    for (int i = 0; i < count; i++) {
                        if (cl.getCommentId(i) == id) {
                            childIds = CommentLogic.getChildren(cl, i);
                            break;
                        }
                    }

                    int childCount = childIds != null ? childIds.length : 0;
                    ArrayList<ContentProviderOperation> ops =
                            new ArrayList<ContentProviderOperation>(childCount + 1);
                    ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                            .withSelection(ID_SELECTION, Array.of(id))
                            .withValue(Comments.COLUMN_EXPANDED, true)
                            .build());
                    for (int i = 0; i < childCount; i++) {
                        ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                                .withSelection(ID_SELECTION, Array.of(childIds[i]))
                                .withValue(Comments.COLUMN_EXPANDED, true)
                                .withValue(Comments.COLUMN_VISIBLE, true)
                                .build());
                    }

                    try {
                        cr.applyBatch(AUTHORITY, ops);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                } finally {
                    c.close();
                }
            }
        });
    }

    public static void collapseInBackground(Context context, final long id,
            final long[] childIds) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int childCount = childIds != null ? childIds.length : 0;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(childCount + 1);
                ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                        .withSelection(ID_SELECTION, Array.of(id))
                        .withValue(Comments.COLUMN_EXPANDED, false)
                        .build());
                for (int i = 0; i < childCount; i++) {
                    ops.add(ContentProviderOperation.newUpdate(COMMENTS_URI)
                            .withSelection(ID_SELECTION, Array.of(childIds[i]))
                            .withValue(Comments.COLUMN_EXPANDED, true)
                            .withValue(Comments.COLUMN_VISIBLE, false)
                            .build());
                }

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
}
