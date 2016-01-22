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

public class SubredditResults implements BaseColumns {

  public static final String TABLE_NAME = "subredditResults";

  public static final String COLUMN_ACCOUNT = SharedColumns.COLUMN_ACCOUNT;

  public static final String COLUMN_NAME = "name";

  public static final String COLUMN_OVER_18 = "over18";

  public static final String COLUMN_SESSION_ID =
      SharedColumns.COLUMN_SESSION_ID;

  /** Number of subscribers to this subreddit. */
  public static final String COLUMN_SUBSCRIBERS = "subscribers";

  public static final String SELECT_BY_SESSION_ID = COLUMN_SESSION_ID + "=?";

  public static final String SORT_BY_NAME = COLUMN_NAME + " COLLATE NOCASE ASC";

  static void create(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY, "
        + COLUMN_ACCOUNT + " TEXT NOT NULL, "
        + COLUMN_NAME + " TEXT, "
        + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
        + COLUMN_SESSION_ID + " TEXT NOT NULL, "
        + COLUMN_SUBSCRIBERS + " INTEGER)");
  }

  /** Creates the temporary table used in v2. Kept for testing upgrades. */
  static void createTempTableV2(SQLiteDatabase db) {
    db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY, "
        + COLUMN_ACCOUNT + " TEXT NOT NULL, "
        + COLUMN_NAME + " TEXT, "
        + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
        + COLUMN_SESSION_ID + " TEXT NOT NULL, "
        + COLUMN_SUBSCRIBERS + " INTEGER)");
  }
}
