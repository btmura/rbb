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
 * {@link SaveActions} is a table that stores pending saves and unsaves before
 * they are synced back the server.
 */
public class SaveActions implements BaseColumns {

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

    // The following columns allow us to show pending saves to the user before
    // we have had a chance to sync them back to the server.

    /** String author name of the saved item. */
    public static final String COLUMN_AUTHOR = Things.COLUMN_AUTHOR;

    /** Long UTC creation time of the saved item. */
    public static final String COLUMN_CREATED_UTC = Things.COLUMN_CREATED_UTC;

    /** String domain of the thing. */
    public static final String COLUMN_DOMAIN = Things.COLUMN_DOMAIN;

    /** Integer number of downvotes. */
    public static final String COLUMN_DOWNS = Things.COLUMN_DOWNS;

    /** Integer either -1, 0, 1 to represent if the user liked it. */
    public static final String COLUMN_LIKES = Things.COLUMN_LIKES;

    /** Integer number of comments. */
    public static final String COLUMN_NUM_COMMENTS = Things.COLUMN_NUM_COMMENTS;

    /** Boolean indicating whether this is for over 18 folks. */
    public static final String COLUMN_OVER_18 = Things.COLUMN_OVER_18;

    /** String URL on reddit of the thing. */
    public static final String COLUMN_PERMA_LINK = Things.COLUMN_PERMA_LINK;

    /** Integer score of the thing. */
    public static final String COLUMN_SCORE = Things.COLUMN_SCORE;

    /** String subreddit name of the thing. */
    public static final String COLUMN_SUBREDDIT = Things.COLUMN_SUBREDDIT;

    /** String title of this thing. */
    public static final String COLUMN_TITLE = "title";

    /** String URL of the thumbnail. */
    public static final String COLUMN_THUMBNAIL_URL = Things.COLUMN_THUMBNAIL_URL;

    /** Integer amount of upvotes for this thing. */
    public static final String COLUMN_UPS = "ups";

    /** String URL of the thing. */
    public static final String COLUMN_URL = Things.COLUMN_URL;

    /** Action column value to save something. */
    public static final int ACTION_SAVE = 1;

    /** Action column value to unsave something. */
    public static final int ACTION_UNSAVE = -1;

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

    public static final String SELECT_UNSAVED_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT
            + " AND " + COLUMN_ACTION + "=" + ACTION_UNSAVE;

    public static final String SORT_BY_ID = SharedColumns.SORT_BY_ID;

    /** Creates the savedThings table. */
    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL,"
                + COLUMN_THING_ID + " TEXT NOT NULL,"
                + COLUMN_ACTION + " INTEGER NOT NULL,"
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0,"

                // The following columns are for storing enough information so
                // we can show the user we're going to save their item.
                + COLUMN_AUTHOR + " TEXT,"
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0,"
                + COLUMN_DOMAIN + " TEXT,"
                + COLUMN_DOWNS + " INTEGER DEFAULT 0,"
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0,"
                + COLUMN_OVER_18 + " INTEGER DEFAULT 0,"
                + COLUMN_PERMA_LINK + " TEXT,"
                + COLUMN_SCORE + " INTEGER DEFAULT 0,"
                + COLUMN_SUBREDDIT + " TEXT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_THUMBNAIL_URL + " TEXT,"
                + COLUMN_UPS + " INTEGER DEFAULT 0,"
                + COLUMN_URL + " TEXT,"

                // Add constraint to make it easy to replace actions.
                + "UNIQUE (" + COLUMN_ACCOUNT + "," + COLUMN_THING_ID + "))");
    }
}
