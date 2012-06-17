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

import java.io.IOException;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.SubredditProvider.Subreddits;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "SyncAdapter";

    public SyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        if (Debug.DEBUG_SYNC) {
            Log.d(TAG, "onPerformSync");
        }

        AccountManager manager = AccountManager.get(getContext());

        try {
            String cookie = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_COOKIE,
                    true);

            ArrayList<Subreddit> subreddits = NetApi.query(cookie);
            int count = subreddits.size();

            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(count);
            for (int i = 0; i < count; i++) {
                ops.add(ContentProviderOperation.newInsert(Subreddits.CONTENT_URI)
                        .withValue(Subreddits.COLUMN_ACCOUNT, account.name)
                        .withValue(Subreddits.COLUMN_NAME, subreddits.get(i).name)
                        .build());
            }
            provider.applyBatch(ops);

            if (Debug.DEBUG_SYNC) {
                Log.d(TAG, "Synced " + count + " subreddits.");
            }


        } catch (OperationCanceledException e) {
            Log.e(TAG, "onPerformSync", e);
            syncResult.stats.numAuthExceptions++;

        } catch (AuthenticatorException e) {
            Log.e(TAG, "onPerformSync", e);
            syncResult.stats.numAuthExceptions++;

        } catch (IOException e) {
            Log.e(TAG, "onPerformSync", e);
            syncResult.stats.numIoExceptions++;

        } catch (RemoteException e) {
            Log.e(TAG, "onPerformSync", e);
            syncResult.databaseError = true;

        } catch (OperationApplicationException e) {
            Log.e(TAG, "onPerformSync", e);
            syncResult.databaseError = true;

        }
    }
}
