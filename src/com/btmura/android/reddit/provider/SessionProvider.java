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
import com.btmura.android.reddit.database.Sessions;
import com.btmura.android.reddit.util.Array;

/**
 * {@link BaseProvider} that has additional methods for handling sessions.
 */
abstract class SessionProvider extends BaseProvider {

    private static final String TAG = "SessionProvider";

    private static final boolean DEBUG = BuildConfig.DEBUG && !true;

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

    private static final String[] SESSION_PROJECTION = {Sessions._ID};
    private static final String SESSION_SELECTION = Sessions.COLUMN_TYPE + "=? AND "
            + Sessions.COLUMN_THING_ID + "=?";

    long getListingSession(int type, String thingId, Listing listing,
            SQLiteDatabase db, String tableName, String sessionIdKey) throws IOException {
        long sessionId = findListingSession(type, thingId, db);
        if (sessionId != -1) {
            if (DEBUG) {
                Log.d(TAG, "findListingSession sessionId: " + sessionId);
            }
            return sessionId;
        }
        return createListingSession(type, thingId, listing, db, tableName, sessionIdKey);
    }

    private long findListingSession(int type, String thingId, SQLiteDatabase db) {
        Cursor c = db.query(Sessions.TABLE_NAME, SESSION_PROJECTION,
                SESSION_SELECTION, Array.of(Integer.toString(type), thingId),
                null, null, null);
        try {
            if (c.moveToNext()) {
                return c.getLong(0);
            }
            return -1;
        } finally {
            c.close();
        }
    }

    private long createListingSession(int type, String thingId, Listing listing,
            SQLiteDatabase db, String tableName, String sessionIdKey) throws IOException {
        ArrayList<ContentValues> values = listing.getValues();
        db.beginTransaction();
        try {
            // Create a new row for this session.
            ContentValues sv = newSessionValues(type, thingId);
            long sessionId = db.insert(Sessions.TABLE_NAME, null, sv);

            // Add the session id to the rows.
            int count = values.size();
            for (int i = 0; i < count; i++) {
                values.get(i).put(sessionIdKey, sessionId);
            }

            // Insert the rows into the database.
            InsertHelper helper = new InsertHelper(db, tableName);
            for (int i = 0; i < count; i++) {
                helper.insert(values.get(i));
            }

            db.setTransactionSuccessful();

            if (DEBUG) {
                Log.d(TAG, "createListingSession sessionId: " + sessionId + " values: " + count);
            }
            return sessionId;

        } finally {
            db.endTransaction();
        }
    }

    private ContentValues newSessionValues(int type, String thingId) {
        ContentValues values = new ContentValues(3);
        values.put(Sessions.COLUMN_TYPE, type);
        values.put(Sessions.COLUMN_THING_ID, thingId);
        values.put(Sessions.COLUMN_TIMESTAMP, System.currentTimeMillis());
        return values;
    }
}
