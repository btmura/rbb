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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.btmura.android.reddit.entity.Subreddit;

/**
 * {@link AccountSubredditProvider} queries reddit.com for account info when
 * {@link Provider} asks for it.
 */
class AccountSubredditProvider {

    public static String TAG = "AccountSubredditProvider";

    private static final AccountSubredditCache CACHE = new AccountSubredditCache();

    static Cursor querySubreddits(SQLiteDatabase db, long accountId) {
        ArrayList<Subreddit> subreddits = CACHE.querySubreddits(db, accountId);        
        return new SubredditCursor(subreddits);
    }

    static int insertSubreddit(SQLiteDatabase db, long accountId, String subreddit) {
        try {
            AccountSubredditUtils.subscribe(db, accountId, subreddit, true);
            CACHE.addSubreddit(accountId, subreddit);            
            return 0;
        } catch (IOException e) {
            Log.e(TAG, "insertSubreddit", e);
        }
        return -1;
    }

    static int deleteSubreddit(SQLiteDatabase db, long accountId, String subreddit) {
        try {
            AccountSubredditUtils.subscribe(db, accountId, subreddit, false);
            CACHE.deleteSubreddit(accountId, subreddit);            
            return 1;
        } catch (IOException e) {
            Log.e(TAG, "deleteSubreddit", e);
        }
        return -1;
    }
}
