/*
 * Copyright (C) 2015 Brian Muramatsu
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

/** Table for actions to be done on accounts like marking messages read. */
public class AccountActions implements BaseColumns {

  /** Name of the table to use in SQL queries. */
  public static final String TABLE_NAME = "accountActions";

  /** Name of the account to perform the action on. */
  public static final String COLUMN_ACCOUNT = "account";

  /** Action to perform on the account. */
  public static final String COLUMN_ACTION = "action";

  /** Action that marks the account's messages read. */
  public static final int ACTION_MARK_MESSAGES_READ = 1;

  /** Where clause fragment to select actions by ID. */
  public static final String SELECT_BY_ID = SharedColumns.SELECT_BY_ID;

  /** Where clause fragment to select actions by account name. */
  public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

  /** Runs SQL to create the table. */
  static void create(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
        + _ID + " INTEGER PRIMARY KEY,"
        + COLUMN_ACCOUNT + " TEXT NOT NULL,"
        + COLUMN_ACTION + " INTEGER NOT NULL)");
  }
}
