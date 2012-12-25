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

public class Sessions implements BaseColumns {

    public static final String TABLE_NAME = "sessions";

    /**
     * String key identifying the session. It means different things for
     * different listing types.
     */
    public static final String COLUMN_KEY = "key";

    /** Long timestamp when the session was created. */
    public static final String COLUMN_TIMESTAMP = "timestamp";

    /** Integer type of the session. */
    public static final String COLUMN_TYPE = "type";

    public static final int TYPE_MESSAGE_THREAD_LISTING = 0;
    public static final int TYPE_MESSAGE_INBOX_LISTING = 1;
    public static final int TYPE_MESSAGE_SENT_LISTING = 2;
    public static final int TYPE_SUBREDDIT_LISTING = 3;
    public static final int TYPE_USER_LISTING = 4;
    public static final int TYPE_COMMENT_LISTING = 5;
    public static final int TYPE_SEARCH_LISTING = 6;
    public static final int TYPE_REDDIT_SEARCH_LISTING = 7;
    public static final int TYPE_COMMENT_CONTEXT_LISTING = 8;

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_KEY + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
                + COLUMN_TYPE + " INTEGER DEFAULT 0)");
    }
}
