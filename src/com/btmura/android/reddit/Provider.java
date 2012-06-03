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

package com.btmura.android.reddit;

import java.util.ArrayList;
import java.util.List;

import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

public class Provider extends ContentProvider {

    public static final String TAG = "Provider";

    static final String AUTHORITY = "com.btmura.android.reddit.provider";

    private static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_SUBREDDITS = 1;
    private static final int MATCH_ONE_SUBREDDIT = 2;
    private static final int MATCH_ALL_ACCOUNTS = 3;
    private static final int MATCH_ONE_ACCOUNT = 4;
    private static final int MATCH_ONE_ACCOUNT_SUBREDDITS = 5;
    static {
        MATCHER.addURI(AUTHORITY, Subreddits.TABLE_NAME, MATCH_ALL_SUBREDDITS);
        MATCHER.addURI(AUTHORITY, Subreddits.TABLE_NAME + "/#", MATCH_ONE_SUBREDDIT);
        MATCHER.addURI(AUTHORITY, Accounts.TABLE_NAME, MATCH_ALL_ACCOUNTS);
        MATCHER.addURI(AUTHORITY, Accounts.TABLE_NAME + "/#", MATCH_ONE_ACCOUNT);
        MATCHER.addURI(AUTHORITY, AccountSubreddits.TABLE_NAME + "/#", MATCH_ONE_ACCOUNT_SUBREDDITS);
    }

    public static class Subreddits implements BaseColumns {
        static final String TABLE_NAME = "subreddits";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + TABLE_NAME);
        public static final String COLUMN_NAME = "name";
        public static final String SORT = Subreddits.COLUMN_NAME + " COLLATE NOCASE ASC";
    }

    public static class Accounts implements BaseColumns {
        static final String TABLE_NAME = "accounts";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + TABLE_NAME);
        public static final String COLUMN_LOGIN = "login";
        public static final String COLUMN_COOKIE = "cookie";
        public static final String COLUMN_MODHASH = "modhash";
        public static final String SORT = Accounts.COLUMN_LOGIN + " COLLATE NOCASE ASC";
    }

    /** AccountSubreddits is a fake table that actually queries Reddit. */
    public static class AccountSubreddits {
        static final String TABLE_NAME = "accountSubreddits";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + TABLE_NAME);
        public static final String COLUMN_NAME = Subreddits.COLUMN_NAME;
    }

    static final String ID_SELECTION = BaseColumns._ID + "= ?";

    private DbHelper helper;

    @Override
    public boolean onCreate() {
        helper = new DbHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String tableName;
        Uri notifyUri;

        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_SUBREDDITS:
            case MATCH_ONE_SUBREDDIT:
                tableName = Subreddits.TABLE_NAME;
                notifyUri = Subreddits.CONTENT_URI;
                break;

            case MATCH_ALL_ACCOUNTS:
            case MATCH_ONE_ACCOUNT:
                tableName = Accounts.TABLE_NAME;
                notifyUri = Accounts.CONTENT_URI;
                break;

            case MATCH_ONE_ACCOUNT_SUBREDDITS:
                tableName = AccountSubreddits.TABLE_NAME;
                notifyUri = AccountSubreddits.CONTENT_URI;
                break;

            default:
                return null;
        }

        switch (match) {
            case MATCH_ONE_ACCOUNT:
            case MATCH_ONE_SUBREDDIT:
                selection = ID_SELECTION;
                selectionArgs = new String[] {Long.toString(ContentUris.parseId(uri))};
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = null;
        if (match == MATCH_ONE_ACCOUNT_SUBREDDITS) {
            c = NetProvider.querySubreddits(db, ContentUris.parseId(uri));
        } else {
            c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        }
        c.setNotificationUri(getContext().getContentResolver(), notifyUri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String tableName;
        Uri notifyUri;

        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_SUBREDDITS:
                tableName = Subreddits.TABLE_NAME;
                notifyUri = Subreddits.CONTENT_URI;
                break;

            case MATCH_ALL_ACCOUNTS:
                tableName = Accounts.TABLE_NAME;
                notifyUri = Accounts.CONTENT_URI;
                break;

            case MATCH_ONE_ACCOUNT_SUBREDDITS:
                tableName = AccountSubreddits.TABLE_NAME;
                notifyUri = AccountSubreddits.CONTENT_URI;
                break;

            default:
                throw new IllegalArgumentException(uri.toString());
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long id = -1;
        if (match == MATCH_ONE_ACCOUNT_SUBREDDITS) {
            id = NetProvider.insertSubreddit(db, ContentUris.parseId(uri), values);
        } else {
            id = db.insert(tableName, null, values);
        }
        getContext().getContentResolver().notifyChange(notifyUri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String tableName;
        Uri notifyUri;

        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_SUBREDDITS:
            case MATCH_ONE_SUBREDDIT:
                tableName = Subreddits.TABLE_NAME;
                notifyUri = Subreddits.CONTENT_URI;
                break;

            case MATCH_ALL_ACCOUNTS:
            case MATCH_ONE_ACCOUNT:
                tableName = Accounts.TABLE_NAME;
                notifyUri = Accounts.CONTENT_URI;
                break;

            default:
                return 0;
        }

        switch (match) {
            case MATCH_ONE_SUBREDDIT:
            case MATCH_ONE_ACCOUNT:
                selection = BaseColumns._ID + "= ?";
                selectionArgs = new String[] {Long.toString(ContentUris.parseId(uri))};
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(tableName, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(notifyUri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    public static Uri getSubredditsUri(long accountId) {
        if (accountId > 0) {
            return ContentUris.withAppendedId(AccountSubreddits.CONTENT_URI, accountId);
        } else {
            return Subreddits.CONTENT_URI;
        }
    }

    public static void addInBackground(final Context context, final Uri uri,
            final ContentValues values) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = context.getContentResolver();
                cr.insert(uri, values);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, 1);
                scheduleBackup(context);
            }
        }.execute();
    }

    public static void addMultipleSubredditsInBackground(final Context context,
            final ContentValues[] values) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = context.getContentResolver();
                cr.bulkInsert(Subreddits.CONTENT_URI, values);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, values.length);
                scheduleBackup(context);
            }
        }.execute();
    }

    public static void deleteInBackground(final Context context, final Uri uri, final long[] ids) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                StringBuilder s = new StringBuilder(BaseColumns._ID).append(" IN (");
                int numIds = ids.length;
                for (int i = 0; i < numIds; i++) {
                    s.append(ids[i]);
                    if (i + 1 < numIds) {
                        s.append(", ");
                    }
                }
                s.append(")");
                ContentResolver cr = context.getContentResolver();
                cr.delete(uri, s.toString(), null);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, -ids.length);
                scheduleBackup(context);
            }
        }.execute();
    }

    public static void combineSubredditsInBackground(final Context context,
            final List<String> names, final long[] ids) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int size = names.size();
                StringBuilder combined = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    combined.append(names.get(i));
                    if (i + 1 < size) {
                        combined.append("+");
                    }
                }

                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        ids.length + 1);
                ops.add(ContentProviderOperation.newInsert(Subreddits.CONTENT_URI)
                        .withValue(Subreddits.COLUMN_NAME, combined.toString()).build());

                size = ids.length;
                for (int i = 0; i < size; i++) {
                    ops.add(ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(Subreddits.CONTENT_URI, ids[i])).build());
                }

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(Provider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "combineSubredditsInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "combineSubredditsInBackground", e);
                }
                return size;
            }

            @Override
            protected void onPostExecute(Integer deleted) {
                showChangeToast(context, 1);
                scheduleBackup(context);
            }
        }.execute();
    }

    public static void splitSubredditInBackground(final Context context, final String name,
            final long id) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                String[] parts = name.split("\\+");
                int numParts = parts.length;

                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        numParts + 1);
                for (int i = 0; i < numParts; i++) {
                    ops.add(ContentProviderOperation.newInsert(Subreddits.CONTENT_URI)
                            .withValue(Subreddits.COLUMN_NAME, parts[i]).build());
                }

                ops.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(Subreddits.CONTENT_URI, id)).build());

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(Provider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "splitSubredditInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "splitSubredditInBackground", e);
                }
                return numParts;
            }

            @Override
            protected void onPostExecute(Integer added) {
                showChangeToast(context, added);
                scheduleBackup(context);
            }
        }.execute();
    }

    private static void showChangeToast(Context context, int count) {
        int resId;
        if (count == 1) {
            resId = R.string.subreddit_one_added;
        } else if (count == -1) {
            resId = R.string.subreddit_one_deleted;
        } else if (count >= 0) {
            resId = R.string.subreddit_x_added;
        } else {
            resId = R.string.subreddit_x_deleted;
        }
        Toast.makeText(context, context.getString(resId, Math.abs(count)), Toast.LENGTH_SHORT)
                .show();
    }

    private static void scheduleBackup(Context context) {
        new BackupManager(context).dataChanged();
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    static class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            super(context, "reddit", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                createSubreddits(db);
                createAccounts(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        private void createSubreddits(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Subreddits.TABLE_NAME + " (" + Subreddits._ID
                    + " INTEGER PRIMARY KEY, " + Subreddits.COLUMN_NAME + " TEXT UNIQUE NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX " + Subreddits.COLUMN_NAME + " ON "
                    + Subreddits.TABLE_NAME + " (" + Subreddits.COLUMN_NAME + " ASC)");

            String[] defaultSubreddits = {
                    "",
                    "AdviceAnimals",
                    "announcements",
                    "AskReddit",
                    "askscience",
                    "atheism",
                    "aww",
                    "blog",
                    "funny",
                    "gaming",
                    "IAmA",
                    "movies",
                    "Music",
                    "pics",
                    "politics",
                    "science",
                    "technology",
                    "todayilearned",
                    "videos",
                    "worldnews",
                    "WTF",};

            for (int i = 0; i < defaultSubreddits.length; i++) {
                ContentValues values = new ContentValues(1);
                values.put(Subreddits.COLUMN_NAME, defaultSubreddits[i]);
                db.insert(Subreddits.TABLE_NAME, null, values);
            }
        }

        private void createAccounts(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Accounts.TABLE_NAME + " (" + Accounts._ID
                    + " INTEGER PRIMARY KEY, " + Accounts.COLUMN_LOGIN + " TEXT UNIQUE NOT NULL, "
                    + Accounts.COLUMN_COOKIE + " TEXT UNIQUE NOT NULL, " + Accounts.COLUMN_MODHASH
                    + " TEXT UNIQUE NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX " + Accounts.COLUMN_LOGIN + " ON "
                    + Accounts.TABLE_NAME + " (" + Accounts.COLUMN_LOGIN + " ASC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 2) {
                createAccounts(db);
            }
        }
    }
}
