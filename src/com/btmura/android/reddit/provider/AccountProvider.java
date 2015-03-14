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

import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.AccountActions;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.util.Array;

public class AccountProvider extends BaseProvider {

    public static final String TAG = "AccountProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.accounts";

    static final String PATH_ACCOUNTS = "accounts";
    static final String PATH_ACCOUNT_ACTIONS = "actions/accounts";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri ACCOUNTS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACCOUNTS);
    public static final Uri ACCOUNT_ACTIONS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACCOUNT_ACTIONS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ACCOUNTS = 1;
    private static final int MATCH_ACCOUNT_ACTIONS = 2;
    static {
        MATCHER.addURI(AUTHORITY, PATH_ACCOUNTS, MATCH_ACCOUNTS);
        MATCHER.addURI(AUTHORITY, PATH_ACCOUNT_ACTIONS, MATCH_ACCOUNT_ACTIONS);
    }

    private static final String METHOD_INITIALIZE_ACCOUNT = "initializeAccount";
    private static final String METHOD_CLEAR_MESSAGES = "clearMessages";

    /** String extra containing the cookie for an account. */
    private static final String EXTRA_COOKIE = "cookie";

    public AccountProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        switch (MATCHER.match(uri)) {
            case MATCH_ACCOUNTS:
                return Accounts.TABLE_NAME;

            case MATCH_ACCOUNT_ACTIONS:
                return AccountActions.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    /**
     * Initializes a new account by importing subreddits and returns true on success.
     */
    public static boolean initializeAccount(Context context, String accountName, String cookie) {
        Bundle args = new Bundle(1);
        args.putString(EXTRA_COOKIE, cookie);
        return Provider.call(context,
                ACCOUNTS_URI,
                METHOD_INITIALIZE_ACCOUNT,
                accountName,
                args) != null;
    }

    public static void clearMessages(Context context, String accountName) {
        Provider.call(context, ACCOUNTS_URI, METHOD_CLEAR_MESSAGES, accountName, null);
    }

    @Override
    public Bundle call(String method, String accountName, Bundle extras) {
        if (METHOD_INITIALIZE_ACCOUNT.equals(method)) {
            return initializeAccount(accountName, extras);
        } else if (METHOD_CLEAR_MESSAGES.equals(method)) {
            return clearMessages(accountName);
        }
        return null;
    }

    /**
     * Returns a non-null empty bundle on successfully creating an account. Otherwise, it returns
     * null on failure whether from getting the user's subreddits or encountering database issues.
     *
     * This method touches many tables that are not the responsibility of AccountProvider, but
     * somebody with access to the database must do this job to assure everything is done in a
     * single transaction.
     */
    private Bundle initializeAccount(String accountName, Bundle extras) {
        String cookie = extras.getString(EXTRA_COOKIE);
        ArrayList<String> subreddits;
        try {
            subreddits = RedditApi.getSubreddits(cookie);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

        String[] tables = {
                Accounts.TABLE_NAME,
                Subreddits.TABLE_NAME,
                Things.TABLE_NAME,
                Comments.TABLE_NAME,
                Messages.TABLE_NAME,
                SubredditResults.TABLE_NAME,
                Sessions.TABLE_NAME,
                CommentActions.TABLE_NAME,
                MessageActions.TABLE_NAME,
                ReadActions.TABLE_NAME,
                SaveActions.TABLE_NAME,
                VoteActions.TABLE_NAME,
        };

        String selection = SharedColumns.SELECT_BY_ACCOUNT;
        String[] selectionArgs = Array.of(accountName);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int tableCount = tables.length;
            int deleted = 0;
            for (int i = 0; i < tableCount; i++) {
                deleted += db.delete(tables[i], selection, selectionArgs);
            }

            ContentValues values = new ContentValues(3);
            values.put(Subreddits.COLUMN_ACCOUNT, accountName);
            values.put(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL);

            int subredditCount = subreddits.size();
            int inserted = 0;
            for (int i = 0; i < subredditCount; i++) {
                values.put(Subreddits.COLUMN_NAME, subreddits.get(i));
                db.insert(Subreddits.TABLE_NAME, null, values);
                inserted++;
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "deleted: " + deleted + " inserted: " + inserted);
            }
            db.setTransactionSuccessful();
            return Bundle.EMPTY;
        } finally {
            db.endTransaction();
        }
    }

    // TODO(btmura): rename to mark messages read.
    private Bundle clearMessages(String accountName) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Update the account row which may or may not exist.
            ContentValues v = new ContentValues(2);
            v.put(Accounts.COLUMN_HAS_MAIL, false);
            db.update(Accounts.TABLE_NAME, v, Accounts.SELECT_BY_ACCOUNT, Array.of(accountName));

            // Schedule an action to mark messages read.
            v.clear();
            v.put(AccountActions.COLUMN_ACCOUNT, accountName);
            v.put(AccountActions.COLUMN_ACTION, AccountActions.ACTION_MARK_MESSAGES_READ);
            if (db.insert(AccountActions.TABLE_NAME, null, v) == -1) {
                return Bundle.EMPTY;
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(ACCOUNTS_URI, null, SYNC);
        return Bundle.EMPTY;
    }
}
