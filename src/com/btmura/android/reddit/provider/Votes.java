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
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.util.ArrayUtils;

public class Votes implements BaseColumns {

    public static final String TAG = "Votes";
    public static final boolean DEBUG = Debug.DEBUG;

    static final String TABLE_NAME = "votes";
    public static final Uri CONTENT_URI = Uri.parse(VoteProvider.BASE_AUTHORITY_URI + TABLE_NAME);

    static final String MIME_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
            + VoteProvider.AUTHORITY + "." + TABLE_NAME;
    static final String MIME_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
            + VoteProvider.AUTHORITY + "." + TABLE_NAME;

    /** Account that liked this thing. */
    public static final String COLUMN_ACCOUNT = "account";

    public static final String COLUMN_THING_ID = "thingId";
    public static final String COLUMN_VOTE = "vote";

    public static final int VOTE_UP = 1;
    public static final int VOTE_DOWN = -1;

    private static final String SELECTION_BY_ACCOUNT_AND_THING_ID =
            COLUMN_ACCOUNT + " = ? AND " + COLUMN_THING_ID + " = ?";

    static void createTable(SQLiteDatabase db) {
        // TODO: Add unique constraint for account + thingId
        // TODO: Add index for account + thingId ?
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_VOTE + " INTEGER NOT NULL)");
    }

    public static void voteInBackground(final Context context, final String accountName,
            final String thingId, final int likes) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                String[] selectionArgs = ArrayUtils.toArray(accountName, thingId);

                ContentValues values = new ContentValues(3);
                values.put(COLUMN_ACCOUNT, accountName);
                values.put(COLUMN_THING_ID, thingId);
                values.put(COLUMN_VOTE, likes);

                Uri uri = CONTENT_URI.buildUpon()
                        .appendQueryParameter(VoteProvider.PARAM_NOTIFY_OTHERS,
                                Boolean.toString(true))
                        .build();
                int count = cr.update(uri, values, SELECTION_BY_ACCOUNT_AND_THING_ID,
                        selectionArgs);
                if (count == 0) {
                    Uri insertUri = cr.insert(uri, values);
                    if (DEBUG) {
                        Log.d(TAG, "inserted: " + insertUri);
                    }
                } else if (DEBUG) {
                    Log.d(TAG, "updated: " + count);
                }
            }
        });
    }
}
