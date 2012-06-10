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

/**
 * {@link AccountSubredditProvider} queries reddit.com for account info when
 * {@link Provider} asks for it.
 */
class AccountSubredditProvider {

    public static String TAG = "AccountSubredditProvider";

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH};

    private static final AccountSubredditCache CACHE = new AccountSubredditCache();

    static Cursor querySubreddits(SQLiteDatabase db, long accountId) {
        ArrayList<Subreddit> subreddits = CACHE.querySubreddits(db, accountId);
        return new SubredditCursor(subreddits);
    }

    static int insertSubreddit(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        CACHE.addSubreddit(context, accountId, subreddit);

        Intent intent = AccountSubredditService.getAddSubredditIntent(context, subreddit);
        addCredentials(intent, db, accountId);
        context.startService(intent);

        return 0;
    }

    static int deleteSubreddit(Context context, SQLiteDatabase db, long accountId, String subreddit) {
        CACHE.deleteSubreddit(accountId, subreddit);

        Intent intent = AccountSubredditService.getRemoveSubredditIntent(context, subreddit);
        addCredentials(intent, db, accountId);
        context.startService(intent);

        return 1;
    }

    private static Intent addCredentials(Intent intent, SQLiteDatabase db, long accountId) {
        String[] selectionArgs = new String[] {Long.toString(accountId)};
        Cursor c = db.query(Accounts.TABLE_NAME,
                CREDENTIALS_PROJECTION,
                Provider.ID_SELECTION,
                selectionArgs,
                null,
                null,
                null);
        try {
            if (c.moveToNext()) {
                intent.putExtra(AccountSubredditService.EXTRA_COOKIE, c.getString(0));
                intent.putExtra(AccountSubredditService.EXTRA_MODHASH, c.getString(1));
            }
        } finally {
            c.close();
        }
        return intent;
    }
}
