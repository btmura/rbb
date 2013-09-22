/*
 * Copyright (C) 2013 Brian Muramatsu
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;

class ListingUtils {

    private static final String[] VOTE_PROJECTION = {
            VoteActions._ID,
            VoteActions.COLUMN_ACTION,
            VoteActions.COLUMN_THING_ID,
    };

    private static final int VOTE_INDEX_ACTION = 1;
    private static final int VOTE_INDEX_THING_ID = 2;

    private static final String VOTE_SELECTION = SharedColumns.SELECT_BY_ACCOUNT;

    static Map<String, Integer> getVoteActionMap(SQLiteOpenHelper dbHelper, String accountName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(VoteActions.TABLE_NAME,
                VOTE_PROJECTION,
                VOTE_SELECTION,
                Array.of(accountName),
                null,
                null,
                null);
        try {
            if (c.getCount() == 0) {
                return Collections.emptyMap();
            }

            Map<String, Integer> map = new HashMap<String, Integer>(c.getCount());
            while (c.moveToNext()) {
                int action = c.getInt(VOTE_INDEX_ACTION);
                String thingId = c.getString(VOTE_INDEX_THING_ID);
                map.put(thingId, action);
            }
            return map;
        } finally {
            c.close();
        }
    }
}
