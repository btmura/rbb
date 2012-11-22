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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.MessageProvider;

public class MessageSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "MessageSyncAdapter";

    private static final int NUM_OPS = 3;
    private static final int OP_INSERTS = 0;
    private static final int OP_UPDATES = 1;
    private static final int OP_DELETES = 2;

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
            String cookie = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_COOKIE, true);

            if (cookie == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            ArrayList<ContentValues> values = RedditApi.getMessages(getContext(),
                    account.name, 0, cookie);

            int count = values.size();
            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(count);
            int[] opCounts = new int[NUM_OPS];
            for (int i = 0; i < count; i++) {
                ops.add(ContentProviderOperation.newInsert(MessageProvider.MESSAGES_URI)
                        .withValues(values.get(i))
                        .build());
                opCounts[OP_INSERTS]++;
            }

            getContext().getContentResolver().applyBatch(authority, ops);
            syncResult.stats.numInserts += opCounts[OP_INSERTS];
            syncResult.stats.numUpdates += opCounts[OP_UPDATES];
            syncResult.stats.numDeletes += opCounts[OP_DELETES];

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numIoExceptions++;
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        }
    }
}
