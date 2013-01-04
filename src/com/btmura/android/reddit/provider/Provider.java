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

import android.app.backup.BackupManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;

/**
 * Provider is a collection of static methods that do user actions which
 * correspond to multiple content provider operations.
 */
public class Provider {

    public static final String TAG = "Provider";

    private static final String TRUE = Boolean.toString(true);

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Things._ID,
            Things.COLUMN_EXPANDED,
            Things.COLUMN_NESTING,
    };

    public static void insertInBackground(Context context, String accountName,
            String... subreddits) {
        modifyInBackground(context, accountName, subreddits, true);
    }

    public static void deleteInBackground(Context context, String accountName,
            String... subreddits) {
        modifyInBackground(context, accountName, subreddits, false);
    }

    private static void modifyInBackground(Context context, final String accountName,
            final String[] subreddits, final boolean add) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Only trigger a sync on real accounts.
                Uri syncUri = SubredditProvider.SUBREDDITS_URI;
                if (AccountUtils.isAccount(accountName)) {
                    syncUri = syncUri.buildUpon()
                            .appendQueryParameter(SubredditProvider.PARAM_SYNC, TRUE)
                            .build();
                }

                int count = subreddits.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count * 2);
                int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newDelete(syncUri)
                            .withSelection(Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                                    Array.of(accountName, subreddits[i]))
                            .build());

                    // Don't insert deletion rows for app storage account,
                    // since they don't need to be synced back.
                    if (AccountUtils.isAccount(accountName) || add) {
                        ops.add(ContentProviderOperation.newInsert(syncUri)
                                .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                                .withValue(Subreddits.COLUMN_NAME, subreddits[i])
                                .withValue(Subreddits.COLUMN_STATE, state)
                                .build());
                    }
                }

                try {
                    appContext.getContentResolver().applyBatch(SubredditProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void intoTheVoid) {
                showChangeToast(appContext, add, subreddits.length);
                scheduleBackup(appContext, accountName);
            }
        }.execute();
    }

    private static void showChangeToast(Context context, boolean added, int count) {
        int resId = added ? R.plurals.subreddits_added : R.plurals.subreddits_deleted;
        CharSequence text = context.getResources().getQuantityString(resId, count, count);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private static void scheduleBackup(Context context, String accountName) {
        if (!AccountUtils.isAccount(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void commentReplyAsync(Context context,
            final long parentId,
            final int parentNumComments,
            final String parentThingId,
            final String replyThingId,
            final String accountName,
            final String body,
            final int nesting,
            final int sequence,
            final long sessionId) {
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

    public static void expandCommentAsync(Context context, final long sessionId, final long id) {
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

    public static void clearNewMessageIndicator(Context context, final String accountName,
            final boolean hasMail) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                // Update the existing row. If there isn't such a row, it will
                // be created upon sync, where it will have the proper value.
                ContentValues values = new ContentValues(1);
                values.put(Accounts.COLUMN_HAS_MAIL, false);
                cr.update(AccountProvider.ACCOUNTS_NOTIFY_URI, values,
                        Accounts.SELECT_BY_ACCOUNT, Array.of(accountName));
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
                Uri uri = ThingProvider.MESSAGES_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_MESSAGE_REPLY, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_PARENT_THING_ID, parentThingId)
                        .appendQueryParameter(ThingProvider.PARAM_THING_ID, thingId)
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
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

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    /** Mark a message either read or unread. */
    public static void readMessageAsync(final Context context, final String accountName,
            final String thingId, final boolean read) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);

                // Delete prior read and unread states of this thing since the
                // last action will be the final state anyway.
                ops.add(ContentProviderOperation.newDelete(ThingProvider.MESSAGE_ACTIONS_URI)
                        .withSelection(MessageActions.SELECT_READ_UNREAD_BY_ACCOUNT_AND_THING_ID,
                                Array.of(accountName, thingId))
                        .build());

                // Insert the latest action we want for this message.
                Uri uri = ThingProvider.MESSAGE_ACTIONS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_MESSAGES, TRUE)
                        .build();
                int action = read ? MessageActions.ACTION_READ : MessageActions.ACTION_UNREAD;
                ops.add(ContentProviderOperation.newInsert(uri)
                        .withValue(MessageActions.COLUMN_ACCOUNT, accountName)
                        .withValue(MessageActions.COLUMN_THING_ID, thingId)
                        .withValue(MessageActions.COLUMN_ACTION, action)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void saveAsync(final Context context,
            final String accountName,
            final String thingId,

            // Following parameters are for faking a thing.
            final String author,
            final long createdUtc,
            final String domain,
            final int downs,
            final int likes,
            final int numComments,
            final boolean over18,
            final String permaLink,
            final int score,
            final String subreddit,
            final String title,
            final String thumbnailUrl,
            final int ups,
            final String url) {

        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voidRay) {
                Uri uri = ThingProvider.SAVE_ACTIONS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, TRUE)
                        .build();

                ContentValues v = new ContentValues(17);
                v.put(SaveActions.COLUMN_ACCOUNT, accountName);
                v.put(SaveActions.COLUMN_THING_ID, thingId);
                v.put(SaveActions.COLUMN_ACTION, SaveActions.ACTION_SAVE);

                // Following values are for faking a thing.
                v.put(SaveActions.COLUMN_AUTHOR, author);
                v.put(SaveActions.COLUMN_CREATED_UTC, createdUtc);
                v.put(SaveActions.COLUMN_DOMAIN, domain);
                v.put(SaveActions.COLUMN_DOWNS, downs);
                v.put(SaveActions.COLUMN_LIKES, likes);
                v.put(SaveActions.COLUMN_NUM_COMMENTS, numComments);
                v.put(SaveActions.COLUMN_OVER_18, over18);
                v.put(SaveActions.COLUMN_PERMA_LINK, permaLink);
                v.put(SaveActions.COLUMN_SCORE, score);
                v.put(SaveActions.COLUMN_SUBREDDIT, subreddit);
                v.put(SaveActions.COLUMN_TITLE, title);
                v.put(SaveActions.COLUMN_THUMBNAIL_URL, thumbnailUrl);
                v.put(SaveActions.COLUMN_UPS, ups);
                v.put(SaveActions.COLUMN_URL, url);

                ContentResolver cr = appContext.getContentResolver();
                return cr.insert(uri, v) != null;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                String msg;
                if (success) {
                    msg = appContext.getResources().getQuantityString(R.plurals.things_saved, 1, 1);
                } else {
                    msg = appContext.getString(R.string.error);
                }
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public static void unsaveAsync(final Context context, final String accountName,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... drVoidberg) {
                Uri uri = ThingProvider.SAVE_ACTIONS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, TRUE)
                        .build();

                ContentValues v = new ContentValues(3);
                v.put(SaveActions.COLUMN_ACCOUNT, accountName);
                v.put(SaveActions.COLUMN_THING_ID, thingId);
                v.put(SaveActions.COLUMN_ACTION, SaveActions.ACTION_UNSAVE);

                ContentResolver cr = appContext.getContentResolver();
                return cr.insert(uri, v) != null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                String msg;
                if (result) {
                    msg = appContext.getResources()
                            .getQuantityString(R.plurals.things_unsaved, 1, 1);
                } else {
                    msg = appContext.getString(R.string.error);
                }
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public static void voteAsync(final Context context, final String accountName,
            final String thingId, final int likes) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                Uri uri = ThingProvider.VOTE_ACTIONS_URI.buildUpon()
                        .appendQueryParameter(ThingProvider.PARAM_SYNC, TRUE)
                        .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, TRUE)
                        .build();

                ContentValues v = new ContentValues(3);
                v.put(VoteActions.COLUMN_ACCOUNT, accountName);
                v.put(VoteActions.COLUMN_THING_ID, thingId);
                v.put(VoteActions.COLUMN_ACTION, likes);

                // No toast needed, since the vote arrows will reflect success
                // or not.
                cr.insert(uri, v);
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
