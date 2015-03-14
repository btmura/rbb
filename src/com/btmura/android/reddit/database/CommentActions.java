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
 * Table for storing pending actions like adding or deleting comments. The actions will be processed
 * one by one in the background.
 */
public class CommentActions implements BaseColumns {

    public static final String TABLE_NAME = "commentActions";

    /** Account that created or deleted this comment. */
    public static final String COLUMN_ACCOUNT = VoteActions.COLUMN_ACCOUNT;

    /** Action this row represents like adding or deleting. */
    public static final String COLUMN_ACTION = "action";

    /**
     * ID of the thing that is the parent of the thing we are commenting on. It could be the same as
     * the thing we are replying to. This is used to merge pending replies with the current
     * comments.
     */
    public static final String COLUMN_PARENT_THING_ID = "parentThingId";

    /** ID of the thing that we are commenting on or deleting. */
    public static final String COLUMN_THING_ID = "thingId";

    /** Text of the reply. */
    public static final String COLUMN_TEXT = "text";

    /** Unused long column with expiration. */
    public static final String COLUMN_EXPIRATION = "expiration";

    /** Number of sync failures. */
    public static final String COLUMN_SYNC_FAILURES = "syncFailures";

    /** Unused string column with sync status. */
    public static final String COLUMN_SYNC_STATUS = "syncStatus";

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

    public static final String SELECT_BY_PARENT_THING_ID = COLUMN_PARENT_THING_ID + " = ?";

    public static final String SORT_BY_ID = _ID + " ASC";

    /** Action meaning the user has responded to another comment. */
    public static final int ACTION_INSERT = 0;

    /** Action meaning the user has deleted one of their own comments. */
    public static final int ACTION_DELETE = 1;

    /** Action meaning teh user has edited a self post or comment. */
    public static final int ACTION_EDIT = 2;

    static void createV2(SQLiteDatabase db) {
        create(db);
        upgradeToV2(db);
    }

    static void upgradeToV2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_SYNC_FAILURES + " INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_SYNC_STATUS + " TEXT");
    }

    static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACTION + " INTEGER NOT NULL,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL,"
                + COLUMN_PARENT_THING_ID + " TEXT,"
                + COLUMN_THING_ID + " TEXT NOT NULL,"
                + COLUMN_TEXT + " TEXT,"
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0)");
    }
}
