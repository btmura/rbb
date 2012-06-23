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
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
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

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.provider.SubredditProvider.Subreddits;

public class SyncAdapterService extends Service {

    public static final String TAG = "SyncAdapterService";

    public static final String EXTRA_INITIAL_SYNC = "firstSync";

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

    @Override
    public IBinder onBind(Intent intent) {
        return new SyncAdapter(this).getSyncAdapterBinder();
    }

    static class SyncAdapter extends AbstractThreadedSyncAdapter {

        public SyncAdapter(Context context) {
            super(context, true);
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {

            int numInserts = 0;
            int numUpdates = 0;
            int numDeletes = 0;
            int numEntries = 0;

            AccountManager manager = AccountManager.get(getContext());
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

            try {
                String cookie = manager.blockingGetAuthToken(account,
                        AccountAuthenticator.AUTH_TOKEN_COOKIE,
                        true);

                ArrayList<String> subreddits = NetApi.query(cookie);

                boolean firstSync = extras.getBoolean(EXTRA_INITIAL_SYNC);
                if (firstSync) {
                    ops.add(newDeleteByAccountName(account.name));
                    ops.add(newInsert(account.name,
                            Subreddits.NAME_FRONT_PAGE,
                            Subreddits.STATE_INSERTING));
                    numInserts++;
                    numEntries++;
                } else {
                    Cursor c = provider.query(Subreddits.CONTENT_URI, PROJECTION,
                            SubredditProvider.SELECTION_ACCOUNT,
                            new String[] {account.name},
                            null);
                    while (c.moveToNext()) {
                        long expiration = c.getLong(INDEX_EXPIRATION);
                        boolean expired = expiration != 0
                                && System.currentTimeMillis() > expiration;

                        String name = c.getString(INDEX_NAME);
                        int index = find(subreddits, name);
                        boolean exists = index != -1;
                        if (exists) {
                            subreddits.remove(index);
                        }

                        long id = c.getLong(INDEX_ID);
                        int state = c.getInt(INDEX_STATE);
                        switch (state) {
                            case Subreddits.STATE_INSERTING:
                            case Subreddits.STATE_DELETING:
                                if (expired) {
                                    if (exists) {
                                        ops.add(newUpdateToNormalState(id));
                                        numUpdates++;
                                    } else {
                                        ops.add(newDeleteById(id));
                                        numDeletes++;
                                    }
                                    numEntries++;
                                }
                                break;

                            case Subreddits.STATE_NORMAL:
                                if (!exists) {
                                    ops.add(newDeleteById(id));
                                    numDeletes++;
                                    numEntries++;
                                }
                                break;
                        }
                    }
                    c.close();
                }

                if (!subreddits.isEmpty()) {
                    int count = subreddits.size();
                    for (int i = 0; i < count; i++) {
                        ops.add(newInsert(account.name, subreddits.get(i), Subreddits.STATE_NORMAL));
                        numInserts++;
                        numEntries++;
                    }
                }

                ContentResolver cr = getContext().getContentResolver();
                ContentProviderResult[] results = cr.applyBatch(SubredditProvider.AUTHORITY, ops);

                if (firstSync) {
                    numDeletes += results[0].count;
                    numEntries += results[0].count;
                }
                syncResult.stats.numInserts += numInserts;
                syncResult.stats.numUpdates += numUpdates;
                syncResult.stats.numDeletes += numDeletes;
                syncResult.stats.numEntries += numEntries;

            } catch (OperationCanceledException e) {
                Log.e(TAG, "onPerformSync", e);
                syncResult.stats.numAuthExceptions++;
            } catch (AuthenticatorException e) {
                Log.e(TAG, "onPerformSync", e);
                syncResult.stats.numAuthExceptions++;
            } catch (IOException e) {
                Log.e(TAG, "onPerformSync", e);
                syncResult.stats.numIoExceptions++;
            } catch (RemoteException e) {
                Log.e(TAG, "onPerformSync", e);
                syncResult.databaseError = true;
            } catch (OperationApplicationException e) {
                Log.e(TAG, "onPerformSync", e);
                syncResult.databaseError = true;
            }

            if (Debug.DEBUG_SYNC) {
                Log.d(TAG, "account: " + account.name + " " + syncResult.toString());
            }
        }

        private static int find(List<String> subreddits, String name) {
            int count = subreddits.size();
            for (int i = 0; i < count; i++) {
                if (name.equalsIgnoreCase(subreddits.get(i))) {
                    return i;
                }
            }
            return -1;
        }

        private static ContentProviderOperation newDeleteByAccountName(String accountName) {
            return ContentProviderOperation.newDelete(Subreddits.CONTENT_URI)
                    .withSelection(SubredditProvider.SELECTION_ACCOUNT, new String[] {accountName})
                    .build();
        }

        private static ContentProviderOperation newUpdateToNormalState(long id) {
            return ContentProviderOperation.newUpdate(Subreddits.CONTENT_URI)
                    .withSelection(SubredditProvider.ID_SELECTION,
                            new String[] {Long.toString(id)})
                    .withValue(Subreddits.COLUMN_STATE, Subreddits.STATE_NORMAL)
                    .withValue(Subreddits.COLUMN_EXPIRATION, 0)
                    .build();
        }

        private static ContentProviderOperation newDeleteById(long id) {
            return ContentProviderOperation.newDelete(Subreddits.CONTENT_URI)
                    .withSelection(SubredditProvider.ID_SELECTION,
                            new String[] {Long.toString(id)})
                    .build();
        }

        private static ContentProviderOperation newInsert(String accountName, String subredditName,
                int state) {
            return ContentProviderOperation.newInsert(Subreddits.CONTENT_URI)
                    .withValue(Subreddits.COLUMN_ACCOUNT, accountName)
                    .withValue(Subreddits.COLUMN_NAME, subredditName)
                    .withValue(Subreddits.COLUMN_STATE, state)
                    .build();
        }
    }
}
