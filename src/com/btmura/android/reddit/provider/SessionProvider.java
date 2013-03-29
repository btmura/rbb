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
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.util.Array;

/**
 * {@link BaseProvider} that has additional methods for handling sessions.
 */
abstract class SessionProvider extends BaseProvider {

    /**
     * Flag indicating whether we have performed initial database cleaning to remove rows left if
     * the app is terminated abruptly.
     */
    private static final AtomicBoolean NEED_DATABASE_CLEANING = new AtomicBoolean(true);

    private static final String SELECT_MORE_WITH_SESSION_ID =
            Kinds.COLUMN_KIND + "=" + Kinds.KIND_MORE
                    + " AND " + SharedColumns.COLUMN_SESSION_ID + "=?";

    SessionProvider(String logTag) {
        super(logTag);
    }

    /** Returns a session id pointing to the data. */
    long getListingSession(Listing listing, SQLiteDatabase db, long sessionId) throws IOException {
        // Perform one time cleaning of the database to avoid leaving residue in the database if
        // the app was terminated abruptly or crashed.
        if (NEED_DATABASE_CLEANING.getAndSet(false)) {
            db.beginTransaction();
            try {
                int deleted = db.delete(Things.TABLE_NAME, null, null);
                deleted += db.delete(Messages.TABLE_NAME, null, null);
                deleted += db.delete(SubredditResults.TABLE_NAME, null, null);
                deleted += db.delete(Sessions.TABLE_NAME, null, null);
                if (BuildConfig.DEBUG) {
                    Log.d(logTag, "cleaned: " + deleted);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        // Double check that the session exists if specified.
        if (sessionId != -1) {
            long count = DatabaseUtils.queryNumEntries(db, Sessions.TABLE_NAME,
                    Sessions.SELECT_BY_ID, Array.of(sessionId));
            if (count == 0) {
                sessionId = -1;
            }
        }

        // Return existing session if it exists and we're not appending more.
        if (sessionId != -1 && !listing.isAppend()) {
            return sessionId;
        }

        // Fetch values to insert from the network.
        ArrayList<ContentValues> values = listing.getValues();

        // Insert new db values.
        db.beginTransaction();
        try {
            // Delete any existing "Loading..." signs if appending.
            if (listing.isAppend()) {
                // Appending requires an existing session to append the data.
                if (sessionId == -1) {
                    throw new IllegalStateException();
                }

                // Delete the row for this append. If there is no such row, then
                // this might be a duplicate append that got triggered, so just
                // return the existing session id and hope for the best.
                int count = db.delete(listing.getTargetTable(),
                        SELECT_MORE_WITH_SESSION_ID, Array.of(sessionId));
                if (count == 0) {
                    return sessionId;
                }
            }

            // Create a new session if there is no id.
            if (sessionId == -1) {
                ContentValues v = new ContentValues(1);
                v.put(Sessions.COLUMN_TIMESTAMP, System.currentTimeMillis());
                sessionId = db.insert(Sessions.TABLE_NAME, null, v);
                if (BuildConfig.DEBUG) {
                    Log.d(logTag, "created new session: " + sessionId);
                }
            }

            // Add the session id to the data rows.
            int count = values.size();
            for (int i = 0; i < count; i++) {
                values.get(i).put(SharedColumns.COLUMN_SESSION_ID, sessionId);
            }

            // Insert the rows into the database.
            InsertHelper helper = new InsertHelper(db, listing.getTargetTable());
            for (int i = 0; i < count; i++) {
                helper.insert(values.get(i));
            }

            db.setTransactionSuccessful();
            return sessionId;
        } finally {
            db.endTransaction();
        }
    }
}
