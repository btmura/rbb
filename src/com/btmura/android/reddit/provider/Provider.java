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
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.CommentLogic;
import com.btmura.android.reddit.app.CommentLogic.CursorCommentList;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

/**
 * Provider is a collection of static methods that do user actions which correspond to multiple
 * content provider operations.
 */
public class Provider {

    public static final String TAG = "Provider";

    /** Projection used by {@link #expandInBackground(Context, long)}. */
    private static final String[] EXPAND_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_EXPANDED,
            Comments.COLUMN_NESTING,
    };

    private static final Uri HIDE_URI =
            ThingProvider.HIDE_ACTIONS_URI.buildUpon()
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_SYNC, ThingProvider.TRUE)
                    .build();

    private static final Uri READ_URI =
            ThingProvider.READ_ACTIONS_URI.buildUpon()
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_MESSAGES, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_SYNC, ThingProvider.TRUE)
                    .build();

    private static final Uri SAVE_URI =
            ThingProvider.SAVE_ACTIONS_URI.buildUpon()
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_COMMENTS, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_SYNC, ThingProvider.TRUE)
                    .build();

    private static final Uri VOTE_URI =
            ThingProvider.VOTE_ACTIONS_URI.buildUpon()
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_THINGS, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_NOTIFY_COMMENTS, ThingProvider.TRUE)
                    .appendQueryParameter(ThingProvider.PARAM_SYNC, ThingProvider.TRUE)
                    .build();

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
        int resId = added ? R.plurals.added : R.plurals.deleted;
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
                Bundle extras = new Bundle(9);
                extras.putString(ThingProvider.CALL_EXTRA_ACCOUNT, accountName);
                extras.putString(ThingProvider.CALL_EXTRA_BODY, body);
                extras.putInt(ThingProvider.CALL_EXTRA_NESTING, nesting);
                extras.putLong(ThingProvider.CALL_EXTRA_PARENT_ID, parentId);
                extras.putInt(ThingProvider.CALL_EXTRA_PARENT_NUM_COMMENTS, parentNumComments);
                extras.putString(ThingProvider.CALL_EXTRA_PARENT_THING_ID, parentThingId);
                extras.putInt(ThingProvider.CALL_EXTRA_SEQUENCE, sequence);
                extras.putLong(ThingProvider.CALL_EXTRA_SESSION_ID, sessionId);
                extras.putString(ThingProvider.CALL_EXTRA_THING_ID, thingId);

                ContentResolver cr = appContext.getContentResolver();
                cr.call(ThingProvider.THINGS_URI, ThingProvider.METHOD_INSERT_COMMENT,
                        null, extras);
            }
        });
    }

    // TODO: Move this to ThingProvider#call like commentReplyAsync.
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
                        new ArrayList<ContentProviderOperation>(count * 2 + 2);

                boolean thingDeleted = false;
                int numDeletes = 0;
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newInsert(
                            ThingProvider.COMMENT_ACTIONS_SYNC_URI)
                            .withValue(CommentActions.COLUMN_ACTION, CommentActions.ACTION_DELETE)
                            .withValue(CommentActions.COLUMN_ACCOUNT, accountName)
                            .withValue(CommentActions.COLUMN_PARENT_THING_ID, parentThingId)
                            .withValue(CommentActions.COLUMN_THING_ID, thingIds[i])
                            .build());

                    // Make sure to sync this logic is duplicated in CommentListing#deleteThing.
                    if (Objects.equals(parentThingId, thingIds[i])) {
                        // Update things viewed by this account as [deleted] even in the thing list.
                        String[] selection = Array.of(accountName, parentThingId);
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                                .withValue(Things.COLUMN_AUTHOR, Things.DELETED_AUTHOR)
                                .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selection)
                                .build());
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                                .withValue(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR)
                                .withSelection(Comments.SELECT_BY_ACCOUNT_AND_THING_ID, selection)
                                .build());
                        thingDeleted = true;
                        // TODO: Make ThingProvider also show [deleted] in thing list on refresh.
                    } else if (hasChildren[i]) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                                .withValue(Comments.COLUMN_AUTHOR, Comments.DELETED_AUTHOR)
                                .withValue(Comments.COLUMN_BODY, Comments.DELETED_BODY)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(ThingProvider.COMMENTS_URI)
                                .withSelection(BaseProvider.ID_SELECTION, Array.of(ids[i]))
                                .build());
                        numDeletes++;
                    }
                }

                // Update the header comment by how comments were truly deleted.
                if (numDeletes > 0) {
                    String[] selection = Array.of(accountName, parentThingId);
                    int numComments = headerNumComments - numDeletes;
                    if (!thingDeleted) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                                .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selection)
                                .withValue(Things.COLUMN_NUM_COMMENTS, numComments)
                                .build());
                    }
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                            .withSelection(Comments.SELECT_BY_ACCOUNT_AND_THING_ID, selection)
                            .withValue(Comments.COLUMN_NUM_COMMENTS, numComments)
                            .build());
                }

                applyOps(appContext, ThingProvider.AUTHORITY, ops);
            }
        });
    }

    public static void editAsync(Context context,
            final String accountName,
            final String parentThingId,
            final String thingId,
            final String text,
            final long sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);

                ops.add(ContentProviderOperation.newInsert(ThingProvider.COMMENT_ACTIONS_SYNC_URI)
                        .withValue(CommentActions.COLUMN_ACCOUNT, accountName)
                        .withValue(CommentActions.COLUMN_ACTION, CommentActions.ACTION_EDIT)
                        .withValue(CommentActions.COLUMN_TEXT, text)
                        .withValue(CommentActions.COLUMN_PARENT_THING_ID, parentThingId)
                        .withValue(CommentActions.COLUMN_THING_ID, thingId)
                        .build());

                ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                        .withSelection(Comments.SELECT_BY_SESSION_ID_AND_THING_ID,
                                Array.of(sessionId, thingId))
                        .withValue(Comments.COLUMN_BODY, text)
                        .withValueBackReference(Comments.COLUMN_COMMENT_ACTION_ID, 0)
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
                Cursor c = cr.query(ThingProvider.COMMENTS_URI, EXPAND_PROJECTION,
                        Comments.SELECT_BY_SESSION_ID, Array.of(sessionId),
                        Comments.SORT_BY_SEQUENCE_AND_ID);
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
                            .withValue(Comments.COLUMN_EXPANDED, true)
                            .build());
                    for (int i = 0; i < childCount; i++) {
                        ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                                .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                                .withValue(Comments.COLUMN_EXPANDED, true)
                                .withValue(Comments.COLUMN_VISIBLE, true)
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
                ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                        .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                        .withValue(Comments.COLUMN_EXPANDED, false)
                        .build());
                for (int i = 0; i < childCount; i++) {
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(childIds[i]))
                            .withValue(Comments.COLUMN_EXPANDED, true)
                            .withValue(Comments.COLUMN_VISIBLE, false)
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
                cr.update(AccountProvider.ACCOUNTS_URI, values,
                        Accounts.SELECT_BY_ACCOUNT, Array.of(accountName));
            }
        });
    }

    public static void messageReplyAsync(Context context,
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

                ops.add(ContentProviderOperation.newInsert(ThingProvider.MESSAGES_SYNC_URI)
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
                cr.insert(READ_URI, v);
            }
        });
    }

    public static void hideAsync(final Context context,
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
            final boolean self,
            final String subreddit,
            final String title,
            final String thumbnailUrl,
            final int ups,
            final String url) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ContentValues v = new ContentValues(18);
                v.put(HideActions.COLUMN_ACCOUNT, accountName);
                v.put(HideActions.COLUMN_THING_ID, thingId);
                v.put(HideActions.COLUMN_ACTION, HideActions.ACTION_HIDE);

                // Following values are for faking a thing.
                v.put(HideActions.COLUMN_AUTHOR, author);
                v.put(HideActions.COLUMN_CREATED_UTC, createdUtc);
                v.put(HideActions.COLUMN_DOMAIN, domain);
                v.put(HideActions.COLUMN_DOWNS, downs);
                v.put(HideActions.COLUMN_LIKES, likes);
                v.put(HideActions.COLUMN_NUM_COMMENTS, numComments);
                v.put(HideActions.COLUMN_OVER_18, over18);
                v.put(HideActions.COLUMN_PERMA_LINK, permaLink);
                v.put(HideActions.COLUMN_SELF, self);
                v.put(HideActions.COLUMN_SCORE, score);
                v.put(HideActions.COLUMN_SUBREDDIT, subreddit);
                v.put(HideActions.COLUMN_TITLE, title);
                v.put(HideActions.COLUMN_THUMBNAIL_URL, thumbnailUrl);
                v.put(HideActions.COLUMN_UPS, ups);
                v.put(HideActions.COLUMN_URL, url);

                cr.insert(HIDE_URI, v);
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
            final boolean self,
            final String subreddit,
            final String title,
            final String thumbnailUrl,
            final int ups,
            final String url) {

        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voidRay) {
                ContentValues v = new ContentValues(18);
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
                v.put(SaveActions.COLUMN_SELF, self);
                v.put(SaveActions.COLUMN_SCORE, score);
                v.put(SaveActions.COLUMN_SUBREDDIT, subreddit);
                v.put(SaveActions.COLUMN_TITLE, title);
                v.put(SaveActions.COLUMN_THUMBNAIL_URL, thumbnailUrl);
                v.put(SaveActions.COLUMN_UPS, ups);
                v.put(SaveActions.COLUMN_URL, url);

                ContentResolver cr = appContext.getContentResolver();
                return cr.insert(SAVE_URI, v) != null;
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
                return cr.insert(SAVE_URI, v) != null;
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

    /**
     * Vote on something but don't make it visible in the liked/disliked listing while the vote is
     * still pending.
     */
    public static void voteAsync(Context context,
            final String accountName,
            final int action,
            final String thingId) {

        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentValues v = new ContentValues(3);
                v.put(VoteActions.COLUMN_ACCOUNT, accountName);
                v.put(VoteActions.COLUMN_ACTION, action);
                v.put(VoteActions.COLUMN_THING_ID, thingId);

                // No toast needed, since the vote arrows will reflect success.
                cr.insert(VOTE_URI, v);
            }
        });
    }

    /**
     * Vote on something and store additional information to make it visible in the liked/disliked
     * listing while the vote is still pending.
     */
    public static void voteAsync(Context context,
            final String accountName,
            final int action,

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
            final boolean self,
            final String subreddit,
            final String thingId,
            final String title,
            final String thumbnailUrl,
            final int ups,
            final String url) {

        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentValues v = new ContentValues(19);
                v.put(VoteActions.COLUMN_ACCOUNT, accountName);
                v.put(VoteActions.COLUMN_ACTION, action);
                v.put(VoteActions.COLUMN_SHOW_IN_LISTING, true);
                v.put(VoteActions.COLUMN_THING_ID, thingId);

                // Following values are for faking a thing.
                v.put(VoteActions.COLUMN_AUTHOR, author);
                v.put(VoteActions.COLUMN_CREATED_UTC, createdUtc);
                v.put(VoteActions.COLUMN_DOMAIN, domain);
                v.put(VoteActions.COLUMN_DOWNS, downs);
                v.put(VoteActions.COLUMN_LIKES, likes);
                v.put(VoteActions.COLUMN_NUM_COMMENTS, numComments);
                v.put(VoteActions.COLUMN_OVER_18, over18);
                v.put(VoteActions.COLUMN_PERMA_LINK, permaLink);
                v.put(VoteActions.COLUMN_SCORE, score);
                v.put(VoteActions.COLUMN_SELF, self);
                v.put(VoteActions.COLUMN_SUBREDDIT, subreddit);
                v.put(VoteActions.COLUMN_TITLE, title);
                v.put(VoteActions.COLUMN_THUMBNAIL_URL, thumbnailUrl);
                v.put(VoteActions.COLUMN_UPS, ups);
                v.put(VoteActions.COLUMN_URL, url);

                // No toast needed, since the vote arrows will reflect success.
                cr.insert(VOTE_URI, v);
            }
        });
    }

    public static void deleteSessionAsync(Context context, final Uri uri, final long sessionId) {
        if (sessionId == -1) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String[] selectionArgs = Array.of(sessionId);
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(2);
                ops.add(ContentProviderOperation.newDelete(ThingProvider.SESSIONS_URI)
                        .withSelection(Sessions.SELECT_BY_ID, selectionArgs)
                        .build());
                ops.add(ContentProviderOperation.newDelete(uri)
                        .withSelection(SharedColumns.SELECT_BY_SESSION_ID, selectionArgs)
                        .build());
                applyOps(appContext, ThingProvider.AUTHORITY, ops);
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
