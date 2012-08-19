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
import android.database.sqlite.SQLiteOpenHelper;


public class DbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_REDDIT = "reddit";
    public static final String DATABASE_TEST = "test";
    public static final int LATEST_VERSION = 2;

    /** Version kept to control what tables are created mostly for testing. */
    private final int version;

    public DbHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.version = version;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            if (version > 1) {
                createSubredditsV2(db);
                Comments.createTable(db);
                Replies.createTable(db);
                Things.createTable(db);
                Votes.createTable(db);
            } else {
                createSubredditsV1(db);
            }
            insertDefaultSubreddits(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void createSubredditsV2(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Subreddits.TABLE_NAME + " ("
                + Subreddits._ID + " INTEGER PRIMARY KEY, "
                + Subreddits.COLUMN_ACCOUNT + " TEXT DEFAULT '', "
                + Subreddits.COLUMN_NAME + " TEXT NOT NULL, "
                + Subreddits.COLUMN_STATE + " INTEGER DEFAULT 0, "
                + Subreddits.COLUMN_EXPIRATION + " INTEGER DEFAULT 0, "
                + "UNIQUE (" + Subreddits.COLUMN_ACCOUNT + "," + Subreddits.COLUMN_NAME + "))");
    }

    private void createSubredditsV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Subreddits.TABLE_NAME + " ("
                + Subreddits._ID + " INTEGER PRIMARY KEY, "
                + Subreddits.COLUMN_NAME + " TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX " + Subreddits.COLUMN_NAME
                + " ON " + Subreddits.TABLE_NAME + " ("
                + Subreddits.COLUMN_NAME + " ASC)");
    }

    private void insertDefaultSubreddits(SQLiteDatabase db) {
        String[] defaultSubreddits = {
                "",
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
            values.put(Subreddits.COLUMN_NAME, defaultSubreddits[i]);
            db.insert(Subreddits.TABLE_NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.beginTransaction();
            try {
                upgradeSubredditsV2(db);
                Comments.createTable(db);
                Replies.createTable(db);
                Things.createTable(db);
                Votes.createTable(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private void upgradeSubredditsV2(SQLiteDatabase db) {
        // 1. Back up the old subreddit rows into ContentValues.
        ArrayList<ContentValues> rows = getSubredditNames(db);

        // 2. Drop the old table and index.
        db.execSQL("DROP INDEX " + Subreddits.COLUMN_NAME);
        db.execSQL("DROP TABLE " + Subreddits.TABLE_NAME);

        // 3. Create the new table and import the backed up subreddits.
        createSubredditsV2(db);
        int count = rows.size();
        for (int i = 0; i < count; i++) {
            db.insert(Subreddits.TABLE_NAME, null, rows.get(i));
        }
    }

    private ArrayList<ContentValues> getSubredditNames(SQLiteDatabase db) {
        ArrayList<ContentValues> rows = new ArrayList<ContentValues>();
        Cursor c = db.query(Subreddits.TABLE_NAME,
                new String[] {Subreddits.COLUMN_NAME},
                null,
                null,
                null,
                null,
                null);
        while (c.moveToNext()) {
            ContentValues values = new ContentValues(1);
            values.put(Subreddits.COLUMN_NAME, c.getString(0));
            rows.add(values);
        }
        c.close();
        return rows;
    }
}