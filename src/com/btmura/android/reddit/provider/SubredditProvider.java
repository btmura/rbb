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
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Array;

public class SubredditProvider extends SessionProvider {

    public static final String TAG = "SubredditProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subreddits";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_SUBREDDITS = "subreddits";
    public static final Uri SUBREDDITS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SUBREDDITS);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDITS = 1;
    static {
        MATCHER.addURI(AUTHORITY, PATH_SUBREDDITS, MATCH_SUBREDDITS);
    }

    public SubredditProvider() {
        super(TAG);
    }

    protected String getTable(Uri uri) {
        return Subreddits.TABLE_NAME;
    }

    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String[] selectionArgs) {
    }

    public static void insertInBackground(Context context, String accountName,
            String... subreddits) {
        modifyInBackground(context, accountName, subreddits, true);
    }

    public static void deleteInBackground(Context context, String accountName,
            String... subreddits) {
        modifyInBackground(context, accountName, subreddits, false);
    }

    private static void modifyInBackground(Context context, final String accountName,
            final String[] subreddits, final boolean add) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Only trigger a sync on real accounts.
                Uri syncUri = SUBREDDITS_URI;
                if (AccountUtils.isAccount(accountName)) {
                    syncUri = syncUri.buildUpon()
                            .appendQueryParameter(PARAM_SYNC, Boolean.toString(true))
                            .build();
                }

                int count = subreddits.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count * 2);
                int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;
                for (int i = 0; i < count; i++) {
                    ops.add(ContentProviderOperation.newDelete(syncUri)
                            .withSelection(Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                                    Array.of(accountName, subreddits[i]))
                            .build());

                    // Don't insert deletion rows for app storage account,
                    // since they don't need to be synced back.
                    if (AccountUtils.isAccount(accountName) || add) {
                        ops.add(ContentProviderOperation.newInsert(syncUri)
                                .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                                .withValue(Subreddits.COLUMN_NAME, subreddits[i])
                                .withValue(Subreddits.COLUMN_STATE, state)
                                .build());
                    }
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
