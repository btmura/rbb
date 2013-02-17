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

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.util.Array;

public class Subreddits implements BaseColumns {
    public static final String TABLE_NAME = "subreddits";

    public static final String COLUMN_ACCOUNT = "account";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_EXPIRATION = "expiration";

    public static final String ACCOUNT_NONE = "";

    public static final String NAME_FRONT_PAGE = "";
    public static final String NAME_ALL = "all";
    public static final String NAME_RANDOM = "random";

    public static final int STATE_NORMAL = 0;
    public static final int STATE_INSERTING = 1;
    public static final int STATE_DELETING = 2;

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;
    public static final String SELECT_BY_ACCOUNT_NOT_DELETED =
            SELECT_BY_ACCOUNT + " AND " + COLUMN_STATE + "!= " + STATE_DELETING;
    public static final String SELECT_BY_ACCOUNT_AND_NAME =
            SELECT_BY_ACCOUNT + " AND " + COLUMN_NAME + "= ?";
    public static final String SELECT_NOT_DELETED_BY_ACCOUNT_AND_LIKE_NAME =
            SELECT_BY_ACCOUNT_NOT_DELETED + " AND " + COLUMN_NAME + " LIKE ?";

    public static final String SORT_BY_NAME = COLUMN_NAME + " COLLATE NOCASE ASC";

    public static boolean isFrontPage(String subreddit) {
        return TextUtils.isEmpty(subreddit);
    }

    public static boolean isAll(String subreddit) {
        return NAME_ALL.equalsIgnoreCase(subreddit);
    }

    public static boolean isRandom(String subreddit) {
        return NAME_RANDOM.equalsIgnoreCase(subreddit);
    }

    // TODO: Remove duplicate logic with hasSidebar.
    public static boolean isSyncable(String subreddit) {
        return !isFrontPage(subreddit)
                && !NAME_ALL.equalsIgnoreCase(subreddit)
                && !NAME_RANDOM.equalsIgnoreCase(subreddit);
    }

    public static boolean hasSidebar(String subreddit) {
        return subreddit != null && !isFrontPage(subreddit) && !isAll(subreddit)
                && !isRandom(subreddit);
    }

    public static String getTitle(Context c, String subreddit) {
        return isFrontPage(subreddit) ? c.getString(R.string.front_page) : subreddit;
    }

    static void createSubredditsV2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT DEFAULT '',"
                + COLUMN_NAME + " TEXT NOT NULL,"
                + COLUMN_STATE + " INTEGER DEFAULT 0,"
                + COLUMN_EXPIRATION + " INTEGER DEFAULT 0,"
                + "UNIQUE (" + COLUMN_ACCOUNT + "," + COLUMN_NAME + "))");
    }

    static void createSubredditsV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX " + COLUMN_NAME
                + " ON " + TABLE_NAME + " ("
                + COLUMN_NAME + " ASC)");
    }

    static void insertDefaultSubreddits(SQLiteDatabase db) {
        String[] defaultSubreddits = {
                "AdviceAnimals",
                "announcements",
                "AskReddit",
                "askscience",
                "atheism",
                "aww",
                "blog",
                "funny",
                "gaming",
                "IAmA",
                "movies",
                "Music",
                "pics",
                "politics",
                "science",
                "technology",
                "todayilearned",
                "videos",
                "worldnews",
                "WTF",};

        for (int i = 0; i < defaultSubreddits.length; i++) {
            ContentValues values = new ContentValues(1);
            values.put(COLUMN_NAME, defaultSubreddits[i]);
            db.insert(TABLE_NAME, null, values);
        }
    }

    static void upgradeSubredditsV2(SQLiteDatabase db) {
        // 1. Back up the old subreddit rows into ContentValues.
        ArrayList<ContentValues> rows = getSubredditNames(db);

        // 2. Drop the old table and index.
        db.execSQL("DROP INDEX " + COLUMN_NAME);
        db.execSQL("DROP TABLE " + TABLE_NAME);

        // 3. Create the new table and import the backed up subreddits.
        createSubredditsV2(db);
        int count = rows.size();
        for (int i = 0; i < count; i++) {
            db.insert(TABLE_NAME, null, rows.get(i));
        }
    }

    private static ArrayList<ContentValues> getSubredditNames(SQLiteDatabase db) {
        Cursor c = db.query(TABLE_NAME, Array.of(COLUMN_NAME),
                null, null, null, null, null);
        ArrayList<ContentValues> rows = new ArrayList<ContentValues>(c.getCount());
        while (c.moveToNext()) {
            String subreddit = c.getString(0);
            if (Subreddits.isSyncable(subreddit)) {
                ContentValues values = new ContentValues(1);
                values.put(COLUMN_NAME, subreddit);
                rows.add(values);
            }
        }
        c.close();
        return rows;
    }
}