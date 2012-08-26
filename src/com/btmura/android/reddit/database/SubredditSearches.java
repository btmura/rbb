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

package com.btmura.android.reddit.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Table for showing subreddit search results.
 */
public class SubredditSearches implements BaseColumns {

    public static final String TABLE_NAME = "subredditSearches";

    /** Account that searched for subreddits. */
    public static final String COLUMN_ACCOUNT = Votes.COLUMN_ACCOUNT;

    /** Subreddit name like AskReddit. */
    public static final String COLUMN_NAME = "name";

    /** Session id for this result set. */
    public static final String COLUMN_SESSION_ID = "sessionId";

    /** Number of subscribers to this subreddit. */
    public static final String COLUMN_SUBSCRIBERS = "subscribers";

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_SESSION_ID + " TEXT NOT NULL, "
                + COLUMN_SUBSCRIBERS + " INTEGER DEFAULT 0)");
    }
}
