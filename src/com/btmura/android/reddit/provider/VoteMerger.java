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
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_DOWNS;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_LIKES;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_SCORE;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_UPS;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_DOWN;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_NEUTRAL;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_UP;
import static com.btmura.android.reddit.database.VoteActions.COLUMN_ACTION;
import static com.btmura.android.reddit.database.VoteActions.COLUMN_THING_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;

class VoteMerger {

    private static final String[] VOTE_PROJECTION = {
            _ID,
            COLUMN_ACTION,
            COLUMN_THING_ID,
    };

    private static final int VOTE_INDEX_ACTION = 1;
    private static final int VOTE_INDEX_THING_ID = 2;

    private static final String THING_UPDATE = "UPDATE " + Things.TABLE_NAME;
    private static final String COMMENT_UPDATE = "UPDATE " + Comments.TABLE_NAME;

    private static final String UP_DOWN_SET = " SET "
            + COLUMN_SCORE + "=" + COLUMN_SCORE + "+?,"
            + COLUMN_LIKES + "=?,"
            + COLUMN_UPS + "=" + COLUMN_UPS + "+?,"
            + COLUMN_DOWNS + "=" + COLUMN_DOWNS + "+? ";

    private static final String NEUTRAL_SET = " SET "
            + COLUMN_SCORE + "= CASE " + COLUMN_LIKES
            + " WHEN " + ACTION_VOTE_UP + " THEN " + COLUMN_SCORE + "-1"
            + " WHEN " + ACTION_VOTE_DOWN + " THEN " + COLUMN_SCORE + "+1"
            + " ELSE " + COLUMN_SCORE
            + " END,"

            + COLUMN_LIKES + "=" + VoteActions.ACTION_VOTE_NEUTRAL + ","

            + COLUMN_UPS + "= CASE " + COLUMN_LIKES
            + " WHEN " + ACTION_VOTE_UP + " THEN " + COLUMN_UPS + "-1"
            + " ELSE " + COLUMN_UPS
            + " END,"

            + COLUMN_DOWNS + "= CASE " + COLUMN_LIKES
            + " WHEN " + ACTION_VOTE_DOWN + " THEN " + COLUMN_DOWNS + "-1"
            + " ELSE " + COLUMN_DOWNS
            + " END ";

    private static final String WHERE = " WHERE " + SharedColumns.SELECT_BY_ACCOUNT_AND_THING_ID;

    private static final String UP_DOWN_THING_VOTE = THING_UPDATE + UP_DOWN_SET + WHERE;
    private static final String UP_DOWN_COMMENT_VOTE = COMMENT_UPDATE + UP_DOWN_SET + WHERE;
    private static final String NEUTRAL_THING_VOTE = THING_UPDATE + NEUTRAL_SET + WHERE;
    private static final String NEUTRAL_COMMENT_VOTE = COMMENT_UPDATE + NEUTRAL_SET + WHERE;

    static Map<String, Integer> getActionMap(SQLiteOpenHelper dbHelper, String accountName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(VoteActions.TABLE_NAME,
                VOTE_PROJECTION,
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
                int action = c.getInt(VOTE_INDEX_ACTION);
                String thingId = c.getString(VOTE_INDEX_THING_ID);
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

    static void updateContentValues(ContentValues v, int action) {
        switch (action) {
            case ACTION_VOTE_UP:
                change(v, COLUMN_SCORE, 1);
                change(v, COLUMN_UPS, 1);
                break;

            case ACTION_VOTE_DOWN:
                change(v, COLUMN_SCORE, -1);
                change(v, COLUMN_DOWNS, 1);
                break;

            case ACTION_VOTE_NEUTRAL:
                if (v.containsKey(COLUMN_LIKES)) {
                    Integer likes = (Integer) v.get(COLUMN_LIKES);
                    if (likes == ACTION_VOTE_UP) {
                        change(v, COLUMN_SCORE, -1);
                        change(v, COLUMN_UPS, -1);
                    } else if (likes == ACTION_VOTE_DOWN) {
                        change(v, COLUMN_SCORE, 1);
                        change(v, COLUMN_DOWNS, -1);
                    }
                }
                break;
        }
        v.put(COLUMN_LIKES, action);
    }

    private static void change(ContentValues v, String key, int delta) {
        Integer value = (Integer) v.get(key);
        v.put(key, (value != null ? value : 0) + delta);
    }

    static int updateDatabase(SQLiteDatabase db, String accountName, int action, String thingId) {
        int changed = 0;
        switch (action) {
            case ACTION_VOTE_UP:
                changed += upVote(db, UP_DOWN_THING_VOTE, accountName, thingId);
                changed += upVote(db, UP_DOWN_COMMENT_VOTE, accountName, thingId);
                break;

            case ACTION_VOTE_DOWN:
                changed += downVote(db, UP_DOWN_THING_VOTE, accountName, thingId);
                changed += downVote(db, UP_DOWN_COMMENT_VOTE, accountName, thingId);
                break;

            case ACTION_VOTE_NEUTRAL:
                changed += neutralVote(db, NEUTRAL_THING_VOTE, accountName, thingId);
                changed += neutralVote(db, NEUTRAL_COMMENT_VOTE, accountName, thingId);
                break;
        }
        return changed;
    }

    private static int upVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindLong(1, 1);
        sql.bindLong(2, ACTION_VOTE_UP);
        sql.bindLong(3, 1);
        sql.bindLong(4, 0);
        sql.bindString(5, accountName);
        sql.bindString(6, thingId);
        return sql.executeUpdateDelete();
    }

    private static int downVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindLong(1, -1);
        sql.bindLong(2, ACTION_VOTE_DOWN);
        sql.bindLong(3, 0);
        sql.bindLong(4, 1);
        sql.bindString(5, accountName);
        sql.bindString(6, thingId);
        return sql.executeUpdateDelete();
    }

    private static int neutralVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindString(1, accountName);
        sql.bindString(2, thingId);
        return sql.executeUpdateDelete();
    }
}
