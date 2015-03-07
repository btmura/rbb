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
    public static final int LATEST_VERSION = 4;

    /**
     * Singleton instances accessible via {@link #getInstance(Context)}.
     */
    private static DbHelper INSTANCE;

    /**
     * Return singleton instance of {@link DbHelper} that all users should use to avoid database
     * locked errors. Make sure to do database writes in serial though.
     */
    public static DbHelper getInstance(Context context) {
        synchronized (DbHelper.class) {
            if (INSTANCE == null) {
                INSTANCE = new DbHelper(context.getApplicationContext(),
                        DATABASE_REDDIT,
                        LATEST_VERSION);
            }
            return INSTANCE;
        }
    }

    /**
     * Version kept to control what tables are created mostly for testing.
     */
    private final int version;

    /**
     * Test constructor. Use {@link #getInstance(Context, String, int)}.
     */
    public DbHelper(Context context, String name, int version) {
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
            case 4:
                createDatabaseV4(db);
                break;

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
            upgradeToDatabaseV2(db);
        }
        if (needsUpgrade(oldVersion, newVersion, 3)) {
            upgradeToDatabaseV3(db);
        }
        if (needsUpgrade(oldVersion, newVersion, 4)) {
            upgradeToDatabaseV4(db);
        }
    }

    private static boolean needsUpgrade(int oldVersion, int newVersion, int upgrade) {
        return oldVersion < upgrade && newVersion >= upgrade;
    }

    private static void createDatabaseV4(SQLiteDatabase db) {
        Accounts.createTable(db);
        Comments.createTable(db);
        Messages.createTable(db);
        Sessions.createTable(db);
        Subreddits.createTableV2(db);
        SubredditResults.createTable(db);
        Things.createTable(db);

        CommentActions.createTableV2(db);
        HideActions.createTableV2(db);
        MessageActions.createTableV2(db);
        ReadActions.createTableV2(db);

        SaveActions.createTableV3(db);
        VoteActions.createTableV3(db);

        Subreddits.insertDefaultSubreddits(db);
    }

    private static void upgradeToDatabaseV4(SQLiteDatabase db) {
        CommentActions.upgradeTableV2(db);
        HideActions.upgradeTableV2(db);
        MessageActions.upgradeTableV2(db);
        ReadActions.upgradeTableV2(db);

        SaveActions.upgradeTableV3(db);
        VoteActions.upgradeTableV3(db);
    }

    /**
     * Creates the tables for database version 3. It converts the temporary tables of V2 into
     * permanent tables. It also adds new tables for comments and hiding things.
     */
    private static void createDatabaseV3(SQLiteDatabase db) {
        Accounts.createTable(db);
        CommentActions.createTable(db);
        Comments.createTable(db);
        HideActions.createTable(db);
        MessageActions.createTable(db);
        Messages.createTable(db);
        ReadActions.createTable(db);
        Sessions.createTable(db);
        SubredditResults.createTable(db);
        Things.createTable(db);

        SaveActions.createTableV2(db);
        Subreddits.createTableV2(db);
        VoteActions.createTableV2(db);

        Subreddits.insertDefaultSubreddits(db);
    }

    /**
     * Upgrade database to version 3 from version 2.
     */
    private static void upgradeToDatabaseV3(SQLiteDatabase db) {
        Comments.createTable(db);
        HideActions.createTable(db);
        Messages.createTable(db);
        Sessions.createTable(db);
        SubredditResults.createTable(db);
        Things.createTable(db);

        SaveActions.upgradeTableV2(db);
        VoteActions.upgradeTableV2(db);
    }

    /**
     * Creates the tables for database version 2. It creates a bunch of new tables to support
     * accounts and sync adapters. It also uses temporary tables to store data created in
     * {@link #onOpen(SQLiteDatabase)}.
     */
    private static void createDatabaseV2(SQLiteDatabase db) {
        Accounts.createTable(db);
        CommentActions.createTable(db);
        MessageActions.createTable(db);
        ReadActions.createTable(db);
        SaveActions.createTableV1(db);
        VoteActions.createTableV1(db);

        Subreddits.createTableV2(db);
        Subreddits.insertDefaultSubreddits(db);
    }

    /**
     * Upgrade database to version 2 from version 1.
     */
    private static void upgradeToDatabaseV2(SQLiteDatabase db) {
        Accounts.createTable(db);
        CommentActions.createTable(db);
        MessageActions.createTable(db);
        ReadActions.createTable(db);
        SaveActions.createTableV1(db);
        VoteActions.createTableV1(db);

        Subreddits.upgradeSubredditsV2(db);
    }

    /**
     * Creates the tables for database version 1. It supports storing local subreddits.
     */
    private static void createDatabaseV1(SQLiteDatabase db) {
        Subreddits.createTableV1(db);
        Subreddits.insertDefaultSubreddits(db);
    }
}