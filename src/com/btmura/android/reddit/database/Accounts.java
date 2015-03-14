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
import android.provider.BaseColumns;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;

/**
 * Database table for information returned by the /api/me.
 */
public class Accounts implements BaseColumns {

    // Considered using AccountManager#setUserData but comments say it is meant
    // to be used by the account authenticator as a scratch pad.

    public static final String TABLE_NAME = "accounts";

    /** String account name. */
    public static final String COLUMN_ACCOUNT = SharedColumns.COLUMN_ACCOUNT;

    /** Integer amount of link karma. */
    public static final String COLUMN_LINK_KARMA = "linkKarma";

    /** Integer amount of comment karma. */
    public static final String COLUMN_COMMENT_KARMA = "commentKarma";

    /** Integer either 0 or 1 indicating whether the account has mail. */
    public static final String COLUMN_HAS_MAIL = "hasMail";

    public static final String SELECT_BY_ACCOUNT = SharedColumns.SELECT_BY_ACCOUNT;

    public static String getTitle(Context context, String accountName) {
        return !AccountUtils.isAccount(accountName)
                ? context.getString(R.string.account_app_storage)
                : accountName;
    }

    static void create(SQLiteDatabase db) {
        // Account is a unique column.
        db.execSQL("CREATE TABLE " + TABLE_NAME + "("
                + _ID + " INTEGER PRIMARY KEY,"
                + COLUMN_ACCOUNT + " TEXT NOT NULL UNIQUE,"
                + COLUMN_LINK_KARMA + " INTEGER DEFAULT 0,"
                + COLUMN_COMMENT_KARMA + " INTEGER DEFAULT 0,"
                + COLUMN_HAS_MAIL + " INTEGER DEFAULT 0)");
    }
}
