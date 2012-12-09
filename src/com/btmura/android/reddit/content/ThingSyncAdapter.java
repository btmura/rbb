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
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

/**
 * {@link AbstractThreadedSyncAdapter} that syncs pending replies to the server.
 * It processes one reply at a time to avoid hitting the rate limit. It
 * schedules a periodic sync when it needs to sync the remaining pending
 * replies.
 */
public class ThingSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "ThingSyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new ThingSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    /** Rate limit in seconds if the server doesn't suggest one. */
    private static final int RATE_LIMIT_SECONDS = 60;

    private static final String[] COMMENT_PROJECTION = {
            Comments._ID,
            Comments.COLUMN_ACTION,
            Comments.COLUMN_THING_ID,
            Comments.COLUMN_TEXT,
    };

    private static final int COMMENT_ID = 0;
    private static final int COMMENT_ACTION = 1;
    private static final int COMMENT_THING_ID = 2;
    private static final int COMMENT_TEXT = 3;

    private static final String[] VOTE_PROJECTION = {
            Votes._ID,
            Votes.COLUMN_THING_ID,
            Votes.COLUMN_VOTE,
    };

    private static final int VOTE_ID = 0;
    private static final int VOTE_THING_ID = 1;
    private static final int VOTE_VOTE = 2;

    public ThingSyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        doSync(account, extras, authority, provider, syncResult);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "accountName: " + account.name + " syncResult: " + syncResult.toString());
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

            // Sync comments and votes. Exceptions are handled within the
            // methods to make sure both comments and votes are processed each
            // time.
            syncComments(account, extras, authority, provider, syncResult, cookie, modhash);
            syncVotes(account, extras, authority, provider, syncResult, cookie, modhash);

        } catch (OperationCanceledException e) {
            // Exception thrown from getting cookie or modhash.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (AuthenticatorException e) {
            // Exception thrown from getting cookie or modhash.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
            return;
        } catch (IOException e) {
            // Exception thrown when requesting cookie or modhash on network.
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numIoExceptions++;
            return;
        }
    }

    private void syncComments(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, String cookie, String modhash) {
        try {
            // Get all pending replies that have not been synced.
            Cursor c = provider.query(ThingProvider.COMMENTS_URI, COMMENT_PROJECTION,
                    Comments.SELECT_BY_ACCOUNT, Array.of(account.name),
                    Comments.SORT_BY_ID);

            int count = c.getCount();

            // Process one reply at a time to avoid rate limit.
            long id = -1;
            int action = -1;
            String thingId = null;
            String text = null;
            if (c.moveToNext()) {
                id = c.getLong(COMMENT_ID);
                action = c.getInt(COMMENT_ACTION);
                thingId = c.getString(COMMENT_THING_ID);
                text = c.getString(COMMENT_TEXT);
            }

            // Close cursor before making network request.
            c.close();

            // Exit early if nothing to process.
            if (id == -1) {
                return;
            }

            try {
                // Try to sync the comment with the server.
                Result result = null;
                if (action == Comments.ACTION_INSERT) {
                    result = RedditApi.comment(thingId, text, cookie, modhash);
                } else if (action == Comments.ACTION_DELETE) {
                    result = RedditApi.delete(thingId, cookie, modhash);
                }

                // Log any errors if there were any.
                if (BuildConfig.DEBUG) {
                    result.logAnyErrors(TAG);
                }

                if (!result.shouldRetry()) {
                    syncResult.stats.numDeletes += provider.delete(
                            ThingProvider.COMMENTS_URI,
                            ThingProvider.ID_SELECTION, Array.of(id));
                    count--;
                }

                // Record the number of entries left for stats purposes
                // only.
                syncResult.stats.numSkippedEntries += count;

                // Respect suggested rate limit or assign our own.
                long rateLimit = RATE_LIMIT_SECONDS;
                if (result.rateLimit > 0) {
                    rateLimit = Math.round(result.rateLimit);
                }

                // SyncManager code seems to be using delayUntil as a
                // timestamp even though the docs say its more of a
                // duration.
                syncResult.delayUntil = System.currentTimeMillis() / 1000 + rateLimit;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "rateLimit: " + rateLimit
                            + " delayUntil: " + syncResult.delayUntil);
                }

                // Use a periodic sync to sync the remaining replies.
                if (count > 0) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "adding periodic sync with rate limit: " + rateLimit);
                    }
                    ContentResolver.addPeriodicSync(account, authority,
                            Bundle.EMPTY, rateLimit);
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "removing periodic sync");
                    }
                    ContentResolver.removePeriodicSync(account, authority,
                            Bundle.EMPTY);
                }
            } catch (IOException e) {
                // If we had a network problem then increment the exception
                // count to indicate a soft error. The sync manager will
                // keep retrying this with exponential back-off.
                Log.e(TAG, e.getMessage(), e);
                syncResult.stats.numIoExceptions++;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        }
    }

    private void syncVotes(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, String cookie, String modhash) {
        try {
            // Get all pending votes for this account that haven't been synced.
            Cursor c = provider.query(ThingProvider.VOTES_URI, VOTE_PROJECTION,
                    Votes.SELECT_BY_ACCOUNT, Array.of(account.name), null);

            // TODO: Drop duplicate votes to minimize number of RPCs.

            // Bail out early if there are no votes to process.
            if (c.getCount() == 0) {
                c.close();
                return;
            }

            int opCount = c.getCount() * 2;
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(opCount);

            while (c.moveToNext()) {
                long id = c.getLong(VOTE_ID);
                String thingId = c.getString(VOTE_THING_ID);
                int vote = c.getInt(VOTE_VOTE);

                // Sync the vote with the server. If successful then schedule
                // deletion of the database row.
                try {
                    RedditApi.vote(getContext(), thingId, vote, cookie, modhash);
                    ops.add(ContentProviderOperation.newDelete(ThingProvider.VOTES_URI)
                            .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                            .build());

                    // Update the tables that join with the votes table since we
                    // will delete the pending vote rows afterwards.
                    String[] selectionArgs = Array.of(account.name, thingId);
                    ops.add(ContentProviderOperation.newUpdate(ThingProvider.THINGS_URI)
                            .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs)
                            .withValue(Things.COLUMN_LIKES, vote)
                            .build());
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }
            c.close();

            // Now delete the rows from the database.
            // The server shows the updates immediately.
            ContentProviderResult[] results = provider.applyBatch(ops);
            for (int i = 0; i < opCount;) {
                syncResult.stats.numDeletes += results[i++].count;
                syncResult.stats.numUpdates += results[i++].count;
            }

        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        }
    }
}
