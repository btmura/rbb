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
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.database.Replies;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.ReplyProvider;
import com.btmura.android.reddit.util.Array;

/**
 * {@link AbstractThreadedSyncAdapter} that syncs pending replies to the server.
 */
public class ReplySyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "ReplySyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new ReplySyncAdapter(this).getSyncAdapterBinder();
        }
    }

    private static final String[] PROJECTION = {
            Replies._ID,
            Replies.COLUMN_THING_ID,
            Replies.COLUMN_TEXT,
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_THING_ID = 1;
    private static final int INDEX_TEXT = 2;

    public ReplySyncAdapter(Context context) {
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

            // Get all pending replies for this account that haven't been
            // synced.
            Cursor c = provider.query(ReplyProvider.CONTENT_URI, PROJECTION,
                    Replies.SELECTION_BY_ACCOUNT, Array.of(account.name), null);

            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(c.getCount());
            while (c.moveToNext()) {
                long id = c.getLong(INDEX_ID);
                String thingId = c.getString(INDEX_THING_ID);
                String text = c.getString(INDEX_TEXT);

                // Sync the reply with the server. If successful then schedule
                // deletion of the database row.
                try {
                    RedditApi.comment(thingId, text, cookie, modhash);
                    ops.add(ContentProviderOperation.newDelete(ReplyProvider.CONTENT_URI)
                            .withSelection(ReplyProvider.ID_SELECTION, Array.of(id))
                            .build());
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }
            c.close();

            // Now delete the rows from the database. The server shows the
            // updates immediately.
            ContentProviderResult[] results = provider.applyBatch(ops);
            int count = results.length;
            for (int i = 0; i < count; i++) {
                syncResult.stats.numDeletes += results[i].count;
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPerformSync syncResult: " + syncResult);
        }
    }
}
