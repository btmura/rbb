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

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.util.Array;

public class Provider {

    public static final String TAG = "Provider";

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Things._ID,
            Things.COLUMN_EXPANDED,
            Things.COLUMN_NESTING,
    };

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void insertCommentAsync(Context context,
            final long parentId,
            final int parentNumComments,
            final String parentThingId,
            final String replyThingId,
            final String accountName,
            final String body,
            final int nesting,
            final int sequence,
            final String sessionId,
            final long sessionTimestamp) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);

                // Increment the header's number of comments.
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                        .withSelection(BaseProvider.ID_SELECTION, Array.of(parentId))
                        .withValue(Things.COLUMN_NUM_COMMENTS, parentNumComments + 1)
                        .build());

                // Insert the placeholder comment.
                Uri uri = ThingProvider.COMMENTS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_REPLY, Boolean.toString(true))
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, Boolean.toString(true))
                        .appendQueryParameter(ThingProvider.PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(ThingProvider.PARAM_THING_ID, replyThingId)
                        .build();
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Things.COLUMN_ACCOUNT, accountName)
                        .withValue(Things.COLUMN_AUTHOR, accountName)
                        .withValue(Things.COLUMN_BODY, body)
                        .withValue(Things.COLUMN_KIND, Things.KIND_COMMENT)
                        .withValue(Things.COLUMN_NESTING, nesting)
                        .withValue(Things.COLUMN_SEQUENCE, sequence)
                        .withValue(Things.COLUMN_SESSION_ID, sessionId)
                        .withValue(Things.COLUMN_SESSION_TIMESTAMP, sessionTimestamp)
                        .build());

                try {
                    appContext.getContentResolver().applyBatch(ThingProvider.AUTHORITY, ops);
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

    public static void deleteCommentAsync(Context context,
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
                    Uri uri = ThingProvider.COMMENTS_URI.buildUpon()
                            .appendQueryParameter(ThingProvider.PARAM_DELETE,
                                    Boolean.toString(true))
                            .appendQueryParameter(ThingProvider.PARAM_SYNC, Boolean.toString(true))
                            .appendQueryParameter(ThingProvider.PARAM_ACCOUNT, accountName)
                            .appendQueryParameter(ThingProvider.PARAM_PARENT_THING_ID,
                                    parentThingId)
                            .appendQueryParameter(ThingProvider.PARAM_THING_ID, thingIds[i])
                            .build();
                    if (hasChildren[i]) {
                        ops.add(ContentProviderOperation.newUpdate(uri)
                                .withValue(Things.COLUMN_AUTHOR, Things.DELETED)
                                .withValue(Things.COLUMN_BODY, Things.DELETED)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(uri)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                        numDeletes++;
                    }
                }

                // Update the header comment by how comments were truly deleted.
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                        .withSelection(BaseProvider.ID_SELECTION, Array.of(headerId))
                        .withValue(Things.COLUMN_NUM_COMMENTS, headerNumComments - numDeletes)
                        .build());

                try {
                    appContext.getContentResolver().applyBatch(ThingProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }

    public static void expandCommentAsync(Context context, final String sessionId, final long id) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Cursor c = cr.query(ThingProvider.COMMENTS_URI, EXPAND_PROJECTION,
                        Things.SELECT_BY_SESSION_ID, Array.of(sessionId),
                        Things.SORT_BY_SEQUENCE_AND_ID);
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
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .withValue(Things.COLUMN_EXPANDED, true)
                            .build());
                    for (int i = 0; i < childCount; i++) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                                .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                                .withValue(Things.COLUMN_EXPANDED, true)
                                .withValue(Things.COLUMN_VISIBLE, true)
                                .build());
                    }

                    try {
                        cr.applyBatch(ThingProvider.AUTHORITY, ops);
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

    public static void collapseCommentAsync(Context context, final long id,
            final long[] childIds) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int childCount = childIds != null ? childIds.length : 0;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(childCount + 1);
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                        .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                        .withValue(Things.COLUMN_EXPANDED, false)
                        .build());
                for (int i = 0; i < childCount; i++) {
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                            .withValue(Things.COLUMN_EXPANDED, true)
                            .withValue(Things.COLUMN_VISIBLE, false)
                            .build());
                }

                try {
                    cr.applyBatch(ThingProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });
    }
}
