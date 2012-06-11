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
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.SyncTasks;

class AccountSubredditProvider {

    public static String TAG = "AccountSubredditProvider";

    private static final String[] SYNC_TASKS_PROJECTION = new String[] {
            SyncTasks.COLUMN_NAME,
            SyncTasks.COLUMN_TYPE,
            SyncTasks.COLUMN_EXPIRATION,
    };
    private static final int INDEX_NAME = 0;
    private static final int INDEX_TYPE = 1;

    private static final String SELECTION = SyncTasks.COLUMN_ACCOUNT_ID + "= ?";
    private static final String SORT = SyncTasks._ID + " ASC";

    static Cursor query(Uri uri, SQLiteDatabase db) {
        List<String> segments = uri.getPathSegments();
        long accountId = Long.parseLong(segments.get(1));
        String[] credentials = Credentials.getCredentials(db, accountId);

        // 1. Ask reddit.com what subreddits the account is subscribed to.
        ArrayList<Subreddit> subreddits = null;
        try {
            subreddits = NetApi.query(credentials[0]);
        } catch (IOException e) {
            Log.e(TAG, "query", e);
            return null;
        }

        // 2. Query the database for local inserts and deletes.
        Cursor c = db.query(SyncTasks.TABLE_NAME,
                SYNC_TASKS_PROJECTION,
                SELECTION,
                new String[] {Long.toString(accountId)},
                null,
                null,
                SORT);

        // 3. Add local inserts and remove local deletes.
        boolean modified = false;
        while (c.moveToNext()) {
            String name = c.getString(INDEX_NAME);
            switch (c.getInt(INDEX_TYPE)) {
                case SyncTasks.TYPE_INSERT:
                    modified |= insertSubreddit(subreddits, name);
                    break;

                case SyncTasks.TYPE_DELETE:
                    deleteSubreddit(subreddits, name);
                    break;

                default:
                    throw new IllegalStateException();
            }
        }
        c.close();

        if (modified) {
            Collections.sort(subreddits);
        }

        return new SubredditCursor(subreddits);
    }

    private static boolean insertSubreddit(ArrayList<Subreddit> subreddits, String name) {
        int count = subreddits.size();
        for (int i = 0; i < count; i++) {
            String subreddit = subreddits.get(i).name;
            if (name.equalsIgnoreCase(subreddit)) {
                return false;
            }
        }
        subreddits.add(Subreddit.newInstance(name));
        return true;
    }

    private static boolean deleteSubreddit(ArrayList<Subreddit> subreddits, String name) {
        int count = subreddits.size();
        for (int i = 0; i < count; i++) {
            String subreddit = subreddits.get(i).name;
            if (name.equalsIgnoreCase(subreddit)) {
                subreddits.remove(i);
                return true;
            }
        }
        return false;
    }

    static int insert(Context context, long accountId, String subreddit) {
        queueSubscription(context, accountId, subreddit, true);
        return 0;
    }

    static int delete(Context context, long accountId, String subreddit) {
        queueSubscription(context, accountId, subreddit, false);
        return 1;
    }

    private static void queueSubscription(Context context, long accountId, String subreddit, boolean subscribe) {
        ContentValues values = new ContentValues(3);
        values.put(SyncTasks.COLUMN_ACCOUNT_ID, accountId);
        values.put(SyncTasks.COLUMN_NAME, subreddit);
        values.put(SyncTasks.COLUMN_TYPE, subscribe ? SyncTasks.TYPE_INSERT : SyncTasks.TYPE_DELETE);

        ContentResolver cr = context.getContentResolver();
        Uri taskUri = cr.insert(SyncTasks.CONTENT_URI, values);

        Intent intent = new Intent(context, SyncService.class);
        intent.setData(taskUri);
        context.startService(intent);
    }
}
