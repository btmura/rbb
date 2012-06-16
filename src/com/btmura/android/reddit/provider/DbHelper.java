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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.btmura.android.reddit.provider.Provider.Subreddits;

class DbHelper extends SQLiteOpenHelper {

    public DbHelper(Context context) {
        super(context, "reddit", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            createSubreddits(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void createSubreddits(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Subreddits.TABLE_NAME + " ("
                + Subreddits._ID + " INTEGER PRIMARY KEY, "
                + Subreddits.COLUMN_NAME + " TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX " + Subreddits.COLUMN_NAME
                + " ON " + Subreddits.TABLE_NAME + " ("
                + Subreddits.COLUMN_NAME + " ASC)");

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
        if (newVersion == 2) {
            db.beginTransaction();
            try {
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}