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

import android.content.Context;
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
                Subreddits.createSubredditsV2(db);
                Things.createTable(db);
                Comments.createTable(db);
                Replies.createTable(db);
                Votes.createTable(db);
                SubredditSearches.createTable(db);
            } else {
                Subreddits.createSubredditsV1(db);
            }
            Subreddits.insertDefaultSubreddits(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.beginTransaction();
            try {
                Subreddits.upgradeSubredditsV2(db);
                Things.createTable(db);
                Comments.createTable(db);
                Replies.createTable(db);
                Votes.createTable(db);
                SubredditSearches.createTable(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}