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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
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
  public void onPerformSync(
      Account account,
      Bundle extras,
      String authority,
      ContentProviderClient provider,
      SyncResult syncResult) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "onPerformSync START a: " + account.name);
    }
    doSync(account, authority, provider, syncResult);
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "onPerformSync FINISH a: " + account.name
          + " sr: " + syncResult.toString());
    }
  }

  private void doSync(
      Account account,
      String authority,
      ContentProviderClient provider,
      SyncResult syncResult) {
    try {
      if (!AccountUtils.hasTokens(getContext(), account.name)) {
        syncResult.stats.numAuthExceptions++;
        return;
      }
      RateLimiter limiter = new RateLimiter();

      int count = SYNCERS.length;
      for (int i = 0; i < count; i++) {
        sync(account, provider, syncResult, SYNCERS[i], limiter);
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
    } catch (AuthenticatorException e) {
      // Exception thrown from getting cookie or modhash.
      // Hard error so the sync manager won't retry.
      Log.e(TAG, e.getMessage(), e);
      syncResult.stats.numAuthExceptions++;
    } catch (IOException e) {
      // Exception thrown when requesting cookie or modhash on network.
      // Soft exception that sync manager will retry.
      Log.e(TAG, e.getMessage(), e);
      syncResult.stats.numIoExceptions++;
    }
  }

  private void sync(
      Account account,
      ContentProviderClient provider,
      SyncResult syncResult,
      Syncer syncer,
      RateLimiter limiter) {
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
          Result result = syncer.sync(getContext(), account.name, c);
          int syncFailures = syncer.getSyncFailures(c);
          CharSequence syncStatus = result.getErrorCodeMessage();

          if (BuildConfig.DEBUG) {
            result.logAnyErrors(TAG, syncer.getTag());
            if (result.hasErrors()) {
              Log.i(TAG, syncer.getTag()
                  + "[" + c.getPosition() + "/" + c.getCount() + "] "
                  + " f: " + syncFailures
                  + " s: " + syncStatus);
            }
          }

          // Quit processing actions if we hit a rate limit.
          if (result.hasRateLimitError() || result.hasUserRequiredError()) {
            limiter.updateLimit(result, count > 0);
            syncer.addUpdateAction(c, ops, syncFailures + 1,
                syncStatus.toString());
            syncResult.stats.numSkippedEntries++;
            break;
          }

          syncer.addDeleteAction(c, ops);
          syncResult.stats.numEntries++;
        } catch (IOException e) {
          // If we had a network problem then increment the exception
          // count to indicate a soft error. The sync manager will
          // keep retrying this with exponential back-off.
          Log.e(TAG, e.getMessage(), e);
          syncResult.stats.numIoExceptions++;
        } catch (AuthenticatorException e) {
          Log.e(TAG, e.getMessage(), e);
          syncResult.stats.numAuthExceptions++;
        } catch (OperationCanceledException e) {
          Log.e(TAG, e.getMessage(), e);
          syncResult.stats.numAuthExceptions++;
        }
      }

      if (!ops.isEmpty()) {
        provider.applyBatch(ops);
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
