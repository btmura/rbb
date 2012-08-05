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

public class Comments implements BaseColumns {

    static final String TABLE_NAME = "comments";

    public static final Uri CONTENT_URI = Uri.parse(CommentProvider.BASE_AUTHORITY_URI);

    public static final String PARAM_SYNC = "sync";
    public static final String PARAM_ACCOUNT_NAME = "accountName";
    public static final String PARAM_THING_ID = "thingId";

    static final int NUM_COLUMNS = 13;
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_DOWNS = "downs";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_KIND = "kind";
    public static final String COLUMN_LIKES = "likes";
    public static final String COLUMN_NESTING = "nesting";
    public static final String COLUMN_NUM_COMMENTS = "numComments";
    public static final String COLUMN_PARENT_ID = "parentId";
    public static final String COLUMN_SELF_TEXT = "selfText";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_UPS = "ups";

    public static final int KIND_HEADER = 0;
    public static final int KIND_COMMENT = 1;
    public static final int KIND_MORE = 2;

    public static String PARENT_ID_SELECTION = COLUMN_PARENT_ID + " = ?";

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_AUTHOR + " TEXT NOT NULL, "
                + COLUMN_BODY + " TEXT DEFAULT '', "
                + COLUMN_CREATED_UTC + " INTEGER NOT NULL, "
                + COLUMN_DOWNS + " INTEGER NOT NULL, "
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_KIND + " INTEGER NOT NULL, "
                + COLUMN_LIKES + " INTEGER NOT NULL, "
                + COLUMN_NESTING + " INTEGER NOT NULL, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_PARENT_ID + " TEXT NOT NULL, "
                + COLUMN_SELF_TEXT + " TEXT DEFAULT '', "
                + COLUMN_TITLE + " TEXT DEFAULT '', "
                + COLUMN_UPS + " INTEGER NOT NULL)");
    }
}
