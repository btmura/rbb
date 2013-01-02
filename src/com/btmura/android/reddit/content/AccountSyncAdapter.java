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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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

/**
 * {@link AbstractThreadedSyncAdapter} for periodically syncing account
 * information using the /api/me API method to check for mail and other info.
 */
public class AccountSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "AccountSyncAdapter";

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

            // Insert or replace the existing row and notify any loaders.
            ContentValues values = new ContentValues(4);
            values.put(Accounts.COLUMN_ACCOUNT, account.name);
            values.put(Accounts.COLUMN_LINK_KARMA, result.linkKarma);
            values.put(Accounts.COLUMN_COMMENT_KARMA, result.commentKarma);
            values.put(Accounts.COLUMN_HAS_MAIL, result.hasMail);
            provider.insert(AccountProvider.ACCOUNTS_NOTIFY_URI, values);

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
        }
    }
}
