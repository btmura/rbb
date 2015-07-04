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

import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_DOWNS;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_LIKES;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_SCORE;
import static com.btmura.android.reddit.database.BaseThingColumns.COLUMN_UPS;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_DOWN;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_NEUTRAL;
import static com.btmura.android.reddit.database.VoteActions.ACTION_VOTE_UP;

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

  public void testUpdateContentValues() {
    ContentValues thingValues = newThingValues();

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_UP);
    assertContentValues(thingValues, 1, ACTION_VOTE_UP, 1, 0);

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_UP);
    assertContentValues(thingValues, 2, ACTION_VOTE_UP, 2, 0);

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_DOWN);
    assertContentValues(thingValues, 1, ACTION_VOTE_DOWN, 2, 1);

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_NEUTRAL);
    assertContentValues(thingValues, 2, ACTION_VOTE_NEUTRAL, 2, 0);

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_UP);
    assertContentValues(thingValues, 3, ACTION_VOTE_UP, 3, 0);

    VoteMerger.updateContentValues(thingValues, ACTION_VOTE_NEUTRAL);
    assertContentValues(thingValues, 2, ACTION_VOTE_NEUTRAL, 2, 0);
  }

  public void testUpdateDatabase() {
    long thingId = insertThing();
    assertTrue(thingId != -1);

    long commentId = insertComment();
    assertTrue(commentId != -1);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_UP, THING_ID);
    assertTable(thingId, 1, VoteActions.ACTION_VOTE_UP, 1, 0);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_UP, THING_ID);
    assertTable(thingId, 2, VoteActions.ACTION_VOTE_UP, 2, 0);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_DOWN, THING_ID);
    assertTable(thingId, 1, VoteActions.ACTION_VOTE_DOWN, 2, 1);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_NEUTRAL, THING_ID);
    assertTable(thingId, 2, VoteActions.ACTION_VOTE_NEUTRAL, 2, 0);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_UP, THING_ID);
    assertTable(thingId, 3, VoteActions.ACTION_VOTE_UP, 3, 0);

    VoteMerger.updateDatabase(db, ACCOUNT_NAME, ACTION_VOTE_NEUTRAL, THING_ID);
    assertTable(thingId, 2, VoteActions.ACTION_VOTE_NEUTRAL, 2, 0);
  }

  private ContentValues newThingValues() {
    ContentValues v = new ContentValues();
    v.put(Things.COLUMN_ACCOUNT, ACCOUNT_NAME);
    v.put(Things.COLUMN_THING_ID, THING_ID);
    return v;
  }

  private ContentValues newCommentValues() {
    ContentValues v = new ContentValues();
    v.put(Comments.COLUMN_ACCOUNT, ACCOUNT_NAME);
    v.put(Comments.COLUMN_THING_ID, THING_ID);
    return v;
  }

  private void assertContentValues(
      ContentValues v,
      int score,
      int likes,
      int ups,
      int downs) {
    assertValue(score, v, COLUMN_SCORE);
    assertValue(likes, v, COLUMN_LIKES);
    assertValue(ups, v, COLUMN_UPS);
    assertValue(downs, v, COLUMN_DOWNS);
  }

  private void assertValue(int expected, ContentValues v, String key) {
    assertEquals(expected,
        v.containsKey(key) ? v.getAsInteger(key).intValue() : 0);
  }

  private long insertThing() {
    return db.insert(Things.TABLE_NAME, null, newThingValues());
  }

  private long insertComment() {
    return db.insert(Comments.TABLE_NAME, null, newCommentValues());
  }

  private void assertTable(long id, int score, int likes, int ups, int downs) {
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
    return db.query(tableName, PROJECTION, SELECTION, Array.of(id), null, null,
        null);
  }
}
