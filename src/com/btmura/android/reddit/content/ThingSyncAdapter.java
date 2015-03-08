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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
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
import com.btmura.android.reddit.net.Result;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ThingSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "ThingSyncAdapter";

    private static final long EXPIRATION_DURATION_MS = TimeUnit.DAYS.toMillis(5);

    private static final int MIN_FAILURES = 100;

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

    // Sync votes first due to loose rate limit.
    private static final Syncer[] SYNCERS = {
            new VoteSyncer(),
            new HideSyncer(),
            new ReadSyncer(),
            new SaveSyncer(),
            new MessageSyncer(),
            new CommentSyncer(),
    };

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

            int count = SYNCERS.length;
            for (int i = 0; i < count; i++) {
                sync(account, extras, authority, provider, syncResult, SYNCERS[i], limiter,
                        cookie, modhash);
            }

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

    private void sync(Account account, Bundle extras, String authority,
                      ContentProviderClient provider, SyncResult syncResult, Syncer syncer,
                      RateLimiter limiter, String cookie, String modhash) {
        Cursor c = null;
        try {
            // Get all pending actions for this account that haven't been synced.
            c = syncer.query(provider, account.name);

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

            Ops ops = new Ops(syncer.getEstimatedOpCount(count));
            long now = System.currentTimeMillis();

            // Process as many actions until we hit some rate limit.
            for (; c.moveToNext(); count--) {
                try {
                    // Sync the local action to the server over the network.
                    Result result = syncer.sync(getContext(), c, cookie, modhash);
                    if (BuildConfig.DEBUG) {
                        result.logAnyErrors(TAG, syncer.getTag());
                        if (result.hasErrors()) {
                            Log.i(TAG, syncer.getTag()
                                    + " c: " + cookie.length()
                                    + " m: " + modhash.length()
                                    + " f: " + syncer.getSyncFailures(c));
                        }
                    }

                    // Quit processing actions if we hit a rate limit.
                    if (result.hasRateLimitError()) {
                        limiter.updateLimit(result, count > 0);
                        syncResult.stats.numSkippedEntries += count;
                        break;
                    }

                    // Remove the action if there were no errors and move on.
                    if (!result.hasErrors()) {
                        syncer.addDeleteAction(account.name, c, ops);
                        syncResult.stats.numEntries++;
                        continue;
                    }

                    long expiration = syncer.getExpiration(c);
                    int syncFailures = syncer.getSyncFailures(c) + 1;

                    // If no expiration or not enough attempts, extend the expiration.
                    if (expiration == 0 || syncFailures < MIN_FAILURES) {
                        expiration = now + EXPIRATION_DURATION_MS;
                    }

                    // Remove the action if it has expired or update it with the new expiration.
                    if (now > expiration) {
                        syncer.addDeleteAction(account.name, c, ops);
                        syncResult.stats.numEntries++;
                    } else {
                        syncer.addUpdateAction(account.name, c, ops, expiration, syncFailures + 1);
                        syncResult.stats.numSkippedEntries++;
                    }
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
                syncResult.stats.numDeletes += ops.deletes;
                syncResult.stats.numUpdates += ops.updates;
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
