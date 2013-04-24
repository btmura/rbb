/*
 * Copyright (C) 2013 Brian Muramatsu
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

/** Table for holding comments on things. */
public class Comments implements BaseColumns {

    public static final String TABLE_NAME = "comments";

    /** Column with the account name of the user viewing the comments. */
    public static final String COLUMN_ACCOUNT = SharedColumns.COLUMN_ACCOUNT;

    /** Column with the author of the comment. */
    public static final String COLUMN_AUTHOR = "author";

    /** Column with the body of a comment. Empty for the header comment. */
    public static final String COLUMN_BODY = "body";

    /** Column with the {@link CommentActions} row ID for a pending comment. */
    public static final String COLUMN_COMMENT_ACTION_ID = "commentActionId";

    /** Column with the creation time in UTC seconds. */
    public static final String COLUMN_CREATED_UTC = "createdUtc";

    /** Column indicating whether or not this comment is expanded. */
    public static final String COLUMN_EXPANDED = "expanded";

    /** Column with the kind of thing. */
    public static final String COLUMN_KIND = Kinds.COLUMN_KIND;

    /** Column indicating whether or not the requesting user likes the comment. */
    public static final String COLUMN_LIKES = "likes";

    /** Column with the nesting level starting from 0. */
    public static final String COLUMN_NESTING = "nesting";

    /** Column with the number of comments. Only for the header comment. */
    public static final String COLUMN_NUM_COMMENTS = "numComments";

    /** Column with the perma link. */
    public static final String COLUMN_PERMA_LINK = "permaLink";

    /** Column with the score of the comment. */
    public static final String COLUMN_SCORE = "score";

    /** Column with the self text of the comment. Only for header comments. */
    public static final String COLUMN_SELF_TEXT = "selfText";

    /** Column with the sequence used to order existing and pending comments. */
    public static final String COLUMN_SEQUENCE = "sequence";

    /** Column with the session ID that groups comments of a thing together. */
    public static final String COLUMN_SESSION_ID = SharedColumns.COLUMN_SESSION_ID;

    /** Column with the title. Only for the header comment. */
    public static final String COLUMN_TITLE = "title";

    /** Column with the thing ID of the comment. */
    public static final String COLUMN_THING_ID = SharedColumns.COLUMN_THING_ID;

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_COMMENT_ACTION_ID + " INTEGER,"
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_EXPANDED + " INTEGER DEFAULT 1, "
                + COLUMN_KIND + " INTEGER, "
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_NESTING + " INTEGER, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_PERMA_LINK + " TEXT, "
                + COLUMN_SCORE + " INTEGER DEFAULT 0, "
                + COLUMN_SELF_TEXT + " TEXT DEFAULT '', "
                + COLUMN_SEQUENCE + " INTEGER, "
                + COLUMN_SESSION_ID + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT, "
                + COLUMN_TITLE + " TEXT)");
    }
}
