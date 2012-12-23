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
 * {@link Saves} is a table that stores pending saves and unsaves before they
 * are synced back the server.
 */
public class Saves implements BaseColumns {

    /** Name of the table to store save and unsave actions. */
    public static final String TABLE_NAME = "saves";

    /** Account that either saved on unsaved this thing. */
    public static final String COLUMN_ACCOUNT = "account";

    /** String ID of the thing that the user wants to save or unsave. */
    public static final String COLUMN_THING_ID = "thingId";

    /** Integer column indicating action whether to save or unsave. */
    public static final String COLUMN_ACTION = "action";

    /** Unused long column for expiration. */
    public static final String COLUMN_EXPIRATION = "expiration";

    /** Action column value to save something. */
    public static final int ACTION_SAVE = 0;

    /** Action column value to unsave something. */
    public static final int ACTION_UNSAVE = 1;

    /** Creates the savedThings table. */
    static void createTable(SQLiteDatabase db) {
        // TODO: Add unique constraint for account + thingId
        // TODO: Add index for account + thingId ?
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_ACTION + " INTEGER NOT NULL, "
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0)");
    }
}
