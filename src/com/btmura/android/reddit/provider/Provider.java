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
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Saves;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class Provider {

    public static final String TAG = "Provider";

    private static final String TRUE = Boolean.toString(true);

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Things._ID,
            Things.COLUMN_EXPANDED,
            Things.COLUMN_NESTING,
    };

    private static final String SELECT_SAVES_BY_THING_ID = Saves.COLUMN_THING_ID + "=?";
    private static final String SELECT_VOTES_BY_THING_ID = Votes.COLUMN_THING_ID + "=?";

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
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                        .withSelection(BaseProvider.ID_SELECTION, Array.of(parentId))
                        .withValue(Things.COLUMN_NUM_COMMENTS, parentNumComments + 1)
                        .build());

                // Insert the placeholder comment.
                Uri uri = ThingProvider.THINGS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_COMMENT_REPLY, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(ThingProvider.PARAM_THING_ID, replyThingId)
                        .build();
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Things.COLUMN_ACCOUNT, accountName)
                        .withValue(Things.COLUMN_AUTHOR, accountName)
                        .withValue(Things.COLUMN_BODY, body)
                        .withValue(Things.COLUMN_KIND, Kinds.KIND_COMMENT)
                        .withValue(Things.COLUMN_NESTING, nesting)
                        .withValue(Things.COLUMN_SEQUENCE, sequence)
                        .withValue(Things.COLUMN_SESSION_ID, sessionId)
                        .withValue(Things.COLUMN_SESSION_TIMESTAMP, sessionTimestamp)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
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
                    Uri uri = ThingProvider.THINGS_URI.buildUpon()
                            .appendQueryParameter(ThingProvider.PARAM_COMMENT_DELETE, TRUE)
                            .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
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
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                        .withSelection(BaseProvider.ID_SELECTION, Array.of(headerId))
                        .withValue(Things.COLUMN_NUM_COMMENTS, headerNumComments - numDeletes)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void expandCommentAsync(Context context, final String sessionId, final long id) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentResolver cr = appContext.getContentResolver();
                Cursor c = cr.query(ThingProvider.THINGS_URI, EXPAND_PROJECTION,
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
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .withValue(Things.COLUMN_EXPANDED, true)
                            .build());
                    for (int i = 0; i < childCount; i++) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                                .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                                .withValue(Things.COLUMN_EXPANDED, true)
                                .withValue(Things.COLUMN_VISIBLE, true)
                                .build());
                    }

                    applyOps(appContext, ThingProvider.AUTHORITY, ops);

                } finally {
                    c.close();
                }
            }
        });
    }

    public static void collapseCommentAsync(Context context, final long id,
            final long[] childIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int childCount = childIds != null ? childIds.length : 0;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(childCount + 1);
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                        .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                        .withValue(Things.COLUMN_EXPANDED, false)
                        .build());
                for (int i = 0; i < childCount; i++) {
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                            .withValue(Things.COLUMN_EXPANDED, true)
                            .withValue(Things.COLUMN_VISIBLE, false)
                            .build());
                }

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void insertMessageReplyAsync(Context context,
            final String accountName,
            final String body,
            final String parentThingId,
            final long sessionId,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Uri uri = MessageProvider.MESSAGES_URI.buildUpon()
                        .appendPath(parentThingId)
                        .appendQueryParameter(MessageProvider.PARAM_THING_ID, thingId)
                        .appendQueryParameter(MessageProvider.PARAM_REPLY, TRUE)
                        .appendQueryParameter(MessageProvider.PARAM_SYNC, TRUE)
                        .build();

                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(1);
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Messages.COLUMN_ACCOUNT, accountName)
                        .withValue(Messages.COLUMN_AUTHOR, accountName)
                        .withValue(Messages.COLUMN_BODY, body)
                        .withValue(Messages.COLUMN_KIND, Kinds.KIND_MESSAGE)
                        .withValue(Messages.COLUMN_SESSION_ID, sessionId)
                        .withValue(Messages.COLUMN_WAS_COMMENT, false)
                        .build());

                applyOps(appContext, MessageProvider.AUTHORITY, ops);
            }
        });
    }

    public static void saveAsync(final Context context, final String accountName,
            final String thingId, final int action) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);
                ops.add(ContentProviderOperation.newDelete(ThingProvider.SAVES_URI)
                        .withSelection(SELECT_SAVES_BY_THING_ID, Array.of(thingId))
                        .build());

                Uri uri = ThingProvider.SAVES_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_OTHERS, TRUE)
                        .build();
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Saves.COLUMN_ACCOUNT, accountName)
                        .withValue(Saves.COLUMN_THING_ID, thingId)
                        .withValue(Saves.COLUMN_ACTION, action)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void voteAsync(final Context context, final String accountName,
            final String thingId, final int likes) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);
                ops.add(ContentProviderOperation.newDelete(ThingProvider.VOTES_URI)
                        .withSelection(SELECT_VOTES_BY_THING_ID, Array.of(thingId))
                        .build());

                Uri uri = ThingProvider.VOTES_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_OTHERS, TRUE)
                        .build();
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(Votes.COLUMN_ACCOUNT, accountName)
                        .withValue(Votes.COLUMN_THING_ID, thingId)
                        .withValue(Votes.COLUMN_VOTE, likes)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    static void applyOps(Context context, String authority,
            ArrayList<ContentProviderOperation> ops) {
        try {
            context.getContentResolver().applyBatch(authority, ops);
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
