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
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

/**
 * Provider is a collection of static methods that do user actions which
 * correspond to multiple content provider operations.
 */
public class Provider {

    public static final String TAG = "Provider";

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Things._ID,
            Things.COLUMN_EXPANDED,
            Things.COLUMN_NESTING,
    };

    public static void addSubredditAsync(Context context, String accountName,
            String... subreddits) {
        changeSubredditAsync(context, accountName, subreddits, true);
    }

    public static void removeSubredditAsync(Context context, String accountName,
            String... subreddits) {
        changeSubredditAsync(context, accountName, subreddits, false);
    }

    private static void changeSubredditAsync(Context context, final String accountName,
            final String[] subreddits, final boolean add) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... fillTheVoid) {
                // Only trigger a sync on real accounts.
                Uri uri;
                if (AccountUtils.isAccount(accountName)) {
                    uri = SubredditProvider.SUBREDDITS_SYNC_URI;
                } else {
                    uri = SubredditProvider.SUBREDDITS_URI;
                }

                int count = subreddits.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count);
                int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;
                for (int i = 0; i < count; i++) {
                    // All subreddit additions require an insert. The insert
                    // would remove any deletes due to table constraints.
                    //
                    // Deletes for an account require an insert with delete
                    // state. However, app storage accounts should just
                    // remove the row altogether.
                    if (add || AccountUtils.isAccount(accountName)) {
                        ops.add(ContentProviderOperation.newInsert(uri)
                                .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                                .withValue(Subreddits.COLUMN_NAME, subreddits[i])
                                .withValue(Subreddits.COLUMN_STATE, state)
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(uri)
                                .withSelection(Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                                        Array.of(accountName, subreddits[i]))
                                .build());
                    }
                }

                return applyOps(appContext, SubredditProvider.AUTHORITY, ops);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    showChangeToast(appContext, add, subreddits.length);
                    scheduleBackup(appContext, accountName);
                } else {
                    Toast.makeText(appContext, R.string.error, Toast.LENGTH_SHORT).show();
                }
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
            final String thingId,
            final String accountName,
            final String body,
            final int nesting,
            final int sequence,
            final long sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(3);

                ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                        .withSelection(BaseProvider.ID_SELECTION, Array.of(parentId))
                        .withValue(Things.COLUMN_NUM_COMMENTS, parentNumComments + 1)
                        .build());

                ops.add(ContentProviderOperation.newInsert(ThingProvider.COMMENT_ACTIONS_URI)
                        .withValue(CommentActions.COLUMN_ACCOUNT, accountName)
                        .withValue(CommentActions.COLUMN_ACTION, CommentActions.ACTION_INSERT)
                        .withValue(CommentActions.COLUMN_TEXT, body)
                        .withValue(CommentActions.COLUMN_PARENT_THING_ID, parentThingId)
                        .withValue(CommentActions.COLUMN_THING_ID, thingId)
                        .build());

                ops.add(ContentProviderOperation.newInsert(ThingProvider.THINGS_NOTIFY_SYNC_URI)
                        .withValue(Things.COLUMN_ACCOUNT, accountName)
                        .withValue(Things.COLUMN_AUTHOR, accountName)
                        .withValue(Things.COLUMN_BODY, body)
                        .withValue(Things.COLUMN_KIND, Kinds.KIND_COMMENT)
                        .withValue(Things.COLUMN_NESTING, nesting)
                        .withValue(Things.COLUMN_SEQUENCE, sequence)
                        .withValue(Things.COLUMN_SESSION_ID, sessionId)
                        .withValueBackReference(Things.COLUMN_COMMENT_ACTION_ID, 1)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void deleteCommentAsync(Context context,
            final String accountName,
            final long headerId,
            final int headerNumComments,
            final String parentId,
            final long[] ids,
            final String[] thingIds,
            final boolean[] hasChildren) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int count = ids.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count * 2 + 1);

                int numDeletes = 0;
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newInsert(ThingProvider.COMMENT_ACTIONS_URI)
                            .withValue(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE)
                            .withValue(CommentActions.COLUMN_ACCOUNT, accountName)
                            .withValue(CommentActions.COLUMN_PARENT_THING_ID, parentId)
                            .withValue(CommentActions.COLUMN_THING_ID, thingIds[i])
                            .build());

                    // Make sure to sync this logic is duplicated in
                    // CommentListing#deleteThing.
                    if (Objects.equals(parentId, thingIds[i])) {
                        // Update things viewed by this account as [deleted]
                        // even in the thing list.
                        // TODO: Make ThingProvider also show [deleted] in thing
                        // list on refresh.
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                                .withValue(Things.COLUMN_AUTHOR, Things.DELETED)
                                .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID,
                                        Array.of(accountName, parentId))
                                .build());
                    } else if (hasChildren[i]) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                                .withValue(Things.COLUMN_AUTHOR, Things.DELETED)
                                .withValue(Things.COLUMN_BODY, Things.DELETED)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(ThingProvider.THINGS_URI)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                        numDeletes++;
                    }
                }

                // Update the header comment by how comments were truly deleted.
                if (numDeletes > 0) {
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_NOTIFY_SYNC_URI)
                            .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID,
                                    Array.of(accountName, parentId))
                            .withValue(Things.COLUMN_NUM_COMMENTS, headerNumComments - numDeletes)
                            .build());
                }

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
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);

                ops.add(ContentProviderOperation.newInsert(ThingProvider.MESSAGE_ACTIONS_URI)
                        .withValue(MessageActions.COLUMN_ACCOUNT, accountName)
                        .withValue(MessageActions.COLUMN_ACTION, MessageActions.ACTION_INSERT)
                        .withValue(MessageActions.COLUMN_PARENT_THING_ID, parentThingId)
                        .withValue(MessageActions.COLUMN_TEXT, body)
                        .withValue(MessageActions.COLUMN_THING_ID, thingId)
                        .build());

                ops.add(ContentProviderOperation.newInsert(ThingProvider.MESSAGES_NOTIFY_SYNC_URI)
                        .withValue(Messages.COLUMN_ACCOUNT, accountName)
                        .withValue(Messages.COLUMN_AUTHOR, accountName)
                        .withValue(Messages.COLUMN_BODY, body)
                        .withValue(Messages.COLUMN_KIND, Kinds.KIND_MESSAGE)
                        .withValue(Messages.COLUMN_SESSION_ID, sessionId)
                        .withValue(Messages.COLUMN_WAS_COMMENT, false)
                        .withValueBackReference(Messages.COLUMN_MESSAGE_ACTION_ID, 0)
                        .build());

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    /** Mark a message either read or unread. */
    public static void readMessageAsync(final Context context, final String accountName,
            final String thingId, final boolean read) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                int action = read ? ReadActions.ACTION_READ : ReadActions.ACTION_UNREAD;
                ContentValues v = new ContentValues(3);
                v.put(ReadActions.COLUMN_ACCOUNT, accountName);
                v.put(ReadActions.COLUMN_THING_ID, thingId);
                v.put(ReadActions.COLUMN_ACTION, action);
                cr.insert(ThingProvider.READ_ACTIONS_NOTIFY_SYNC_URI, v);
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
                return cr.insert(ThingProvider.SAVE_ACTIONS_NOTIFY_SYNC_URI, v) != null;
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
                ContentValues v = new ContentValues(3);
                v.put(SaveActions.COLUMN_ACCOUNT, accountName);
                v.put(SaveActions.COLUMN_THING_ID, thingId);
                v.put(SaveActions.COLUMN_ACTION, SaveActions.ACTION_UNSAVE);

                ContentResolver cr = appContext.getContentResolver();
                return cr.insert(ThingProvider.SAVE_ACTIONS_NOTIFY_SYNC_URI, v) != null;
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
                ContentValues v = new ContentValues(3);
                v.put(VoteActions.COLUMN_ACCOUNT, accountName);
                v.put(VoteActions.COLUMN_THING_ID, thingId);
                v.put(VoteActions.COLUMN_ACTION, likes);

                // No toast needed, since the vote arrows will reflect success.
                cr.insert(ThingProvider.VOTE_ACTIONS_NOTIFY_SYNC_URI, v);
            }
        });
    }

    static boolean applyOps(Context context, String authority,
            ArrayList<ContentProviderOperation> ops) {
        try {
            context.getContentResolver().applyBatch(authority, ops);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return false;
    }
}
