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

package com.btmura.android.reddit.provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

public class Things implements BaseColumns, SyncColumns {
    static final String TABLE_NAME = "things";
    public static final Uri CONTENT_URI = Uri.parse(ThingProvider.BASE_AUTHORITY_URI);

    /** Account for joining with the votes table. */
    public static final String COLUMN_ACCOUNT = Votes.COLUMN_ACCOUNT;

    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_DOMAIN = "domain";
    public static final String COLUMN_DOWNS = "downs";
    public static final String COLUMN_LIKES = "likes";
    public static final String COLUMN_NUM_COMMENTS = "numComments";
    public static final String COLUMN_OVER_18 = "over18";
    public static final String COLUMN_PARENT = "parent";
    public static final String COLUMN_PERMA_LINK = "permaLink";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_SELF = "self";
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_THING_ID = Votes.COLUMN_THING_ID;
    public static final String COLUMN_THUMBNAIL_URL = "thumbnailUrl";
    public static final String COLUMN_UPS = "ups";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_VOTE = Votes.COLUMN_VOTE;

    public static final String SELECTION_BY_PARENT = COLUMN_PARENT + " = ?";
    public static final String SELECTION_BY_THING_ID = COLUMN_THING_ID + " = ?";

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT NOT NULL, "
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_DOMAIN + " TEXT NOT NULL, "
                + COLUMN_DOWNS + " INTEGER DEFAULT 0, "
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
                + COLUMN_PARENT + " TEXT NOT NULL, "
                + COLUMN_PERMA_LINK + " TEXT NOT NULL, "
                + COLUMN_SCORE + " INTEGER DEFAULT 0, "
                + COLUMN_SELF + " INTEGER NOT NULL, "
                + COLUMN_SUBREDDIT + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_THUMBNAIL_URL + " TEXT, "
                + COLUMN_TYPE + " INTEGER DEFAULT 0, "
                + COLUMN_UPS + " INTEGER DEFAULT 0, "
                + COLUMN_URL + " TEXT)");
    }

}
