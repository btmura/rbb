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

package com.btmura.android.reddit.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.util.Array;

public class SubredditSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "SubredditSyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new SubredditSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    private static final String[] PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME,
            Subreddits.COLUMN_STATE,
            Subreddits.COLUMN_EXPIRATION,
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_NAME = 1;
    private static final int INDEX_STATE = 2;
    private static final int INDEX_EXPIRATION = 3;

    private static final int NUM_OPS = 3;
    private static final int OP_INSERTS = 0;
    private static final int OP_UPDATES = 1;
    private static final int OP_DELETES = 2;

    private static final long EXPIRATION_PADDING_MS = TimeUnit.MINUTES.toMillis(5);

    private static final long POLL_FREQUENCY_SECONDS = TimeUnit.HOURS.toSeconds(3);

    public SubredditSyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // Extra method just allows us to always print out sync stats after.
        doSync(account, extras, authority, provider, syncResult);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "accountName: " + account.name + " syncResult: " + syncResult.toString());
        }

        // Always schedule the next sync to get new subreddits added.
        ContentResolver.addPeriodicSync(account, authority, Bundle.EMPTY, POLL_FREQUENCY_SECONDS);
    }

    private void doSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            // Get cookie and modhash needed to subscribe to subreddits.
            AccountManager manager = AccountManager.get(getContext());
            String cookie = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_COOKIE, true);
            String modhash = manager.blockingGetAuthToken(account,
                    AccountAuthenticator.AUTH_TOKEN_MODHASH, true);

            // Quit if there are authentication issues.
            if (cookie == null || modhash == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            // Get subreddits from reddit. These could be a bit stale.
            ArrayList<String> subreddits = RedditApi.getSubreddits(cookie);

            // Get database operations required to sync with the server.
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            int[] opCounts = new int[NUM_OPS];
            syncDatabase(account, provider, cookie, modhash, subreddits, ops, opCounts, syncResult);

            // Apply all database ops and then update the stats on success.
            getContext().getContentResolver().applyBatch(SubredditProvider.AUTHORITY, ops);
            syncResult.stats.numInserts += opCounts[OP_INSERTS];
            syncResult.stats.numUpdates += opCounts[OP_UPDATES];
            syncResult.stats.numDeletes += opCounts[OP_DELETES];

        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numIoExceptions++;
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        }
    }

    private void syncDatabase(Account account, ContentProviderClient provider, String cookie,
            String modhash, ArrayList<String> subreddits, ArrayList<ContentProviderOperation> ops,
            int[] opCounts, SyncResult syncResult) throws RemoteException {
        Cursor c = provider.query(SubredditProvider.SUBREDDITS_URI, PROJECTION,
                Subreddits.SELECT_BY_ACCOUNT, Array.of(account.name), null);
        while (c.moveToNext()) {
            syncRow(c, cookie, modhash, subreddits, ops, opCounts, syncResult);
        }
        c.close();
        insertSubredditOps(account, subreddits, ops, opCounts);
    }

    private void syncRow(Cursor c, String cookie, String modhash, ArrayList<String> subreddits,
            ArrayList<ContentProviderOperation> ops, int[] opCounts, SyncResult syncResult) {
        long id = c.getLong(INDEX_ID);
        String name = c.getString(INDEX_NAME);
        long expiration = c.getLong(INDEX_EXPIRATION);
        int state = c.getInt(INDEX_STATE);

        // Don't sync subreddits that can't be like front page, random, and all.
        if (!Subreddits.isSyncable(name)) {
            return;
        }

        // Remove subreddit from the list, since it's now new.
        String found = find(subreddits, name);
        if (found != null) {
            subreddits.remove(found);
        }

        switch (state) {
            case Subreddits.STATE_NORMAL:
                if (found != null) {
                    if (!name.equals(found)) {
                        ops.add(ContentProviderOperation.newUpdate(SubredditProvider.SUBREDDITS_URI)
                                .withSelection(SubredditProvider.ID_SELECTION, Array.of(id))
                                .withValue(Subreddits.COLUMN_NAME, found)
                                .build());
                        opCounts[OP_UPDATES]++;
                    }
                } else {
                    ops.add(ContentProviderOperation.newDelete(SubredditProvider.SUBREDDITS_URI)
                            .withSelection(SubredditProvider.ID_SELECTION, Array.of(id))
                            .build());
                    opCounts[OP_DELETES]++;
                }
                break;

            case Subreddits.STATE_INSERTING:
            case Subreddits.STATE_DELETING:
                if (expiration == 0) {
                    try {
                        boolean subscribe = state == Subreddits.STATE_INSERTING;
                        RedditApi.subscribe(name, subscribe, cookie, modhash);
                        long newExpiration = System.currentTimeMillis() + EXPIRATION_PADDING_MS;
                        ops.add(ContentProviderOperation.newUpdate(SubredditProvider.SUBREDDITS_URI)
                                .withSelection(SubredditProvider.ID_SELECTION, Array.of(id))
                                .withValue(Subreddits.COLUMN_EXPIRATION, newExpiration)
                                .build());
                        opCounts[OP_UPDATES]++;
                    } catch (IOException e) {
                        syncResult.stats.numIoExceptions++;
                    }
                } else if (System.currentTimeMillis() >= expiration) {
                    if (state == Subreddits.STATE_INSERTING) {
                        ops.add(ContentProviderOperation.newUpdate(SubredditProvider.SUBREDDITS_URI)
                                .withSelection(SubredditProvider.ID_SELECTION, Array.of(id))
                                .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL)
                                .withValue(Subreddits.COLUMN_EXPIRATION, 0)
                                .build());
                        opCounts[OP_UPDATES]++;
                    } else if (state == Subreddits.STATE_DELETING) {
                        ops.add(ContentProviderOperation.newDelete(SubredditProvider.SUBREDDITS_URI)
                                .withSelection(SubredditProvider.ID_SELECTION, Array.of(id))
                                .build());
                        opCounts[OP_DELETES]++;
                    }
                }
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private void insertSubredditOps(Account account, ArrayList<String> subreddits,
            ArrayList<ContentProviderOperation> ops, int[] opCounts) {
        int size = subreddits.size();
        for (int i = 0; i < size; i++) {
            ops.add(ContentProviderOperation.newInsert(SubredditProvider.SUBREDDITS_URI)
                    .withValue(Subreddits.COLUMN_ACCOUNT, account.name)
                    .withValue(Subreddits.COLUMN_NAME, subreddits.get(i))
                    .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL)
                    .withValue(Subreddits.COLUMN_EXPIRATION, 0)
                    .build());
            opCounts[OP_INSERTS]++;
        }
    }

    /** @return subreddit from list that matches name ignoring case */
    private static String find(List<String> subreddits, String name) {
        int count = subreddits.size();
        for (int i = 0; i < count; i++) {
            if (name.equalsIgnoreCase(subreddits.get(i))) {
                return subreddits.get(i);
            }
        }
        return null;
    }

    public static void initializeAccount(Context context, String login, String cookie)
            throws RemoteException, OperationApplicationException, IOException {
        ArrayList<String> subreddits = RedditApi.getSubreddits(cookie);
        int count = subreddits.size();

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(count + 2);
        ops.add(ContentProviderOperation.newDelete(SubredditProvider.SUBREDDITS_URI)
                .withSelection(Subreddits.SELECT_BY_ACCOUNT, Array.of(login))
                .build());
        for (int i = 0; i < count; i++) {
            ops.add(ContentProviderOperation.newInsert(SubredditProvider.SUBREDDITS_URI)
                    .withValue(Subreddits.COLUMN_ACCOUNT, login)
                    .withValue(Subreddits.COLUMN_NAME, subreddits.get(i))
                    .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL)
                    .build());
        }
        ContentResolver cr = context.getContentResolver();
        cr.applyBatch(SubredditProvider.AUTHORITY, ops);
    }
}