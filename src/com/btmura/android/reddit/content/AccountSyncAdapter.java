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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.AccountResult;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.util.Array;

/**
 * {@link AbstractThreadedSyncAdapter} for periodically syncing account
 * information using the /api/me API method to check for mail and other info.
 */
public class AccountSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "AccountSyncAdapter";

    private static final int POLL_FREQUENCY_SECONDS = 24 * 60 * 60; // 1 day

    private static final String ACCOUNT_SELECTION = Accounts.COLUMN_ACCOUNT + "=?";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new AccountSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    public AccountSyncAdapter(Context context) {
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

        // Schedule the next sync to check messages.
        ContentResolver.addPeriodicSync(account, authority, extras, POLL_FREQUENCY_SECONDS);
    }

    private void doSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            // Get the necessary account credentials or bail out.
            String cookie = AccountUtils.getCookie(getContext(), account);
            if (cookie == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            // Get the account information.
            AccountResult result = RedditApi.me(cookie);

            // Delete and then insert to use a single transaction rather than
            // doing a update and then insert which can't be done in a
            // transaction as easily.
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(2);
            ops.add(ContentProviderOperation.newDelete(AccountProvider.ACCOUNTS_URI)
                    .withSelection(ACCOUNT_SELECTION, Array.of(account.name))
                    .build());
            ops.add(ContentProviderOperation.newInsert(AccountProvider.ACCOUNTS_URI)
                    .withValue(Accounts.COLUMN_ACCOUNT, account.name)
                    .withValue(Accounts.COLUMN_HAS_MAIL, result.hasMail)
                    .build());

            // Tally up the results.
            ContentProviderResult[] results = provider.applyBatch(ops);
            syncResult.stats.numDeletes = results[0].count;
            syncResult.stats.numInserts++;

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        }
    }
}
