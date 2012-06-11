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

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.provider.Provider.Accounts;
import com.btmura.android.reddit.provider.Provider.SyncTasks;

public class SyncService extends IntentService {

    public static final String TAG = "SyncService";

    private static final boolean DEBUG_SERVICE = true;

    private static final String[] SYNC_TASK_PROJECTION = {
            SyncTasks.COLUMN_ACCOUNT_ID,
            SyncTasks.COLUMN_NAME,
            SyncTasks.COLUMN_TYPE,
    };

    private static final int INDEX_ACCOUNT_ID = 0;
    private static final int INDEX_NAME = 1;
    private static final int INDEX_TYPE = 2;

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH
    };

    static final int INDEX_COOKIE = 0;
    static final int INDEX_MODHASH = 1;

    public SyncService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (DEBUG_SERVICE) {
            Log.d(TAG, "Handling intent for " + intent.getDataString());
        }

        long accountId = 0;
        String subreddit = null;
        boolean subscribe = false;
        String cookie = null;
        String modhash = null;

        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(intent.getData(), SYNC_TASK_PROJECTION, null, null, null);
        try {
            if (c.moveToNext()) {
                accountId = c.getLong(INDEX_ACCOUNT_ID);
                subreddit = c.getString(INDEX_NAME);
                subscribe = c.getInt(INDEX_TYPE) == SyncTasks.TYPE_INSERT;
            } else {
                throw new IllegalStateException();
            }
        } finally {
            c.close();
        }

        c = cr.query(ContentUris.withAppendedId(Accounts.CONTENT_URI, accountId),
                CREDENTIALS_PROJECTION, null, null, null);
        try {
            if (c.moveToNext()) {
                cookie = c.getString(INDEX_COOKIE);
                modhash = c.getString(INDEX_MODHASH);
            } else {
                Log.w(TAG, "No credentials found. Account deleted?");
                return;
            }
        } finally {
            c.close();
        }

        try {
            NetApi.subscribe(cookie, modhash, subreddit, subscribe);
        } catch (IOException e) {
            Log.e(TAG, "handleSubscribe", e);
        }

        ContentValues values = new ContentValues(1);
        values.put(SyncTasks.COLUMN_EXPIRATION, System.currentTimeMillis());
        int count = cr.update(intent.getData(), values, null, null);
        if (DEBUG_SERVICE) {
            Log.d(TAG, "Updated expiration time for " + count + " tasks.");
        }

    }
}
