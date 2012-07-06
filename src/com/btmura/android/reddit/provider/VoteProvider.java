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

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

public class VoteProvider extends BaseProvider {

    public static final String TAG = "VoteProvider";
    public static boolean DEBUG = Debug.DEBUG;

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.votes";

    private static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ALL_VOTES = 1;
    private static final int MATCH_ONE_VOTE = 2;
    static {
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME, MATCH_ALL_VOTES);
        MATCHER.addURI(AUTHORITY, Votes.TABLE_NAME + "/#", MATCH_ONE_VOTE);
    }

    public static class Votes implements BaseColumns, SyncColumns {
        static final String TABLE_NAME = "votes";
        public static final Uri CONTENT_URI = Uri.parse(BASE_AUTHORITY_URI + TABLE_NAME);

        static final String MIME_TYPE_DIR = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                + AUTHORITY + "." + TABLE_NAME;
        static final String MIME_TYPE_ITEM = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
                + AUTHORITY + "." + TABLE_NAME;

        public static final String COLUMN_ACCOUNT = "account";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_VOTE = "vote";
        public static final String COLUMN_STATE = "state";

        public static final int VOTE_UP = 1;
        public static final int VOTE_RESCIND = 0;
        public static final int VOTE_DOWN = -1;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.query(Votes.TABLE_NAME,
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
        long id = db.insert(Votes.TABLE_NAME, null, values);
        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.update(Votes.TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ONE_VOTE:
                selection = appendIdSelection(selection);
                selectionArgs = appendIdSelectionArg(selectionArgs, uri.getLastPathSegment());
                break;
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        int count = db.delete(Votes.TABLE_NAME, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = MATCHER.match(uri);
        switch (match) {
            case MATCH_ALL_VOTES:
                return Votes.MIME_TYPE_DIR;

            case MATCH_ONE_VOTE:
                return Votes.MIME_TYPE_ITEM;

            default:
                return null;
        }
    }

    public static void insertMultipleVotesInBackground(final Context context,
            final String accountName, final String[] names, final int vote) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(names.length);
                for (int i = 0; i < names.length; i++) {
                    if (DEBUG) {
                        Log.d(TAG, "n:" + names[i]);
                    }
                    ops.add(ContentProviderOperation.newInsert(Votes.CONTENT_URI)
                            .withValue(Votes.COLUMN_ACCOUNT, accountName)
                            .withValue(Votes.COLUMN_NAME, names[i])
                            .withValue(Votes.COLUMN_VOTE, vote)
                            .withValue(Votes.COLUMN_STATE, Votes.STATE_INSERTING)
                            .build());
                }

                ContentResolver cr = context.getContentResolver();
                try {
                    ContentProviderResult[] results = cr.applyBatch(VoteProvider.AUTHORITY, ops);
                    for (int i = 0; i < results.length; i++) {
                        Intent intent = new Intent(context, SyncOperationService.class);
                        intent.setData(results[i].uri);
                        context.startService(intent);
                    }
                    return results.length;
                } catch (RemoteException e) {
                    Log.e(TAG, "insertMultipleVotesInBackground", e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, "insertMultipleVotesInBackground", e);
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer count) {
                super.onPostExecute(count);
                int resId;
                switch (vote) {
                    case Votes.VOTE_UP:
                        resId = R.plurals.votes_up;
                        break;

                    case Votes.VOTE_RESCIND:
                        resId = R.plurals.votes_rescind;
                        break;

                    case Votes.VOTE_DOWN:
                        resId = R.plurals.votes_down;
                        break;

                    default:
                        throw new IllegalStateException();

                }
                String text = context.getResources().getQuantityString(resId, count, count);
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }
}
