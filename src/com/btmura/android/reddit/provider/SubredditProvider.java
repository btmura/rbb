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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Array;

public class SubredditProvider extends BaseProvider {

    public static final String TAG = "SubredditProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subreddits";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_SUBREDDITS = "subreddits";
    public static final Uri SUBREDDITS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SUBREDDITS);
    public static final Uri SUBREDDITS_SYNC_URI = makeSyncUri(SUBREDDITS_URI);

    private static final String METHOD_ADD_SUBREDDITS = "addSubreddits";
    private static final String METHOD_REMOVE_SUBREDDITS = "removeSubreddits";

    private static final String EXTRA_SUBREDDIT_ARRAY = "subredditArray";

    public SubredditProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        return Subreddits.TABLE_NAME;
    }

    public static Bundle addSubreddits(Context context,
            String accountName,
            String... subreddits) {
        Bundle extras = new Bundle(1);
        extras.putStringArray(EXTRA_SUBREDDIT_ARRAY, subreddits);
        return Provider.call(context,
                SUBREDDITS_URI,
                METHOD_ADD_SUBREDDITS,
                accountName,
                extras);
    }

    public static Bundle removeSubreddits(Context context,
            String accountName,
            String... subreddits) {
        Bundle extras = new Bundle(1);
        extras.putStringArray(EXTRA_SUBREDDIT_ARRAY, subreddits);
        return Provider.call(context,
                SUBREDDITS_URI,
                METHOD_REMOVE_SUBREDDITS,
                accountName,
                extras);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_ADD_SUBREDDITS.equals(method)) {
            return addSubredditAsync(arg, extras);
        } else if (METHOD_REMOVE_SUBREDDITS.equals(method)) {
            return removeSubredditAsync(arg, extras);
        }
        return null;
    }

    private Bundle addSubredditAsync(String accountName, Bundle extras) {
        String[] subreddits = extras.getStringArray(EXTRA_SUBREDDIT_ARRAY);
        return changeSubreddits(accountName, subreddits, true);
    }

    private Bundle removeSubredditAsync(String accountName, Bundle extras) {
        String[] subreddits = extras.getStringArray(EXTRA_SUBREDDIT_ARRAY);
        return changeSubreddits(accountName, subreddits, false);
    }

    private Bundle changeSubreddits(String accountName, String[] subreddits, boolean add) {
        ContentValues values = new ContentValues(3);
        int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int count = subreddits.length;
            for (int i = 0; i < count; i++) {
                // All subreddit additions require an insert. The insert would remove any deletes
                // due to table constraints.
                //
                // Deletes for an account require an insert with delete state. However, app storage
                // accounts should just remove the row altogether.
                if (add || AccountUtils.isAccount(accountName)) {
                    values.clear();
                    values.put(Subreddits.COLUMN_ACCOUNT, accountName);
                    values.put(Subreddits.COLUMN_NAME, subreddits[i]);
                    values.put(Subreddits.COLUMN_STATE, state);
                    if (db.replace(Subreddits.TABLE_NAME, null, values) == -1) {
                        return null;
                    }
                } else {
                    db.delete(Subreddits.TABLE_NAME,
                            Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                            Array.of(accountName, subreddits[i]));
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Provider.scheduleBackup(getContext(), accountName);

        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(SUBREDDITS_URI, null, AccountUtils.isAccount(accountName));
        return Bundle.EMPTY;
    }
}
