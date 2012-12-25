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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.SessionIds;
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.util.Array;

/**
 * {@link BaseProvider} that has additional methods for handling sessions.
 */
abstract class SessionProvider extends BaseProvider {

    /** Projection with timestamps to see if the sessions are fresh enough. */
    private static final String[] SESSION_PROJECTION = {
            Sessions._ID,
    };

    /** Column index for the session ID in {@link #SESSION_PROJECTION} */
    private static final int INDEX_ID = 0;

    /** Selection for finding a session directly by id. */
    private static final String SELECT_BY_SESSION_ID = SessionIds.COLUMN_SESSION_ID + "=?";

    /** Selection for finding existing sessions for a thing. */
    private static final String SELECT_BY_TYPE_AND_KEY =
            Sessions.COLUMN_TYPE + "=? AND " + Sessions.COLUMN_KEY + "=?";

    SessionProvider(String logTag) {
        super(logTag);
    }

    /**
     * Returns a session id that contains the data matching the type and thing
     * ID given. If there is no such session, it will attempt to use the network
     * to create a session with the data.
     */
    long getListingSession(Listing listing, SQLiteDatabase db, boolean refresh) {

        // Id of a potentially existing session for the same content. We can
        // reuse this id if the caller doesn't request a refresh or if the
        // network is down.
        long existingSessionId = -1;

        // Get the type of listing to search for.
        int type = listing.getType();

        // Get the key to identify existing listings of the same type.
        String key = listing.getKey();

        // Get the name of the table to insert the values into.
        String table = listing.getTargetTable();

        // Get list of matching sessions for the same content.
        Cursor c = db.query(Sessions.TABLE_NAME, SESSION_PROJECTION,
                SELECT_BY_TYPE_AND_KEY, Array.of(Integer.toString(type), key),
                null, null, null);

        // Return the first session with content that we still consider fresh.
        while (c.moveToNext()) {
            existingSessionId = c.getLong(INDEX_ID);
            break;
        }
        c.close();

        // Return the existing session if a refresh is notr equire.
        if (!refresh) {
            if (BuildConfig.DEBUG) {
                Log.d(logTag, "reusing session: " + existingSessionId);
            }
            return existingSessionId;
        }

        // Fetch data from the network. Return the existing session if it fails.
        try {
            ArrayList<ContentValues> values = listing.getValues();

            // Insert new db values. Delete prior existing session.
            db.beginTransaction();
            try {
                // Delete prior candidate session since we have new data.
                if (existingSessionId != -1) {
                    String[] selectionArgs = Array.of(existingSessionId);
                    int deleted1 = db.delete(Sessions.TABLE_NAME, ID_SELECTION, selectionArgs);
                    int deleted2 = db.delete(table, SELECT_BY_SESSION_ID, selectionArgs);
                    if (BuildConfig.DEBUG) {
                        Log.d(logTag, "deleted session: " + existingSessionId
                                + " count: " + deleted1 + "," + deleted2);
                    }
                }

                // Insert a session row for the new data.
                ContentValues sv = new ContentValues(3);
                sv.put(Sessions.COLUMN_TYPE, type);
                sv.put(Sessions.COLUMN_KEY, key);
                sv.put(Sessions.COLUMN_TIMESTAMP, System.currentTimeMillis());
                long sessionId = db.insert(Sessions.TABLE_NAME, null, sv);

                // Add the session id to the data rows.
                int count = values.size();
                for (int i = 0; i < count; i++) {
                    values.get(i).put(SessionIds.COLUMN_SESSION_ID, sessionId);
                }

                // Insert the rows into the database.
                InsertHelper helper = new InsertHelper(db, table);
                for (int i = 0; i < count; i++) {
                    helper.insert(values.get(i));
                }

                if (BuildConfig.DEBUG) {
                    Log.d(logTag, "created session: " + sessionId + " count: " + count);
                }

                db.setTransactionSuccessful();
                return sessionId;

            } finally {
                db.endTransaction();
            }

        } catch (IOException e) {
            Log.e(logTag, e.getMessage(), e);
            return existingSessionId;
        }
    }
}
