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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.backup.BackupManager;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.SubredditSearches;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Array;

public class SubredditProvider extends SessionProvider {

    public static final String TAG = "SubredditProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subreddits";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_SUBREDDITS = "subreddits";
    static final String PATH_SEARCHES = "searches";

    public static final Uri SUBREDDITS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SUBREDDITS);
    public static final Uri SEARCHES_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SEARCHES);

    // Query parameters related to fetching search results before querying.
    public static final String SYNC_ENABLE = "sync";
    public static final String SYNC_ACCOUNT = "accountName";
    public static final String SYNC_SESSION_ID = "sessionId";
    public static final String SYNC_QUERY = "query";

    /** Sync changes back to the network. Don't set this in sync adapters. */
    public static final String PARAM_SYNC = "syncToNetwork";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDITS = 1;
    private static final int MATCH_SEARCHES = 2;
    static {
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDITS, MATCH_SUBREDDITS);
        MATCHER.addURI(AUTHORITY, PATH_SEARCHES, MATCH_SEARCHES);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        processQueryUri(uri);
        String tableName = getTableName(uri);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "query table: " + tableName);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    private void processQueryUri(Uri uri) {
        if (MATCHER.match(uri) != MATCH_SEARCHES
                || !uri.getBooleanQueryParameter(PARAM_SYNC, false)) {
            return;
        }

        try {
            // Determine the cutoff first to avoid deleting synced data.
            long timestampCutoff = getSessionTimestampCutoff();
            long sessionTimestamp = System.currentTimeMillis();

            String accountName = uri.getQueryParameter(SYNC_ACCOUNT);
            String sessionId = uri.getQueryParameter(SYNC_SESSION_ID);
            String query = uri.getQueryParameter(SYNC_QUERY);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            SubredditSearchListing listing = SubredditSearchListing.get(context, accountName,
                    sessionId, sessionTimestamp, query, cookie);

            long cleaned;
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                // Delete old results that can't be possibly viewed anymore.
                cleaned = db.delete(SubredditSearches.TABLE_NAME,
                        SubredditSearches.SELECT_BEFORE_TIMESTAMP,
                        Array.of(timestampCutoff));
                InsertHelper insertHelper = new InsertHelper(db, SubredditSearches.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "cleaned: " + cleaned);
            }
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private String getTableName(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_SUBREDDITS:
                return Subreddits.TABLE_NAME;

            case MATCH_SEARCHES:
                return SubredditSearches.TABLE_NAME;

            default:
                throw new IllegalArgumentException("uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = db.insert(Subreddits.TABLE_NAME, null, values);
        if (id != -1) {
            notifyChange(uri);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(getTableName(uri), values, selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(getTableName(uri), selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private void notifyChange(Uri uri) {
        boolean sync = uri.getBooleanQueryParameter(PARAM_SYNC, false);
        getContext().getContentResolver().notifyChange(uri, null, sync);
    }

    public static void
            insertInBackground(Context context, String accountName, String... subreddits) {
        modifyInBackground(context, accountName, subreddits, true);
    }

    public static void
            deleteInBackground(Context context, String accountName, String... subreddits) {
        modifyInBackground(context, accountName, subreddits, false);
    }

    private static void modifyInBackground(Context context, final String accountName,
            final String[] subreddits, final boolean add) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Uri syncUri = SUBREDDITS_URI.buildUpon()
                        .appendQueryParameter(PARAM_SYNC, Boolean.toString(true))
                        .build();

                int count = subreddits.length;
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        count * 2);
                int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newDelete(syncUri)
                            .withSelection(Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                                    Array.of(accountName, subreddits[i]))
                            .build());
                    ops.add(ContentProviderOperation.newInsert(syncUri)
                            .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                            .withValue(Subreddits.COLUMN_NAME, subreddits[i])
                            .withValue(Subreddits.COLUMN_STATE, state)
                            .build());
                }

                try {
                    appContext.getContentResolver().applyBatch(SubredditProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void intoTheVoid) {
                showChangeToast(appContext, add, subreddits.length);
                scheduleBackup(appContext, accountName);
            }
        }.execute();
    }

    private static void showChangeToast(Context context, boolean added, int count) {
        int resId = added ? R.plurals.subreddits_added : R.plurals.subreddits_deleted;
        CharSequence text = context.getResources().getQuantityString(resId, count, count);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private static void scheduleBackup(Context context, String accountName) {
        if (!AccountUtils.isAccount(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }
}
