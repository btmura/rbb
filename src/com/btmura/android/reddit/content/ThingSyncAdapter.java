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

package com.btmura.android.reddit.content;

import java.io.IOException;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

public class ThingSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "ThingSyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new ThingSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    /** Report so far of the sync and whether we will need another sync. */
    static class RateLimiter {

        /** Rate limit until the next sync operation should happen. */
        long rateLimit;

        /** True if we need another sync to finish our work. */
        boolean needExtraSync;

        void updateLimit(Result result, boolean workLeft) {
            // Update the rate limit if the server said to back off.
            if (rateLimit < result.rateLimit) {
                rateLimit = Math.round(result.rateLimit);
            }

            // Indicate whether we need an extra sync to finish.
            needExtraSync |= workLeft;
        }
    }

    private static final String[] COMMENT_PROJECTION = {
            CommentActions._ID,
            CommentActions.COLUMN_ACTION,
            CommentActions.COLUMN_THING_ID,
            CommentActions.COLUMN_TEXT,
    };

    private static final int COMMENT_ID = 0;
    private static final int COMMENT_ACTION = 1;
    private static final int COMMENT_THING_ID = 2;
    private static final int COMMENT_TEXT = 3;

    private static final String[] MESSAGE_PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_THING_ID,
            MessageActions.COLUMN_TEXT,
    };

    private static final int MESSAGE_ID = 0;
    private static final int MESSAGE_ACTION = 1;
    private static final int MESSAGE_THING_ID = 2;
    private static final int MESSAGE_TEXT = 3;

    private static final String[] SAVE_PROJECTION = {
            SaveActions._ID,
            SaveActions.COLUMN_THING_ID,
            SaveActions.COLUMN_ACTION,
    };

    private static final int SAVE_ID = 0;
    private static final int SAVE_THING_ID = 1;
    private static final int SAVE_ACTION = 2;

    private static final String[] VOTE_PROJECTION = {
            VoteActions._ID,
            VoteActions.COLUMN_THING_ID,
            VoteActions.COLUMN_ACTION,
    };

    private static final int VOTE_ID = 0;
    private static final int VOTE_THING_ID = 1;
    private static final int VOTE_VOTE = 2;

    private static final String SELECT_BY_ACCOUNT = VoteActions.COLUMN_ACCOUNT + " = ?";
    private static final String SORT_BY_ID = BaseColumns._ID + " ASC";

    public ThingSyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPerformSync START account: " + account.name);
        }
        doSync(account, extras, authority, provider, syncResult);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPerformSync FINISH account: " + account.name
                    + " syncResult: " + syncResult.toString());
        }
    }

    private void doSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            AccountManager manager = AccountManager.get(getContext());

            String cookie = AccountUtils.getCookie(manager, account);
            if (cookie == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            String modhash = AccountUtils.getModhash(manager, account);
            if (modhash == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            RateLimiter limiter = new RateLimiter();

            // Sync votes first due to loose rate limit.
            syncVotes(account, extras, authority, provider, syncResult, limiter, cookie, modhash);
            syncSaves(account, extras, authority, provider, syncResult, limiter, cookie, modhash);
            syncMessages(account, extras, authority, provider, syncResult, limiter, cookie, modhash);
            syncComments(account, extras, authority, provider, syncResult, limiter, cookie, modhash);

            // SyncManager code seems to be using delayUntil as a timestamp even
            // though the docs say its more of a duration.
            if (limiter.rateLimit > 0) {
                syncResult.delayUntil = System.currentTimeMillis() / 1000 + limiter.rateLimit;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "rateLimit: " + limiter.rateLimit);
                }
            }

            // Schedule or cancel periodic sync to handle the next batch.
            if (limiter.needExtraSync) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "adding periodic sync");
                }
                ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY,
                        limiter.rateLimit);
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "removing periodic sync");
                }
                ContentResolver.removePeriodicSync(account, authority, Bundle.EMPTY);
            }

        } catch (OperationCanceledException e) {
            // Exception thrown from getting cookie or modhash.
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (AuthenticatorException e) {
            // Exception thrown from getting cookie or modhash.
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (IOException e) {
            // Exception thrown when requesting cookie or modhash on network.
            // Soft exception that sync manager will retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numIoExceptions++;
            return;
        }
    }

    private void syncVotes(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, RateLimiter limiter,
            String cookie, String modhash) {
        Cursor c = null;
        try {
            // Get all pending votes for this account that haven't been synced.
            c = provider.query(ThingProvider.VOTE_ACTIONS_URI, VOTE_PROJECTION,
                    SELECT_BY_ACCOUNT, Array.of(account.name), null);

            int count = c.getCount();

            // Bail out early if there is nothing to do.
            if (count == 0) {
                return;
            }

            // Record skipped if the rate limit has been reached.
            if (limiter.rateLimit > 0) {
                syncResult.stats.numSkippedEntries += count;
                return;
            }

            // 2 ops per vote: 1 for vote and 1 to update affected sessions.
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(count * 2);

            // Process as many votes until we hit some rate limit.
            for (; c.moveToNext(); count--) {
                long id = c.getLong(VOTE_ID);
                String thingId = c.getString(VOTE_THING_ID);
                int vote = c.getInt(VOTE_VOTE);

                try {
                    Result result = RedditApi.vote(getContext(), thingId, vote, cookie, modhash);
                    if (BuildConfig.DEBUG) {
                        result.logAnyErrors(TAG);
                    }

                    // Quit processing votes if we hit a rate limit.
                    if (result.hasRateLimitError()) {
                        limiter.updateLimit(result, count > 0);
                        syncResult.stats.numSkippedEntries += count;
                        break;
                    }

                    // Delete the row corresponding to the pending vote.
                    ops.add(ContentProviderOperation.newDelete(ThingProvider.VOTE_ACTIONS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .build());

                    // Update the tables that join with the votes table
                    // since we will delete the pending vote rows.
                    String[] selectionArgs = Array.of(account.name, thingId);
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                            .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs)
                            .withValue(Things.COLUMN_LIKES, vote)
                            .build());

                } catch (IOException e) {
                    // If we had a network problem then increment the exception
                    // count to indicate a soft error. The sync manager will
                    // keep retrying this with exponential back-off.
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }

            if (!ops.isEmpty()) {
                ContentProviderResult[] results = provider.applyBatch(ops);
                int opCount = ops.size();
                for (int i = 0; i < opCount;) {
                    syncResult.stats.numDeletes += results[i++].count;
                    syncResult.stats.numUpdates += results[i++].count;
                }
            }

        } catch (RemoteException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void syncSaves(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, RateLimiter limiter,
            String cookie, String modhash) {
        Cursor c = null;
        try {
            // Get all pending saves for this account that haven't been synced.
            c = provider.query(ThingProvider.SAVE_ACTIONS_URI, SAVE_PROJECTION,
                    SELECT_BY_ACCOUNT, Array.of(account.name), null);

            int count = c.getCount();

            // Bail out early if there is nothing to do.
            if (count == 0) {
                return;
            }

            // Record skipped if the rate limit has been reached.
            if (limiter.rateLimit > 0) {
                syncResult.stats.numSkippedEntries += count;
                return;
            }

            // 2 ops per save. 1 for save and 1 to update affected sessions.
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(count * 2);

            // Process as many saves until we hit some rate limit.
            for (; c.moveToNext(); count--) {
                long id = c.getLong(SAVE_ID);
                String thingId = c.getString(SAVE_THING_ID);
                int action = c.getInt(SAVE_ACTION);
                boolean saved = action == SaveActions.ACTION_SAVE;

                try {
                    Result result = RedditApi.save(thingId, saved, cookie, modhash);
                    if (BuildConfig.DEBUG) {
                        result.logAnyErrors(TAG);
                    }

                    // Quit processing votes if we hit a rate limit.
                    if (result.hasRateLimitError()) {
                        limiter.updateLimit(result, count > 0);
                        syncResult.stats.numSkippedEntries += count;
                        break;
                    }

                    // Delete the row corresponding to the pending save.
                    ops.add(ContentProviderOperation.newDelete(ThingProvider.SAVE_ACTIONS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .build());

                    // Update the tables that join with the saves table
                    // since we will delete the pending save rows.
                    String[] selectionArgs = Array.of(account.name, thingId);
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                            .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs)
                            .withValue(Things.COLUMN_SAVED, saved)
                            .build());

                } catch (IOException e) {
                    // If we had a network problem then increment the exception
                    // count to indicate a soft error. The sync manager will
                    // keep retrying this with exponential back-off.
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }

            if (!ops.isEmpty()) {
                ContentProviderResult[] results = provider.applyBatch(ops);
                int opCount = ops.size();
                for (int i = 0; i < opCount;) {
                    syncResult.stats.numDeletes += results[i++].count;
                    syncResult.stats.numUpdates += results[i++].count;
                }
            }

        } catch (RemoteException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    // TODO: Remove duplicate logic with ThingSyncAdapter.
    private void syncMessages(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, RateLimiter limiter,
            String cookie, String modhash) {
        Cursor c = null;
        try {
            // Get all pending comments that have not been synced.
            c = provider.query(ThingProvider.MESSAGE_ACTIONS_URI, MESSAGE_PROJECTION,
                    SELECT_BY_ACCOUNT, Array.of(account.name), SORT_BY_ID);

            int count = c.getCount();

            // Bail out early if there is nothing to do.
            if (count == 0) {
                return;
            }

            // Record skipped if the rate limit has been reached.
            if (limiter.rateLimit > 0) {
                syncResult.stats.numSkippedEntries += count;
                return;
            }

            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(count);

            // Process as many messages until we hit a rate limit.
            for (; c.moveToNext(); count--) {
                long id = c.getLong(MESSAGE_ID);
                int action = c.getInt(MESSAGE_ACTION);
                String thingId = c.getString(MESSAGE_THING_ID);
                String text = c.getString(MESSAGE_TEXT);

                try {
                    // Try to sync the message with the server.
                    Result result = null;
                    switch (action) {
                        case MessageActions.ACTION_INSERT:
                            result = RedditApi.comment(thingId, text, cookie, modhash);

                        case MessageActions.ACTION_DELETE:
                            result = RedditApi.delete(thingId, cookie, modhash);
                            break;

                        case MessageActions.ACTION_READ:
                            result = RedditApi.readMessage(thingId, true, cookie, modhash);
                            break;

                        case MessageActions.ACTION_UNREAD:
                            result = RedditApi.readMessage(thingId, false, cookie, modhash);
                            break;

                        default:
                            throw new IllegalArgumentException();
                    }

                    if (BuildConfig.DEBUG) {
                        result.logAnyErrors(TAG);
                    }

                    // Quit processing messages if we hit a rate limit.
                    if (result.hasRateLimitError()) {
                        limiter.updateLimit(result, count > 0);
                        syncResult.stats.numSkippedEntries += count;
                        break;
                    }

                    // Delete the row corresponding to the pending action.
                    ops.add(ContentProviderOperation.newDelete(ThingProvider.MESSAGE_ACTIONS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .build());

                } catch (IOException e) {
                    // If we had a network problem then increment the exception
                    // count to indicate a soft error. The sync manager will
                    // keep retrying this with exponential back-off.
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }

            if (!ops.isEmpty()) {
                ContentProviderResult[] results = provider.applyBatch(ops);
                int opCount = ops.size();
                for (int i = 0; i < opCount; i++) {
                    syncResult.stats.numDeletes += results[i].count;
                }
            }

        } catch (RemoteException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void syncComments(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, RateLimiter limiter,
            String cookie, String modhash) {
        Cursor c = null;
        try {
            // Get all pending replies that have not been synced.
            c = provider.query(ThingProvider.COMMENT_ACTIONS_URI, COMMENT_PROJECTION,
                    CommentActions.SELECT_BY_ACCOUNT, Array.of(account.name), SORT_BY_ID);

            int count = c.getCount();

            // Bail out early if there is nothing to do.
            if (count == 0) {
                return;
            }

            // Record skipped if the rate limit has been reached.
            if (limiter.rateLimit > 0) {
                syncResult.stats.numSkippedEntries += count;
                return;
            }

            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(count);

            // Process as many comments until we hit a rate limit.
            for (; c.moveToNext(); count--) {
                long id = c.getLong(COMMENT_ID);
                int action = c.getInt(COMMENT_ACTION);
                String thingId = c.getString(COMMENT_THING_ID);
                String text = c.getString(COMMENT_TEXT);

                try {
                    // Try to sync the comment with the server.
                    Result result = null;
                    switch (action) {
                        case CommentActions.ACTION_INSERT:
                            result = RedditApi.comment(thingId, text, cookie, modhash);
                            break;

                        case CommentActions.ACTION_DELETE:
                            result = RedditApi.delete(thingId, cookie, modhash);
                            break;

                        default:
                            throw new IllegalArgumentException();
                    }

                    if (BuildConfig.DEBUG) {
                        result.logAnyErrors(TAG);
                    }

                    // Quit processing comments if we hit a rate limit.
                    if (result.hasRateLimitError()) {
                        limiter.updateLimit(result, count > 0);
                        syncResult.stats.numSkippedEntries += count;
                        break;
                    }

                    ops.add(ContentProviderOperation.newDelete(ThingProvider.COMMENT_ACTIONS_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .build());

                } catch (IOException e) {
                    // If we had a network problem then increment the exception
                    // count to indicate a soft error. The sync manager will
                    // keep retrying this with exponential back-off.
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }

            if (!ops.isEmpty()) {
                ContentProviderResult[] results = provider.applyBatch(ops);
                int opCount = ops.size();
                for (int i = 0; i < opCount; i++) {
                    syncResult.stats.numDeletes += results[i].count;
                }
            }

        } catch (RemoteException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            // Hard error so the sync manager won't retry.
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
}
