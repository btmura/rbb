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
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountAuthenticator;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "SyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new SyncAdapter(this).getSyncAdapterBinder();
        }
    }

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            AccountManager manager = AccountManager.get(getContext());
            String cookie = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_COOKIE, true);
            String modhash = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_MODHASH, true);

            if (cookie == null || modhash == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            SubredditSyncAdapter.sync(getContext(), account, cookie, provider,
                    syncResult);
            CommentSyncAdapter.sync(getContext(), account, cookie, modhash, authority, provider,
                    syncResult);
            VoteSyncAdapter.sync(getContext(), account, cookie, modhash, authority, provider,
                    syncResult);

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
}
