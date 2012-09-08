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

public class Votes implements BaseColumns {
    public static final String TABLE_NAME = "votes";

    /** Account that liked this thing. */
    public static final String COLUMN_ACCOUNT = "account";

    public static final String COLUMN_THING_ID = "thingId";
    public static final String COLUMN_VOTE = "vote";

    public static final int VOTE_UP = 1;
    public static final int VOTE_DOWN = -1;

    public static final String SELECTION_BY_ACCOUNT = COLUMN_ACCOUNT + " = ?";

    public static final String SELECTION_BY_ACCOUNT_AND_THING_ID =
            SELECTION_BY_ACCOUNT + " AND " + COLUMN_THING_ID + " = ?";

    static void createTable(SQLiteDatabase db) {
        // TODO: Add unique constraint for account + thingId
        // TODO: Add index for account + thingId ?
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_VOTE + " INTEGER NOT NULL)");
    }
}
