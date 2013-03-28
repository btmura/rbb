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

    static final String DATABASE_REDDIT = "reddit";
    static final String DATABASE_TEST = "test";
    static final int LATEST_VERSION = 3;

    /** Singleton instances accessible via {@link #getInstance(Context)}. */
    private static DbHelper INSTANCE;

    /**
     * Return singleton instance of {@link DbHelper} that all users should use to avoid database
     * locked errors. Make sure to do database writes in serial though.
     */
    public static DbHelper getInstance(Context context) {
        synchronized (DbHelper.class) {
            if (INSTANCE == null) {
                INSTANCE = new DbHelper(context.getApplicationContext(),
                        DATABASE_REDDIT, LATEST_VERSION);
            }
            return INSTANCE;
        }
    }

    /** Version kept to control what tables are created mostly for testing. */
    private final int version;

    /** Test constructor. Use {@link #getInstance(Context, String, int)}. */
    DbHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.version = version;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if (!db.isReadOnly() && version == 2) {
            Sessions.createTempTableV2(db);
            Things.createTempTableV2(db);
            Messages.createTempTableV2(db);
            SubredditResults.createTempTableV2(db);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        switch (version) {
            case 3:
                // Version 3 converts the temporary tables of V2 into permanent tables.
                Subreddits.createSubredditsV2(db);
                Subreddits.insertDefaultSubreddits(db);
                createNewTablesV2(db);
                createNewTablesV3(db);
                break;

            case 2:
                // Version 2 upgrades the subreddits table and creates a bunch of other tables to
                // support accounts. It also uses temporary tables to store data that is difficult
                // to clean up.
                Subreddits.createSubredditsV2(db);
                Subreddits.insertDefaultSubreddits(db);
                createNewTablesV2(db);
                break;

            case 1:
                // Version 1 creates a table to support subreddits without any account support.
                Subreddits.createSubredditsV1(db);
                Subreddits.insertDefaultSubreddits(db);
                break;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            Subreddits.upgradeSubredditsV2(db);
            createNewTablesV2(db);
        }

        if (oldVersion >= 1 && newVersion == 3) {
            createNewTablesV3(db);
        }
    }

    /** Creates new tables introduced in version 3. */
    private static void createNewTablesV3(SQLiteDatabase db) {
        Sessions.createTable(db);
        Things.createTable(db);
        Messages.createTable(db);
        SubredditResults.createTable(db);
    }

    /** Creates new tables introduced in version 2. */
    private static void createNewTablesV2(SQLiteDatabase db) {
        Accounts.createTable(db);
        CommentActions.createTable(db);
        MessageActions.createTable(db);
        ReadActions.createTable(db);
        SaveActions.createTable(db);
        VoteActions.createTable(db);
    }
}