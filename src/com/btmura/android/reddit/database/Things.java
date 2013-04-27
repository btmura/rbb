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

public class Things implements BaseColumns {

    public static final String TABLE_NAME = "things";

    /** Account for joining with the votes table. */
    public static final String COLUMN_ACCOUNT = SharedColumns.COLUMN_ACCOUNT;

    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_COMMENT_ACTION_ID = "commentActionId";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_DOMAIN = "domain";
    public static final String COLUMN_DOWNS = "downs";

    /** Column to indicate whether this comment is expanded. */
    @Deprecated
    // This column is not available after splitting things and comments in database V3.
    public static final String COLUMN_EXPANDED = "expanded";

    public static final String COLUMN_KIND = Kinds.COLUMN_KIND;
    public static final String COLUMN_LIKES = "likes";
    public static final String COLUMN_LINK_ID = "linkId";
    public static final String COLUMN_LINK_TITLE = "linkTitle";

    @Deprecated
    // This column is not available after splitting things and comments in database V3.
    public static final String COLUMN_NESTING = "nesting";

    public static final String COLUMN_NUM_COMMENTS = "numComments";
    public static final String COLUMN_OVER_18 = "over18";
    public static final String COLUMN_PERMA_LINK = "permaLink";
    public static final String COLUMN_SAVED = "saved";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_SELF = "self";
    public static final String COLUMN_SELF_TEXT = "selfText";

    @Deprecated
    // This column is not available after splitting things and comments in database V3.
    public static final String COLUMN_SEQUENCE = "sequence";

    public static final String COLUMN_SESSION_ID = SharedColumns.COLUMN_SESSION_ID;
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_THING_ID = SharedColumns.COLUMN_THING_ID;
    public static final String COLUMN_THUMBNAIL_URL = "thumbnailUrl";
    public static final String COLUMN_UPS = "ups";
    public static final String COLUMN_URL = "url";

    /**
     * Column to indicate whether this comment is visible. A comment whose parent is collapsed will
     * have expanded to true but visible to false.
     */
    @Deprecated
    // This column is not available after splitting things and comments in database V3.
    public static final String COLUMN_VISIBLE = "visible";

    /** Deleted comments have an author and body with this string. */
    public static final String DELETED = "[deleted]";

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

    public static final String SELECT_BY_ACCOUNT_AND_THING_ID =
            SELECT_BY_ACCOUNT + " AND " + COLUMN_THING_ID + "=?";

    public static final String SELECT_BY_COMMENT_ACTION_ID = COLUMN_COMMENT_ACTION_ID + "=?";

    public static final String SELECT_BY_SESSION_ID = SharedColumns.SELECT_BY_SESSION_ID;

    public static final String SELECT_BY_SESSION_ID_AND_THING_ID =
            SELECT_BY_SESSION_ID + " AND " + COLUMN_THING_ID + "=?";

    // TODO: Do we need an index for sessionId and more?
    public static final String SELECT_BY_SESSION_ID_AND_MORE =
            SELECT_BY_SESSION_ID + " AND " + COLUMN_KIND + " = " + Kinds.KIND_MORE;

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_COMMENT_ACTION_ID + " INTEGER,"
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_DOMAIN + " TEXT, "
                + COLUMN_DOWNS + " INTEGER DEFAULT 0, "
                + COLUMN_KIND + " INTEGER, "
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_LINK_ID + " TEXT, "
                + COLUMN_LINK_TITLE + " TEXT, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
                + COLUMN_PERMA_LINK + " TEXT, "
                + COLUMN_SAVED + " INTEGER DEFAULT 0, "
                + COLUMN_SCORE + " INTEGER DEFAULT 0, "
                + COLUMN_SELF + " INTEGER DEFAULT 0, "
                + COLUMN_SELF_TEXT + " TEXT DEFAULT '', "
                + COLUMN_SESSION_ID + " TEXT NOT NULL, "
                + COLUMN_SUBREDDIT + " TEXT, "
                + COLUMN_THING_ID + " TEXT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_THUMBNAIL_URL + " TEXT, "
                + COLUMN_UPS + " INTEGER DEFAULT 0, "
                + COLUMN_URL + " TEXT)");
    }

    /** Creates the temporary table used in version 2. Kept for testing upgrades. */
    static void createTempTableV2(SQLiteDatabase db) {
        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_COMMENT_ACTION_ID + " INTEGER,"
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_DOMAIN + " TEXT, "
                + COLUMN_DOWNS + " INTEGER DEFAULT 0, "
                + COLUMN_EXPANDED + " INTEGER DEFAULT 1, "
                + COLUMN_KIND + " INTEGER, "
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_LINK_ID + " TEXT, "
                + COLUMN_LINK_TITLE + " TEXT, "
                + COLUMN_NESTING + " INTEGER, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
                + COLUMN_PERMA_LINK + " TEXT, "
                + COLUMN_SAVED + " INTEGER DEFAULT 0, "
                + COLUMN_SCORE + " INTEGER DEFAULT 0, "
                + COLUMN_SELF + " INTEGER DEFAULT 0, "
                + COLUMN_SELF_TEXT + " TEXT DEFAULT '', "
                + COLUMN_SEQUENCE + " INTEGER, "
                + COLUMN_SESSION_ID + " TEXT NOT NULL, "
                + COLUMN_SUBREDDIT + " TEXT, "
                + COLUMN_THING_ID + " TEXT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_THUMBNAIL_URL + " TEXT, "
                + COLUMN_UPS + " INTEGER DEFAULT 0, "
                + COLUMN_URL + " TEXT,"
                + COLUMN_VISIBLE + " INTEGER DEFAULT 1)");
    }
}
