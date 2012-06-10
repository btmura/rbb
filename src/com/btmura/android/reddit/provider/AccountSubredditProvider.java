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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.AccountSubreddits;
import com.btmura.android.reddit.provider.Provider.Accounts;

class AccountSubredditProvider {

    public static String TAG = "AccountSubredditProvider";

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH
    };

    private static final int INDEX_COOKIE = 0;
    private static final int INDEX_MODHASH = 1;

    private static final String[] ACCOUNT_SUBREDDITS_PROJECTION = new String[] {
            AccountSubreddits.COLUMN_NAME,
            AccountSubreddits.COLUMN_TYPE,
            AccountSubreddits.COLUMN_CREATION_TIME,
    };

    private static final int INDEX_NAME = 0;
    private static final int INDEX_TYPE = 1;

    static Cursor query(SQLiteDatabase db, long accountId) {
        String[] credentials = getCredentials(db, accountId);

        // 1. Ask reddit.com what subreddits the account is subscribed to.
        ArrayList<Subreddit> subreddits = null;
        try {
            subreddits = NetApi.query(credentials[INDEX_COOKIE]);
        } catch (IOException e) {
            Log.e(TAG, "query", e);
            return null;
        }

        // 2. Query the database for local inserts and deletes.
        Cursor c = db.query(AccountSubreddits.TABLE_NAME,
                ACCOUNT_SUBREDDITS_PROJECTION,
                AccountSubreddits.ACCOUNT_ID_SELECTION,
                new String[] {Long.toString(accountId)},
                null,
                null,
                null);

        // 3. Add local inserts and remove local deletes.
        while (c.moveToNext()) {
            String name = c.getString(INDEX_NAME);
            switch (c.getInt(INDEX_TYPE)) {
                case AccountSubreddits.TYPE_INSERT:
                    insertSubreddit(subreddits, name);
                    break;

                case AccountSubreddits.TYPE_DELETE:
                    deleteSubreddit(subreddits, name);
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        c.close();

        return new SubredditCursor(subreddits);
    }

    private static void insertSubreddit(ArrayList<Subreddit> subreddits, String name) {
        int count = subreddits.size();
        for (int i = 0; i < count; i++) {
            String subreddit = subreddits.get(i).name;
            if (name.equalsIgnoreCase(subreddit)) {
                return;
            }
        }
        subreddits.add(Subreddit.newInstance(name));
    }

    private static void deleteSubreddit(ArrayList<Subreddit> subreddits, String name) {
        int count = subreddits.size();
        for (int i = 0; i < count; i++) {
            String subreddit = subreddits.get(i).name;
            if (name.equalsIgnoreCase(subreddit)) {
                subreddits.remove(i);
                return;
            }
        }
    }

    static int insert(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        ContentValues values = new ContentValues(4);
        values.put(AccountSubreddits.COLUMN_ACCOUNT_ID, accountId);
        values.put(AccountSubreddits.COLUMN_NAME, subreddit);
        values.put(AccountSubreddits.COLUMN_TYPE, AccountSubreddits.TYPE_INSERT);
        values.put(AccountSubreddits.COLUMN_CREATION_TIME, System.currentTimeMillis());
        db.insert(AccountSubreddits.TABLE_NAME, null, values);

        queueSubscription(context, db, accountId, subreddit, true);

        return 0;
    }

    static int delete(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        ContentValues values = new ContentValues(4);
        values.put(AccountSubreddits.COLUMN_ACCOUNT_ID, accountId);
        values.put(AccountSubreddits.COLUMN_NAME, subreddit);
        values.put(AccountSubreddits.COLUMN_TYPE, AccountSubreddits.TYPE_DELETE);
        values.put(AccountSubreddits.COLUMN_CREATION_TIME, System.currentTimeMillis());
        db.insert(AccountSubreddits.TABLE_NAME, null, values);

        queueSubscription(context, db, accountId, subreddit, false);

        return 1;
    }

    private static void queueSubscription(Context context, SQLiteDatabase db, long accountId,
            String subreddit, boolean subscribe) {
        String[] credentials = getCredentials(db, accountId);
        Intent intent = new Intent(context, SyncService.class);
        intent.putExtra(SyncService.EXTRA_COOKIE, credentials[INDEX_COOKIE]);
        intent.putExtra(SyncService.EXTRA_MODHASH, credentials[INDEX_MODHASH]);
        intent.putExtra(SyncService.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(SyncService.EXTRA_SUBSCRIBE, subscribe);
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
