package com.btmura.android.reddit.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.test.AndroidTestCase;

import com.btmura.android.reddit.database.BaseThingColumns;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.DbHelper;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.VoteActions;
import com.btmura.android.reddit.util.Array;

public class VoteMergerTest extends AndroidTestCase {

    private static final String ACCOUNT_NAME = "account1";
    private static final String THING_ID = "thing1";

    private static final String[] TABLES = {
            Things.TABLE_NAME,
            Comments.TABLE_NAME,
    };

    private static final String[] PROJECTION = {
            BaseColumns._ID,
            BaseThingColumns.COLUMN_SCORE,
            BaseThingColumns.COLUMN_LIKES,
            BaseThingColumns.COLUMN_UPS,
            BaseThingColumns.COLUMN_DOWNS,
    };

    private static final int INDEX_SCORE = 1;
    private static final int INDEX_LIKES = 2;
    private static final int INDEX_UPS = 3;
    private static final int INDEX_DOWNS = 4;

    private static final String SELECTION = Things._ID + "=?";

    private SQLiteDatabase db;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext.deleteDatabase(DbHelper.DATABASE_TEST);
        SQLiteOpenHelper dbHelper = new DbHelper(mContext,
                DbHelper.DATABASE_TEST,
                DbHelper.LATEST_VERSION);
        db = dbHelper.getWritableDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mContext.deleteDatabase(DbHelper.DATABASE_TEST);
    }

    public void testExecute() {
        long thingId = insertThing();
        assertTrue(thingId != -1);

        long commentId = insertComment();
        assertTrue(commentId != -1);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_UP, THING_ID);
        assertVote(thingId, 1, VoteActions.ACTION_VOTE_UP, 1, 0);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_UP, THING_ID);
        assertVote(thingId, 2, VoteActions.ACTION_VOTE_UP, 2, 0);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_DOWN, THING_ID);
        assertVote(thingId, 1, VoteActions.ACTION_VOTE_DOWN, 2, 1);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_NEUTRAL, THING_ID);
        assertVote(thingId, 2, VoteActions.ACTION_VOTE_NEUTRAL, 2, 0);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_UP, THING_ID);
        assertVote(thingId, 3, VoteActions.ACTION_VOTE_UP, 3, 0);

        VoteMerger.updateDatabase(db, ACCOUNT_NAME, VoteActions.ACTION_VOTE_NEUTRAL, THING_ID);
        assertVote(thingId, 2, VoteActions.ACTION_VOTE_NEUTRAL, 2, 0);
    }

    private long insertThing() {
        ContentValues v = new ContentValues();
        v.put(Things.COLUMN_ACCOUNT, ACCOUNT_NAME);
        v.put(Things.COLUMN_THING_ID, THING_ID);
        return db.insert(Things.TABLE_NAME, null, v);
    }

    private long insertComment() {
        ContentValues v = new ContentValues();
        v.put(Comments.COLUMN_ACCOUNT, ACCOUNT_NAME);
        v.put(Comments.COLUMN_THING_ID, THING_ID);
        return db.insert(Comments.TABLE_NAME, null, v);
    }

    private void assertVote(long id, int score, int likes, int ups, int downs) {
        for (String table : TABLES) {
            Cursor cursor = getCursor(table, id);
            assertTrue(table, cursor.moveToNext());
            assertEquals(table, score, cursor.getInt(INDEX_SCORE));
            assertEquals(table, likes, cursor.getInt(INDEX_LIKES));
            assertEquals(table, ups, cursor.getInt(INDEX_UPS));
            assertEquals(table, downs, cursor.getInt(INDEX_DOWNS));
            cursor.close();
        }
    }

    private Cursor getCursor(String tableName, long id) {
        return db.query(tableName, PROJECTION, SELECTION, Array.of(id), null, null, null);
    }
}
