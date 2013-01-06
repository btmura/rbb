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

public class Messages implements BaseColumns {
    public static final String TABLE_NAME = "messages";

    /** Account for joining with the votes table. */
    public static final String COLUMN_ACCOUNT = SharedColumns.COLUMN_ACCOUNT;

    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_CONTEXT = "context";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_KIND = "kind";
    public static final String COLUMN_MESSAGE_ACTION_ID = "messageActionId";
    public static final String COLUMN_NEW = "new";
    public static final String COLUMN_SESSION_ID = SharedColumns.COLUMN_SESSION_ID;
    public static final String COLUMN_SUBJECT = "subject";
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_THING_ID = SharedColumns.COLUMN_THING_ID;
    public static final String COLUMN_WAS_COMMENT = "wasComment";

    public static final String SELECT_BY_ACCOUNT_AND_THING_ID =
            COLUMN_ACCOUNT + "=? AND " + COLUMN_THING_ID + "=?";

    static void createTempTable(SQLiteDatabase db) {
        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL,"
                + COLUMN_AUTHOR + " TEXT,"
                + COLUMN_BODY + " TEXT,"
                + COLUMN_CONTEXT + " TEXT,"
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0,"
                + COLUMN_KIND + " INTEGER NOT NULL,"
                + COLUMN_MESSAGE_ACTION_ID + " INTEGER,"
                + COLUMN_NEW + " INTEGER DEFAULT 0,"
                + COLUMN_SESSION_ID + " TEXT,"
                + COLUMN_SUBJECT + " TEXT,"
                + COLUMN_SUBREDDIT + " TEXT,"
                + COLUMN_THING_ID + " TEXT,"
                + COLUMN_WAS_COMMENT + " INTEGER DEFAULT 0)");
    }
}
