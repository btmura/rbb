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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.Accounts;

class AccountSubredditProvider {

    public static String TAG = "AccountSubredditProvider";

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH};

    private static final int INDEX_COOKIE = 0;
    private static final int INDEX_MODHASH = 1;

    private static final AccountSubredditCache CACHE = new AccountSubredditCache();

    static Cursor query(SQLiteDatabase db, long accountId) {
        String[] credentials = getCredentials(db, accountId);
        ArrayList<Subreddit> subreddits = CACHE.query(accountId, credentials[0]);
        return new SubredditCursor(subreddits);
    }

    static int insert(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        if (CACHE.insert(context, accountId, subreddit)) {
            queueSubscription(context, db, accountId, subreddit, true);
        }
        return 0;
    }

    static int delete(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        if (CACHE.delete(accountId, subreddit)) {
            queueSubscription(context, db, accountId, subreddit, false);
        }
        return 1;
    }

    private static void queueSubscription(Context context, SQLiteDatabase db, long accountId,
            String subreddit, boolean subscribe) {
        String[] credentials = getCredentials(db, accountId);
        Intent intent = new Intent(context, AccountSubredditService.class);
        intent.putExtra(AccountSubredditService.EXTRA_COOKIE, credentials[INDEX_COOKIE]);
        intent.putExtra(AccountSubredditService.EXTRA_MODHASH, credentials[INDEX_MODHASH]);
        intent.putExtra(AccountSubredditService.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(AccountSubredditService.EXTRA_SUBSCRIBE, subscribe);
        context.startService(intent);
    }

    static String[] getCredentials(SQLiteDatabase db, long id) {
        String[] credentials = {null, null};
        String[] selectionArgs = new String[] {Long.toString(id)};
        Cursor c = db.query(Accounts.TABLE_NAME,
                CREDENTIALS_PROJECTION,
                Provider.ID_SELECTION,
                selectionArgs,
                null,
                null,
                null);
        try {
            if (c.moveToNext()) {
                credentials[INDEX_COOKIE] = c.getString(INDEX_COOKIE);
                credentials[INDEX_MODHASH] = c.getString(INDEX_MODHASH);
            }
        } finally {
            c.close();
        }
        return credentials;
    }
}
