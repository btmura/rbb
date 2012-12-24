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
import java.util.concurrent.TimeUnit;

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

    /** Timestamp to apply to all data so we can clean it up later if necessary. */
    private static long SESSION_TIMESTAMP = -1;

    SessionProvider(String logTag) {
        super(logTag);
    }

    /**
     * Return the session timestamp to mark the data.
     */
    static long getSessionTimestamp() {
        // Initialize this once to delete all session data that was created
        // before this time. This allows to clean up any residue in the
        // database that can no longer be viewed.
        if (SESSION_TIMESTAMP == -1) {
            SESSION_TIMESTAMP = System.currentTimeMillis();
        }
        return SESSION_TIMESTAMP;
    }

    /** Projection with timestamps to see if the sessions are fresh enough. */
    private static final String[] SESSION_PROJECTION = {
            Sessions._ID,
            Sessions.COLUMN_TIMESTAMP,
    };

    /** Column index for the session ID in {@link #SESSION_PROJECTION} */
    private static final int INDEX_ID = 0;

    /** Column index for the timestamp in {@link #SESSION_PROJECTION} */
    private static final int INDEX_TIMESTAMP = 1;

    /** Selection for finding a session directly by id. */
    private static final String SELECT_BY_SESSION_ID = SessionIds.COLUMN_SESSION_ID + "=?";

    /** Selection for finding existing sessions for a thing. */
    private static final String SELECT_BY_KEY_AND_TYPE =
            Sessions.COLUMN_KEY + "=? AND " + Sessions.COLUMN_TYPE + "=?";

    /** Interval for how long a session's data is considered still fresh. */
    private static final long SESSION_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    /**
     * Returns a session id that contains the data matching the type and thing
     * ID given. If there is no such session, it will attempt to use the network
     * to create a session with the data.
     */
    long getListingSession(Listing listing, String key,
            SQLiteDatabase db, String tableName, String sessionIdKey) {

        // Current time for measuring age of sessions and inserting new ones.
        long now = System.currentTimeMillis();

        // Fetch is whether or not we want to use the network to get new data.
        boolean fetch = true;

        // candidateSession is the best session of data to return in case the
        // network is down. It may have old data. -1 means no candidate.
        long candidateId = -1;

        // Get the type of listing to search for.
        int type = listing.getType();

        // Get list of matching sessions expired or not.
        Cursor c = db.query(Sessions.TABLE_NAME, SESSION_PROJECTION,
                SELECT_BY_KEY_AND_TYPE, Array.of(key, Integer.toString(type)),
                null, null, null);

        // Return the first session with content that we still consider fresh.
        while (c.moveToNext()) {
            long id = c.getLong(INDEX_ID);
            long timestamp = c.getLong(INDEX_TIMESTAMP);

            // Any matching data serves as some sort of candidate.
            if (candidateId == -1) {
                candidateId = id;
            }

            // Use fresh data and avoid going to the network.
            if (timestamp + SESSION_INTERVAL >= now) {
                candidateId = id;
                fetch = false;
                break;
            }
        }
        c.close();

        // Return fresh session if it was found.
        if (!fetch) {
            if (BuildConfig.DEBUG) {
                Log.d(logTag, "reusing session: " + candidateId);
            }
            return candidateId;
        }

        // Fetch data from the network. Return the candidate if it fails.
        try {
            ArrayList<ContentValues> values = listing.getValues();

            // Insert new db values. Delete prior candidate session.
            db.beginTransaction();
            try {
                // Delete prior candidate session since we have new data.
                if (candidateId != -1) {
                    String[] selectionArgs = Array.of(candidateId);
                    int deleted1 = db.delete(Sessions.TABLE_NAME, ID_SELECTION, selectionArgs);
                    int deleted2 = db.delete(tableName, SELECT_BY_SESSION_ID, selectionArgs);
                    if (BuildConfig.DEBUG) {
                        Log.d(logTag, "deleted session: " + candidateId
                                + " count: " + deleted1 + " " + deleted2);
                    }
                }

                // Insert a session row for the new data.
                ContentValues sv = new ContentValues(3);
                sv.put(Sessions.COLUMN_TYPE, type);
                sv.put(Sessions.COLUMN_KEY, key);
                sv.put(Sessions.COLUMN_TIMESTAMP, now);
                long sessionId = db.insert(Sessions.TABLE_NAME, null, sv);

                // Add the session id to the data rows.
                int count = values.size();
                for (int i = 0; i < count; i++) {
                    values.get(i).put(sessionIdKey, sessionId);
                }

                // Insert the rows into the database.
                InsertHelper helper = new InsertHelper(db, tableName);
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
            return candidateId;
        }
    }
}
