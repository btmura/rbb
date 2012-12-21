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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.provider.MessageProvider;
import com.btmura.android.reddit.util.Array;

public class MessageSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "MessageSyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new MessageSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    public MessageSyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // Extra method just allows us to always print out sync stats after.
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

            String modhash = AccountUtils.getModhash(getContext(), account);
            if (modhash == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            syncActions(account, extras, authority, provider, syncResult, cookie, modhash);

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numIoExceptions++;
        }
    }

    private static final String[] ACTION_PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_THING_ID,
            MessageActions.COLUMN_TEXT,
    };

    private static final int ACTION_INDEX_ID = 0;
    private static final int ACTION_INDEX_ACTION = 1;
    private static final int ACTION_INDEX_THING_ID = 2;
    private static final int ACTION_INDEX_TEXT = 3;

    private static final String ACTION_SELECTION = MessageActions.COLUMN_ACCOUNT + "=?";

    private static final String ACTION_SORT = MessageActions._ID + " ASC";

    // TODO: Remove duplicate logic with ThingSyncAdapter.
    private void syncActions(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult, String cookie, String modhash) {
        try {
            // Get all pending comments that have not been synced.
            Cursor c = provider.query(MessageProvider.ACTIONS_URI, ACTION_PROJECTION,
                    ACTION_SELECTION, Array.of(account.name), ACTION_SORT);

            int count = c.getCount();

            // Process one reply at a time to avoid rate limit.
            long id = -1;
            int action = -1;
            String thingId = null;
            String text = null;
            if (c.moveToNext()) {
                id = c.getLong(ACTION_INDEX_ID);
                action = c.getInt(ACTION_INDEX_ACTION);
                thingId = c.getString(ACTION_INDEX_THING_ID);
                text = c.getString(ACTION_INDEX_TEXT);
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
                if (action == MessageActions.ACTION_INSERT) {
                    result = RedditApi.comment(thingId, text, cookie, modhash);
                } else if (action == MessageActions.ACTION_DELETE) {
                    result = RedditApi.delete(thingId, cookie, modhash);
                }

                // Log any errors if there were any.
                if (BuildConfig.DEBUG) {
                    result.logAnyErrors(TAG);
                }

                if (!result.shouldRetry()) {
                    syncResult.stats.numDeletes += provider.delete(
                            MessageProvider.ACTIONS_URI,
                            MessageProvider.ID_SELECTION, Array.of(id));
                    count--;
                }

                // Record the number of entries left for stats purposes
                // only.
                syncResult.stats.numSkippedEntries += count;

                // Respect suggested rate limit or assign our own.
                long rateLimit = ThingSyncAdapter.RATE_LIMIT_SECONDS;
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
}
