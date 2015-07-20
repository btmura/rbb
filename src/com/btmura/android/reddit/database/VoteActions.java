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

/**
 * {@link VoteActions} is a table that stores pending upvotes and downvotes
 * before they are synced back to the server.
 */
public class VoteActions implements BaseThingColumns, BaseColumns {

  public static final String TABLE_NAME = "voteActions";

  /** Account that liked this thing. */
  public static final String COLUMN_ACCOUNT = "account";

  /** Integer column indicating either an upvote or downvote. */
  public static final String COLUMN_ACTION = "action";

  /** Unused long column with expiration. */
  public static final String COLUMN_EXPIRATION = "expiration";

  /** Boolean column indicating whether to show this in the liked listing. */
  public static final String COLUMN_SHOW_IN_LISTING = "showInListing";

  /** Number of sync failures. */
  public static final String COLUMN_SYNC_FAILURES = "syncFailures";

  /** Unused string column with sync status. */
  public static final String COLUMN_SYNC_STATUS = "syncStatus";

  /** String ID of the thing that the user wants to vote on. */
  public static final String COLUMN_THING_ID = "thingId";

  /** Vote column value indicating an upvote. */
  public static final int ACTION_VOTE_UP = 1;

  /** Vote column value indicating a neutral vote. */
  public static final int ACTION_VOTE_NEUTRAL = 0;

  /** Vote column value indicating a downvote. */
  public static final int ACTION_VOTE_DOWN = -1;

  public static final String SELECT_SHOWABLE_BY_ACCOUNT =
      SharedColumns.SELECT_BY_ACCOUNT + " AND " + COLUMN_SHOW_IN_LISTING + "=1";

  public static final String SELECT_SHOWABLE_NOT_DOWN_BY_ACCOUNT =
      SELECT_SHOWABLE_BY_ACCOUNT + " AND "
          + COLUMN_ACTION + "!=" + ACTION_VOTE_DOWN;

  public static final String SELECT_SHOWABLE_NOT_UP_BY_ACCOUNT =
      SELECT_SHOWABLE_BY_ACCOUNT + " AND "
          + COLUMN_ACTION + "!=" + ACTION_VOTE_UP;

  public static final String SORT_BY_ID = SharedColumns.SORT_BY_ID;

  static void createV3(SQLiteDatabase db) {
    createV2(db);
    upgradeToV3(db);
  }

  static void upgradeToV3(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE " + TABLE_NAME
        + " ADD " + COLUMN_SYNC_FAILURES + " INTEGER DEFAULT 0");
    db.execSQL("ALTER TABLE " + TABLE_NAME
        + " ADD " + COLUMN_SYNC_STATUS + " TEXT");
  }

  static void createV2(SQLiteDatabase db) {
    create(db);
    upgradeToV2(db);
  }

  static void upgradeToV2(SQLiteDatabase db) {
    db.execSQL("ALTER TABLE " + TABLE_NAME
        + " ADD " + COLUMN_HIDDEN + " INTEGER DEFAULT 0");
    db.execSQL("ALTER TABLE " + TABLE_NAME
        + " ADD " + COLUMN_SAVED + " INTEGER DEFAULT 0");
  }

  static void create(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY,"
        + COLUMN_ACCOUNT + " TEXT NOT NULL,"
        + COLUMN_ACTION + " INTEGER NOT NULL,"
        + COLUMN_EXPIRATION + " INTEGER DEFAULT 0,"
        + COLUMN_SHOW_IN_LISTING + " INTEGER DEFAULT 0,"
        + COLUMN_THING_ID + " TEXT NOT NULL,"

        // Create thing columns to store enough info needed to display a
        // fake item in certain listing.
        + CREATE_THING_COLUMNS + ","

        // Add constraint to make it easy to replace actions.
        + "UNIQUE (" + COLUMN_ACCOUNT + "," + COLUMN_THING_ID + "))");
  }
}
