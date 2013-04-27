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
                createDatabaseV3(db);
                break;

            case 2:
                createDatabaseV2(db);
                break;

            case 1:
                createDatabaseV1(db);
                break;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrades are applied incrementally to get up to the latest version.
        if (needsUpgrade(oldVersion, newVersion, 2)) {
            upgradeDatabaseV2(db);
        }
        if (needsUpgrade(oldVersion, newVersion, 3)) {
            upgradeDatabaseV3(db);
        }
    }

    private static boolean needsUpgrade(int oldVersion, int newVersion, int upgrade) {
        return oldVersion < upgrade && newVersion >= upgrade;
    }

    /**
     * Creates the tables for database version 3. It converts the temporary tables of V2 into
     * permanent tables. It also adds new tables for comments and hiding things.
     */
    private static void createDatabaseV3(SQLiteDatabase db) {
        createNewTablesV3(db);
        createNewTablesV2(db, 2);
        createNewTablesV1(db, 2);
    }

    /** Upgrade database to version 3 from version 2. */
    private static void upgradeDatabaseV3(SQLiteDatabase db) {
        createNewTablesV3(db);
        SaveActions.upgradeTableV2(db);
        VoteActions.upgradeTableV2(db);
    }

    /** Creates the tables introduced in database version 3. */
    private static void createNewTablesV3(SQLiteDatabase db) {
        Comments.createTable(db);
        HideActions.createTable(db);
        Messages.createTable(db);
        Sessions.createTable(db);
        SubredditResults.createTable(db);
        Things.createTable(db);
    }

    /**
     * Creates the tables for database version 2. It creates a bunch of new tables to support
     * accounts and sync adapters. It also uses temporary tables to store data created in
     * {@link #onOpen(SQLiteDatabase)}.
     */
    private static void createDatabaseV2(SQLiteDatabase db) {
        createNewTablesV2(db, 1);
        createNewTablesV1(db, 2);
    }

    /** Upgrade database to version 2 from version 1. */
    private static void upgradeDatabaseV2(SQLiteDatabase db) {
        createNewTablesV2(db, 1);
        Subreddits.upgradeSubredditsV2(db);
    }

    /** Creates the tables introduced in database version 2. */
    private static void createNewTablesV2(SQLiteDatabase db, int version) {
        Accounts.createTable(db);
        CommentActions.createTable(db);
        MessageActions.createTable(db);
        ReadActions.createTable(db);
        switch (version) {
            case 2:
                SaveActions.createTableV2(db);
                VoteActions.createTableV2(db);
                break;

            case 1:
                SaveActions.createTableV1(db);
                VoteActions.createTableV1(db);
                break;
        }
    }

    /** Creates the tables for database version 1. It supports storing local subreddits. */
    private static void createDatabaseV1(SQLiteDatabase db) {
        createNewTablesV1(db, 1);
    }

    /** Creates the tables introduced in database version 1. */
    private static void createNewTablesV1(SQLiteDatabase db, int version) {
        switch (version) {
            case 2:
                Subreddits.createTableV2(db);
                Subreddits.insertDefaultSubreddits(db);
                break;

            case 1:
                Subreddits.createTableV1(db);
                Subreddits.insertDefaultSubreddits(db);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }
}