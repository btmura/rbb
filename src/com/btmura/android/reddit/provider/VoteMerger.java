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

import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_DOWNS;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_LIKES;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_SCORE;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_UPS;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
            VoteActions._ID,
            VoteActions.COLUMN_ACTION,
            VoteActions.COLUMN_THING_ID,
    };

    private static final int VOTE_INDEX_ACTION = 1;
    private static final int VOTE_INDEX_THING_ID = 2;

    private static final String VOTE_SELECTION = SharedColumns.SELECT_BY_ACCOUNT;

    private static final String THING_UPDATE = "UPDATE " + Things.TABLE_NAME;
    private static final String COMMENT_UPDATE = "UPDATE " + Comments.TABLE_NAME;

    private static final String UP_DOWN_SET = " SET "
            + COLUMN_SCORE + "=" + COLUMN_SCORE + "+?,"
            + COLUMN_LIKES + "=?,"
            + COLUMN_UPS + "=" + COLUMN_UPS + "+?,"
            + COLUMN_DOWNS + "=" + COLUMN_DOWNS + "+? ";

    private static final String NEUTRAL_SET = " SET "
            + COLUMN_SCORE + "= CASE " + COLUMN_LIKES
            + " WHEN " + VoteActions.ACTION_VOTE_UP + " THEN " + COLUMN_SCORE + "-1"
            + " WHEN " + VoteActions.ACTION_VOTE_DOWN + " THEN " + COLUMN_SCORE + "+1"
            + " ELSE " + COLUMN_SCORE
            + " END,"

            + COLUMN_LIKES + "=" + VoteActions.ACTION_VOTE_NEUTRAL + ","

            + COLUMN_UPS + "= CASE " + COLUMN_LIKES
            + " WHEN " + VoteActions.ACTION_VOTE_UP + " THEN " + COLUMN_UPS + "-1"
            + " ELSE " + COLUMN_UPS
            + " END,"

            + COLUMN_DOWNS + "= CASE " + COLUMN_LIKES
            + " WHEN " + VoteActions.ACTION_VOTE_DOWN + " THEN " + COLUMN_DOWNS + "-1"
            + " ELSE " + COLUMN_DOWNS
            + " END ";

    private static final String WHERE = " WHERE "
            + SharedColumns.COLUMN_ACCOUNT + "=? AND " + SharedColumns.COLUMN_THING_ID + "=?";

    private static final String UP_DOWN_THING_VOTE = THING_UPDATE + UP_DOWN_SET + WHERE;
    private static final String UP_DOWN_COMMENT_VOTE = COMMENT_UPDATE + UP_DOWN_SET + WHERE;
    private static final String NEUTRAL_THING_VOTE = THING_UPDATE + NEUTRAL_SET + WHERE;
    private static final String NEUTRAL_COMMENT_VOTE = COMMENT_UPDATE + NEUTRAL_SET + WHERE;

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

    static void execute(SQLiteDatabase db, String accountName, int action, String thingId) {
        switch (action) {
            case VoteActions.ACTION_VOTE_UP:
                upVote(db, UP_DOWN_THING_VOTE, accountName, thingId);
                upVote(db, UP_DOWN_COMMENT_VOTE, accountName, thingId);
                break;

            case VoteActions.ACTION_VOTE_DOWN:
                downVote(db, UP_DOWN_THING_VOTE, accountName, thingId);
                downVote(db, UP_DOWN_COMMENT_VOTE, accountName, thingId);
                break;

            case VoteActions.ACTION_VOTE_NEUTRAL:
                neutralVote(db, NEUTRAL_THING_VOTE, accountName, thingId);
                neutralVote(db, NEUTRAL_COMMENT_VOTE, accountName, thingId);
                break;
        }
    }

    private static void upVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindLong(1, 1);
        sql.bindLong(2, VoteActions.ACTION_VOTE_UP);
        sql.bindLong(3, 1);
        sql.bindLong(4, 0);
        sql.bindString(5, accountName);
        sql.bindString(6, thingId);
        sql.executeUpdateDelete();
    }

    private static void downVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindLong(1, -1);
        sql.bindLong(2, VoteActions.ACTION_VOTE_DOWN);
        sql.bindLong(3, 0);
        sql.bindLong(4, 1);
        sql.bindString(5, accountName);
        sql.bindString(6, thingId);
        sql.executeUpdateDelete();
    }

    private static void neutralVote(SQLiteDatabase db,
            String statement,
            String accountName,
            String thingId) {
        SQLiteStatement sql = db.compileStatement(statement);
        sql.bindString(1, accountName);
        sql.bindString(2, thingId);
        sql.executeUpdateDelete();
    }
}
