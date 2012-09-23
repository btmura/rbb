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

import java.util.ArrayList;

import android.app.backup.BackupManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;

public class SubredditProvider extends BaseProvider {

    public static final String TAG = "SubredditProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subreddits";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri CONTENT_URI = Uri.parse(SubredditProvider.BASE_AUTHORITY_URI
            + Subreddits.TABLE_NAME);

    /** Sync changes back to the network. Don't set this in sync adapters. */
    public static final String PARAM_SYNC = "syncToNetwork";

    static final String MIME_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
            + SubredditProvider.AUTHORITY + "." + Subreddits.TABLE_NAME;
    static final String MIME_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
            + SubredditProvider.AUTHORITY + "." + Subreddits.TABLE_NAME;

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_SUBREDDITS = 1;
    private static final int MATCH_ONE_SUBREDDIT = 2;
    static {
        MATCHER.addURI(AUTHORITY, Subreddits.TABLE_NAME, MATCH_ALL_SUBREDDITS);
        MATCHER.addURI(AUTHORITY, Subreddits.TABLE_NAME + "/#", MATCH_ONE_SUBREDDIT);
    }

    public static final String SELECTION_ACCOUNT = Subreddits.COLUMN_ACCOUNT + "= ?";

    public static final String SELECTION_ACCOUNT_NOT_DELETED = SELECTION_ACCOUNT + " AND "
            + Subreddits.COLUMN_STATE + "!= " + Subreddits.STATE_DELETING;

    public static final String SELECTION_ACCOUNT_AND_NAME = SELECTION_ACCOUNT + " AND "
            + Subreddits.COLUMN_NAME + "= ?";

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_SUBREDDIT:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(Subreddits.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
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
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_SUBREDDIT:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Subreddits.TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_SUBREDDIT:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Subreddits.TABLE_NAME, selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_SUBREDDITS:
                return MIME_TYPE_DIR;

            case MATCH_ONE_SUBREDDIT:
                return MIME_TYPE_ITEM;

            default:
                return null;
        }
    }

    private void notifyChange(Uri uri) {
        boolean sync = uri.getBooleanQueryParameter(PARAM_SYNC, false);
        getContext().getContentResolver().notifyChange(uri, null, sync);
    }

    public static void addInBackground(final Context context, final String accountName,
            final String subredditName) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(2);
                ops.add(ContentProviderOperation.newDelete(CONTENT_URI)
                        .withSelection(SELECTION_ACCOUNT_AND_NAME, new String[] {
                                accountName,
                                subredditName,
                        })
                        .build());
                ops.add(ContentProviderOperation.newInsert(CONTENT_URI)
                        .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                        .withValue(Subreddits.COLUMN_NAME, subredditName)
                        .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_INSERTING)
                        .build());

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(SubredditProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "addInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "addInBackground", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, 1);
                scheduleBackup(context, accountName);
            }
        }.execute();
    }

    public static void deleteInBackground(final Context context, final String accountName,
            final long[] ids) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int count = ids.length;
                Uri[] uris = new Uri[count];
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(
                        count);
                for (int i = 0; i < count; i++) {
                    uris[i] = ContentUris.withAppendedId(CONTENT_URI, ids[i]);
                    ops.add(ContentProviderOperation.newUpdate(uris[i])
                            .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_DELETING)
                            .build());
                }

                ContentResolver cr = context.getContentResolver();
                try {
                    cr.applyBatch(SubredditProvider.AUTHORITY, ops);
                } catch (RemoteException e) {
                    Log.e(TAG, "deleteInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "deleteInBackground", e);
                }
                return count;
            }

            @Override
            protected void onPostExecute(Integer count) {
                showChangeToast(context, -count);
                scheduleBackup(context, accountName);
            }
        }.execute();
    }


    public static void addMultipleSubredditsInBackground(final Context context,
            final ContentValues[] values) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ContentResolver cr = context.getContentResolver();
                cr.bulkInsert(CONTENT_URI, values);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                showChangeToast(context, values.length);
                scheduleBackup(context, null);
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

    private static void scheduleBackup(Context context, String accountName) {
        if (TextUtils.isEmpty(accountName)) {
            new BackupManager(context).dataChanged();
        }
    }
}
