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

public class Messages implements BaseColumns, KindColumns {
    public static final String TABLE_NAME = "messages";

    /** Account for joining with the votes table. */
    public static final String COLUMN_ACCOUNT = Votes.COLUMN_ACCOUNT;

    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_CONTEXT = "context";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_KIND = "kind";
    public static final String COLUMN_NEW = "new";

    public static final String COLUMN_SESSION_ID = "sessionId";

    /**
     * Integer column indicating where this message came from. This is not from
     * reddit's JSON response. It is an column used internally to implement
     * displaying of sent messages.
     */
    public static final String COLUMN_SOURCE = "source";

    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_THING_ID = Votes.COLUMN_THING_ID;
    public static final String COLUMN_WAS_COMMENT = "wasComment";
    public static final String COLUMN_VOTE = Votes.COLUMN_VOTE;

    /** Message came from the inbox data from the server. */
    public static final int SOURCE_INBOX = 0;

    /** Message came from the sent data the server. */
    public static final int SOURCE_SENT = 1;

    public static final String SELECT_BY_SESSION_ID = COLUMN_SESSION_ID + "=?";

    public static final String SELECT_BY_ACCOUNT = COLUMN_ACCOUNT + "= ?";

    public static final String SELECT_BY_ACCOUNT_FROM_INBOX =
            COLUMN_SOURCE + "=" + SOURCE_INBOX + " AND " + SELECT_BY_ACCOUNT;

    public static final String SELECT_BY_ACCOUNT_FROM_SENT =
            COLUMN_SOURCE + "=" + SOURCE_SENT + " AND " + SELECT_BY_ACCOUNT;

    public static final String SELECT_NEW_BY_ACCOUNT_FROM_INBOX =
            COLUMN_NEW + "= 1 AND " + SELECT_BY_ACCOUNT_FROM_INBOX;

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_CONTEXT + " TEXT, "
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_KIND + " INTEGER NOT NULL, "
                + COLUMN_NEW + " INTEGER DEFAULT 0, "
                + COLUMN_SESSION_ID + " TEXT, "
                + COLUMN_SOURCE + " INTEGER DEFAULT 0, "
                + COLUMN_SUBREDDIT + " TEXT, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_WAS_COMMENT + " INTEGER DEFAULT 0)");
    }
}
