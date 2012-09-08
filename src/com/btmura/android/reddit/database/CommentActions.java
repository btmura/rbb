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
 * Table for storing pending actions like adding or deleting comments. The
 * actions will be processed one by one in the background.
 */
public class CommentActions implements BaseColumns {

    public static final String TABLE_NAME = "commentActions";

    /** Account that created or deleted this comment. */
    public static final String COLUMN_ACCOUNT = Votes.COLUMN_ACCOUNT;

    /**
     * ID of the thing that is the parent of the thing we are commenting on. It
     * could be the same as the thing we are replying to. This is used to merge
     * pending replies with the current comments.
     */
    public static final String COLUMN_PARENT_THING_ID = "parentThingId";

    /** ID of the thing that we are commenting on. */
    public static final String COLUMN_THING_ID = "thingId";

    /** Text of the reply. */
    public static final String COLUMN_TEXT = "text";

    public static final String SELECTION_BY_ACCOUNT = COLUMN_ACCOUNT + " = ?";

    public static final String SELECTION_BY_ACCOUNT_AND_PARENT_THING_ID =
            SELECTION_BY_ACCOUNT + " AND " + COLUMN_PARENT_THING_ID + " = ?";

    public static final String SORT_BY_ID = _ID + " ASC";

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_PARENT_THING_ID + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_TEXT + " TEXT NOT NULL)");
    }
}
