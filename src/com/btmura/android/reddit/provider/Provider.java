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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
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

    /** Inserts a placeholder comment yet to be synced with Reddit. */
    public static void commentReplyAsync(Context context,
            final String accountName,
            final String body,
            final String parentThingId,
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.insertComment(appContext, accountName, body, parentThingId, thingId);
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
            @Override
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
            @Override
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

    public static void expandCommentAsync(Context context, final long id, final long sessionId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.expandComment(appContext, id, sessionId);
            }
        });
    }

    public static void collapseCommentAsync(Context context, final long id,
            final long[] childIds) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.collapseComment(appContext, id, childIds);
            }
        });
    }

    public static void clearNewMessageIndicator(Context context, final String accountName,
            final boolean hasMail) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
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
            final String thingId) {
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ThingProvider.insertMessage(appContext, accountName, body, parentThingId, thingId);
            }
        });
    }

    /** Mark a message either read or unread. */
    public static void readMessageAsync(final Context context, final String accountName,
            final String thingId, final boolean read) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
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
            // Following parameters are for faking a thing.
            // TODO: Use a bundle argument for this instead.
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
            final String thumbnailUrl,
            final String title,
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

    public static void unhideAsync(final Context context,
            final String accountName,
            final String thingId) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                ContentValues v = new ContentValues(3);
                v.put(HideActions.COLUMN_ACCOUNT, accountName);
                v.put(HideActions.COLUMN_THING_ID, thingId);
                v.put(HideActions.COLUMN_ACTION, HideActions.ACTION_UNHIDE);
                cr.insert(HIDE_URI, v);
            }
        });
    }

    public static void saveAsync(final Context context,
            final String accountName,

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
            final String thumbnailUrl,
            final String title,
            final int ups,
            final String url) {

        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
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

                cr.insert(SAVE_URI, v);
            }
        });
    }

    public static void unsaveAsync(final Context context, final String accountName,
            final String thingId) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ContentValues v = new ContentValues(3);
                v.put(SaveActions.COLUMN_ACCOUNT, accountName);
                v.put(SaveActions.COLUMN_THING_ID, thingId);
                v.put(SaveActions.COLUMN_ACTION, SaveActions.ACTION_UNSAVE);
                cr.insert(SAVE_URI, v);
            }
        });
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
            @Override
            public void run() {
                ContentValues v = new ContentValues(3);
                v.put(VoteActions.COLUMN_ACCOUNT, accountName);
                v.put(VoteActions.COLUMN_ACTION, action);
                v.put(VoteActions.COLUMN_THING_ID, thingId);
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
            final String thumbnailUrl,
            final String title,
            final int ups,
            final String url) {
        final ContentResolver cr = context.getApplicationContext().getContentResolver();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
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

    static boolean applyOps(Context context,
            String authority,
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

    static void scheduleBackup(Context context, String accountName) {
        if (!AccountUtils.isAccount(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }
}
