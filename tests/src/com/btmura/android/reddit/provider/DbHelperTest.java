package com.btmura.android.reddit.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.btmura.android.reddit.provider.Provider.Subreddits;

public class DbHelperTest extends AndroidTestCase {

    private static final String[] PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_ACCOUNT,
            Subreddits.COLUMN_NAME,
            Subreddits.COLUMN_STATE,
            Subreddits.COLUMN_EXPIRATION,
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext.deleteDatabase(DbHelper.DATABASE_TEST);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mContext.deleteDatabase(DbHelper.DATABASE_TEST);
    }

    public void testSubreddits_defaults() {
        DbHelper helper = createHelperVersion(2);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Subreddits.COLUMN_NAME, "Android");
        long id = db.insert(Subreddits.TABLE_NAME, null, values);

        Cursor cursor = db.query(Subreddits.TABLE_NAME, PROJECTION, Provider.ID_SELECTION,
                new String[] {Long.toString(id)}, null, null, null);
        try {
            assertTrue(cursor.moveToNext());
            assertEquals(id, cursor.getLong(0));
            assertEquals("", cursor.getString(1));
            assertEquals("Android", cursor.getString(2));
            assertEquals(0, cursor.getInt(3));
            assertEquals(0, cursor.getLong(4));
        } finally {
            cursor.close();
        }
    }

    public void testSubreddits_differentAccountsDuplicateSubreddits() {
        DbHelper helper = createHelperVersion(2);
        SQLiteDatabase db = helper.getWritableDatabase();

        int count = db.delete(Subreddits.TABLE_NAME, null, null);
        assertTrue("Table should not be empty.", count > 0);

        ContentValues values = new ContentValues();
        values.put(Subreddits.COLUMN_NAME, "Android");
        long id = db.insert(Subreddits.TABLE_NAME, null, values);
        assertTrue(id != -1);

        values.put(Subreddits.COLUMN_ACCOUNT, "btmura");
        long id2 = db.insert(Subreddits.TABLE_NAME, null, values);
        assertTrue(id2 != -1);

        helper.close();
    }

    public void testSubreddits_noAccountDuplicateSubreddits() {
        DbHelper helper = createHelperVersion(2);
        SQLiteDatabase db = helper.getWritableDatabase();

        int count = db.delete(Subreddits.TABLE_NAME, null, null);
        assertTrue("Table should not be empty.", count > 0);

        ContentValues values = new ContentValues();
        values.put(Subreddits.COLUMN_NAME, "Android");
        long id = db.insert(Subreddits.TABLE_NAME, null, values);
        assertTrue(id != -1);

        long id2 = db.insert(Subreddits.TABLE_NAME, null, values);
        assertEquals(-1, id2);

        helper.close();
    }

    public void testSubreddits_someAccountDuplicateSubreddits() {
        DbHelper helper = createHelperVersion(2);
        SQLiteDatabase db = helper.getWritableDatabase();

        int count = db.delete(Subreddits.TABLE_NAME, null, null);
        assertTrue("Table should not be empty.", count > 0);

        ContentValues values = new ContentValues();
        values.put(Subreddits.COLUMN_ACCOUNT, "btmura");
        values.put(Subreddits.COLUMN_NAME, "Android");
        long id = db.insert(Subreddits.TABLE_NAME, null, values);
        assertTrue(id != -1);

        long id2 = db.insert(Subreddits.TABLE_NAME, null, values);
        assertEquals(-1, id2);

        helper.close();
    }

    public void testOnUpgrade_v1ToV2() {
        DbHelper helper = createHelperVersion(1);
        helper.getWritableDatabase();
        helper.close();

        helper = createHelperVersion(2);
        helper.getWritableDatabase();
        helper.close();
    }

    private DbHelper createHelperVersion(int version) {
        return new DbHelper(mContext, DbHelper.DATABASE_TEST, version);
    }
}
