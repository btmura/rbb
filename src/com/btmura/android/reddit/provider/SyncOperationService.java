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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.provider.SubredditProvider.Subreddits;

public class SyncOperationService extends IntentService {

    public static final String TAG = "SyncOperationService";

    private static final String[] PROJECTION = {
            Subreddits.COLUMN_ACCOUNT,
            Subreddits.COLUMN_NAME,
            Subreddits.COLUMN_STATE,
    };

    private static final int EXPIRATION_PADDING = 5 * 60 * 1000; // 5 minutes

    public SyncOperationService() {
        super("SyncOperationServiceWorker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Debug.DEBUG_SYNC) {
            Log.d(TAG, "onHandleIntent uri: " + intent.getDataString());
        }

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(intent.getData(), PROJECTION, null, null, null);
        try {
            while (c.moveToNext()) {
                String accountName = c.getString(0);
                String subreddit = c.getString(1);
                int state = c.getInt(2);

                if (Debug.DEBUG_SYNC) {
                    Log.d(TAG, "account name: " + accountName + " subreddit: " + subreddit
                            + " state: " + state);
                }

                AccountManager manager = AccountManager.get(this);
                Account account = new Account(accountName, getString(R.string.account_type));
                String cookie = manager.blockingGetAuthToken(account,
                        AccountAuthenticator.AUTH_TOKEN_COOKIE,
                        true);
                String modhash = manager.blockingGetAuthToken(account,
                        AccountAuthenticator.AUTH_TOKEN_MODHASH,
                        true);

                boolean subscribe = state == Subreddits.STATE_INSERTING;
                NetApi.subscribe(cookie, modhash, subreddit, subscribe);

                ContentValues values = new ContentValues(1);
                values.put(Subreddits.COLUMN_EXPIRATION, System.currentTimeMillis()
                        + EXPIRATION_PADDING);
                int count = cr.update(intent.getData(), values, null, null);
                if (Debug.DEBUG_SYNC) {
                    Log.d(TAG, "updated: " + count);
                }
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, "onHandleIntent", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "onHandleIntent", e);
        } catch (IOException e) {
            Log.e(TAG, "onHandleIntent", e);
        } finally {
            c.close();
        }
    }
}