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
 * {@link SaveActions} is a table that stores pending saves and unsaves before they are synced back
 * the server. It implements {@link BaseThingColumns}, so that pending saves can be shown in the
 * user's saved tab.
 */
public class SaveActions implements BaseThingColumns, BaseColumns {

    /** Name of the table to store save and unsave actions. */
    public static final String TABLE_NAME = "saveActions";

    /** Account that either saved on unsaved this thing. */
    public static final String COLUMN_ACCOUNT = "account";

    /** Integer column indicating action whether to save or unsave. */
    public static final String COLUMN_ACTION = "action";

    /** Unused long column for expiration. */
    public static final String COLUMN_EXPIRATION = "expiration";

    /** String ID of the thing that the user wants to save or unsave. */
    public static final String COLUMN_THING_ID = "thingId";

    /** Action column value to save something. */
    public static final int ACTION_SAVE = 1;

    /** Action column value to unsave something. */
    public static final int ACTION_UNSAVE = -1;

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

    public static final String SELECT_UNSAVED_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT
            + " AND " + COLUMN_ACTION + "=" + ACTION_UNSAVE;

    public static final String SORT_BY_ID = SharedColumns.SORT_BY_ID;

    static void createTableV2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL,"
                + COLUMN_THING_ID + " TEXT NOT NULL,"
                + COLUMN_ACTION + " INTEGER NOT NULL,"
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0,"

                // Create the base columns to display pending save items in the listing.
                + CREATE_THING_COLUMNS_V2 + ","

                // Add constraint to make it easy to replace actions.
                + "UNIQUE (" + COLUMN_ACCOUNT + "," + COLUMN_THING_ID + "))");
    }

    static void upgradeTableV2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD " + COLUMN_SAVED + " INTEGER DEFAULT 0");
    }

    static void createTableV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL,"
                + COLUMN_THING_ID + " TEXT NOT NULL,"
                + COLUMN_ACTION + " INTEGER NOT NULL,"
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0,"

                // Create the base columns to display pending save items in the listing.
                + CREATE_THING_COLUMNS + ","

                // Add constraint to make it easy to replace actions.
                + "UNIQUE (" + COLUMN_ACCOUNT + "," + COLUMN_THING_ID + "))");
    }
}
