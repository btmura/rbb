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

package com.btmura.android.reddit.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.btmura.android.reddit.database.DbHelper;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.util.Array;

public class DbHelperTest extends AndroidTestCase {

    private static final String[] PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_ACCOUNT,
            Subreddits.COLUMN_NAME,
            Subreddits.COLUMN_STATE,
            Subreddits.COLUMN_EXPIRATION,
    };

    private static final String[] TABLES_V1 = {
            Subreddits.TABLE_NAME,
    };

    private static final String[] TABLES_V2 = {
            Accounts.TABLE_NAME,
            Sessions.TABLE_NAME,
            Subreddits.TABLE_NAME,
            Things.TABLE_NAME,
            CommentActions.TABLE_NAME,
            VoteActions.TABLE_NAME,
            SaveActions.TABLE_NAME,
            Messages.TABLE_NAME,
            MessageActions.TABLE_NAME,
            ReadActions.TABLE_NAME,
    };

    private static final String[] TABLES_V3 = {
            Accounts.TABLE_NAME,
            Sessions.TABLE_NAME,
            Subreddits.TABLE_NAME,
            Things.TABLE_NAME,
            CommentActions.TABLE_NAME,
            VoteActions.TABLE_NAME,
            SaveActions.TABLE_NAME,
            Messages.TABLE_NAME,
            MessageActions.TABLE_NAME,
            ReadActions.TABLE_NAME,
            HideActions.TABLE_NAME,
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

    public void testOnCreate_v2() {
        DbHelper helper = createHelperVersion(2);
        assertTablesExist(helper.getReadableDatabase(), TABLES_V2);
        helper.close();
    }

    public void testOnCreate_v3() {
        DbHelper helper = createHelperVersion(3);
        assertTablesExist(helper.getReadableDatabase(), TABLES_V3);
        helper.close();
    }

    public void testOnUpgrade() {
        DbHelper helper = createHelperVersion(1);
        assertTablesExist(helper.getReadableDatabase(), TABLES_V1);
        helper.close();

        helper = createHelperVersion(2);
        assertTablesExist(helper.getReadableDatabase(), TABLES_V2);
        helper.close();

        helper = createHelperVersion(3);
        assertTablesExist(helper.getReadableDatabase(), TABLES_V3);
        helper.close();
    }

    public void testSubreddits_defaults() {
        DbHelper helper = createHelperVersion(2);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Subreddits.COLUMN_NAME, "Android");
        long id = db.insert(Subreddits.TABLE_NAME, null, values);

        Cursor cursor = db.query(Subreddits.TABLE_NAME, PROJECTION,
                SubredditProvider.ID_SELECTION, Array.of(id), null, null, null);
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

    private DbHelper createHelperVersion(int version) {
        return new DbHelper(mContext, DbHelper.DATABASE_TEST, version);
    }

    // TODO: Improve test to fail if there are unexpected tables.
    private void assertTablesExist(SQLiteDatabase db, String[] tables) {
        for (String table : tables) {
            assertTableExists(db, table);
        }
    }

    private void assertTableExists(SQLiteDatabase db, String table) {
        assertNotNull("Missing table: " + table,
                db.query(table, null, null, null, null, null, null));
    }
}
