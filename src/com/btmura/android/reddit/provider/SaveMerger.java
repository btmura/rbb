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

import static android.provider.BaseColumns._ID;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_SAVED;
import static com.btmura.android.reddit.database.SaveActions.ACTION_SAVE;
import static com.btmura.android.reddit.database.SaveActions.ACTION_UNSAVE;
import static com.btmura.android.reddit.database.SaveActions.COLUMN_ACTION;
import static com.btmura.android.reddit.database.SaveActions.COLUMN_THING_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.btmura.android.reddit.database.BaseThingColumns;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.util.Array;

class SaveMerger {

    private static final String[] SAVE_PROJECTION = {
            _ID,
            COLUMN_ACTION,
            COLUMN_THING_ID,
    };

    private static final int SAVE_INDEX_ACTION = 1;
    private static final int SAVE_INDEX_THING_ID = 2;

    static Map<String, Integer> getActionMap(SQLiteOpenHelper dbHelper, String accountName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(SaveActions.TABLE_NAME,
                SAVE_PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT,
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
                int action = c.getInt(SAVE_INDEX_ACTION);
                String thingId = c.getString(SAVE_INDEX_THING_ID);
                map.put(thingId, action);
            }
            return map;
        } finally {
            c.close();
        }
    }

    static void updateContentValues(ContentValues v, Map<String, Integer> actionMap) {
        if (!actionMap.isEmpty()) {
            String thingId = (String) v.get(SharedColumns.COLUMN_THING_ID);
            Integer action = actionMap.remove(thingId);
            if (action != null) {
                updateContentValues(v, action);
            }
        }
    }

    private static void updateContentValues(ContentValues v, int action) {
        switch (action) {
            case ACTION_SAVE:
                v.put(COLUMN_SAVED, 1);
                break;

            case ACTION_UNSAVE:
                v.put(COLUMN_SAVED, 0);
                break;
        }
    }

    static int updateDatabase(SQLiteDatabase db,
            String accountName,
            int action,
            String thingId,
            ContentValues v) {
        v.clear();
        v.put(BaseThingColumns.COLUMN_SAVED, action == ACTION_SAVE);

        int changed = 0;
        changed += db.update(Things.TABLE_NAME, v,
                SharedColumns.SELECT_BY_ACCOUNT_AND_THING_ID,
                Array.of(accountName, thingId));
        changed += db.update(Comments.TABLE_NAME, v,
                SharedColumns.SELECT_BY_ACCOUNT_AND_THING_ID,
                Array.of(accountName, thingId));
        return changed;
    }
}
