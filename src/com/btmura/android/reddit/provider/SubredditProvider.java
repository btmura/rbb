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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Array;

public class SubredditProvider extends BaseProvider {

    public static final String TAG = "SubredditProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.subreddits";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_SUBREDDITS = "subreddits";
    public static final Uri SUBREDDITS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_SUBREDDITS);
    public static final Uri SUBREDDITS_SYNC_URI = makeSyncUri(SUBREDDITS_URI);

    public SubredditProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        return Subreddits.TABLE_NAME;
    }

    public static void addSubredditAsync(Context context,
            String accountName,
            String... subreddits) {
        changeSubredditAsync(context, accountName, subreddits, true);
    }

    public static void removeSubredditAsync(Context context,
            String accountName,
            String... subreddits) {
        changeSubredditAsync(context, accountName, subreddits, false);
    }

    private static void changeSubredditAsync(Context context,
            final String accountName,
            final String[] subreddits,
            final boolean add) {
        final Context appContext = context.getApplicationContext();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... fillTheVoid) {
                // Only trigger a sync on real accounts.
                Uri uri;
                if (AccountUtils.isAccount(accountName)) {
                    uri = SubredditProvider.SUBREDDITS_SYNC_URI;
                } else {
                    uri = SubredditProvider.SUBREDDITS_URI;
                }

                int count = subreddits.length;
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>(count);
                int state = add ? Subreddits.STATE_INSERTING : Subreddits.STATE_DELETING;
                for (int i = 0; i < count; i++) {
                    // All subreddit additions require an insert. The insert
                    // would remove any deletes due to table constraints.
                    //
                    // Deletes for an account require an insert with delete
                    // state. However, app storage accounts should just
                    // remove the row altogether.
                    if (add || AccountUtils.isAccount(accountName)) {
                        ops.add(ContentProviderOperation.newInsert(uri)
                                .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                                .withValue(Subreddits.COLUMN_NAME, subreddits[i])
                                .withValue(Subreddits.COLUMN_STATE, state)
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newDelete(uri)
                                .withSelection(Subreddits.SELECT_BY_ACCOUNT_AND_NAME,
                                        Array.of(accountName, subreddits[i]))
                                .build());
                    }
                }

                return Provider.applyOps(appContext, SubredditProvider.AUTHORITY, ops);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Provider.scheduleBackup(appContext, accountName);
                }
            }
        }.execute();
    }
}
