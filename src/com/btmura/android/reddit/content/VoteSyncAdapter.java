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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.provider.VoteProvider;
import com.btmura.android.reddit.util.Array;

/**
 * {@link AbstractThreadedSyncAdapter} that syncs pending votes to the reddit
 * backend servers.
 */
public class VoteSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "VoteSyncAdapter";

    public static class Service extends android.app.Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new VoteSyncAdapter(this).getSyncAdapterBinder();
        }
    }

    // Avoid notifying the ThingProvider that we are making changes,
    // because the UI already reflects the pending changes.
    private static Uri THINGS_URI = ThingProvider.THINGS_URI.buildUpon()
            .appendQueryParameter(ThingProvider.PARAM_NOTIFY, Boolean.toString(false))
            .build();

    private static final String[] PROJECTION = {
            Votes._ID,
            Votes.COLUMN_THING_ID,
            Votes.COLUMN_VOTE,
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_THING_ID = 1;
    private static final int INDEX_VOTE = 2;

    public VoteSyncAdapter(Context context) {
        super(context, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // Extra method just allows us to always print out sync stats after.
        doSync(account, extras, authority, provider, syncResult);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPerformSync syncResult: " + syncResult);
        }
    }

    private void doSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            // Check that we have a non-null cookie and modhash.
            AccountManager manager = AccountManager.get(getContext());
            String cookie = AccountUtils.getCookie(manager, account);
            if (cookie == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }
            String modhash = AccountUtils.getModhash(manager, account);
            if (modhash == null) {
                syncResult.stats.numAuthExceptions++;
                return;
            }

            // Get all pending votes for this account that haven't been synced.
            Cursor c = provider.query(VoteProvider.ACTIONS_URI, PROJECTION,
                    Votes.SELECT_BY_ACCOUNT, Array.of(account.name), null);

            // TODO: Drop duplicate votes to minimize number of RPCs.

            // Bail out early if there are no votes to process.
            if (c.getCount() == 0) {
                c.close();
                return;
            }

            ArrayList<ContentProviderOperation> ops =
                    new ArrayList<ContentProviderOperation>(c.getCount());

            // Things need to be updated manually after deleting a votes row.
            // This is because pagination ends up doing a join with the votes
            // table again and requerying could cause another join to be
            // executed.
            ArrayList<ContentProviderOperation> thingOps =
                    new ArrayList<ContentProviderOperation>(c.getCount());

            while (c.moveToNext()) {
                long id = c.getLong(INDEX_ID);
                String thingId = c.getString(INDEX_THING_ID);
                int vote = c.getInt(INDEX_VOTE);

                // Sync the vote with the server. If successful then schedule
                // deletion of the database row.
                try {
                    RedditApi.vote(getContext(), thingId, vote, cookie, modhash);
                    ops.add(ContentProviderOperation.newDelete(VoteProvider.ACTIONS_URI)
                            .withSelection(VoteProvider.ID_SELECTION, Array.of(id))
                            .build());

                    // Update the tables that join with the votes table since we
                    // will delete the pending vote rows afterwards.
                    String[] selectionArgs = Array.of(account.name, thingId);
                    thingOps.add(ContentProviderOperation.newUpdate(THINGS_URI)
                            .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs)
                            .withValue(Things.COLUMN_LIKES, vote)
                            .build());
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                    syncResult.stats.numIoExceptions++;
                }
            }
            c.close();

            // Update the things since pending votes will be deleted.
            ContentResolver cr = getContext().getContentResolver();
            ContentProviderResult[] results = cr.applyBatch(ThingProvider.AUTHORITY, thingOps);
            int count = results.length;
            for (int i = 0; i < count; i++) {
                syncResult.stats.numUpdates += results[i].count;
            }

            // Now delete the rows from the database. The server shows the
            // updates immediately.
            results = provider.applyBatch(ops);
            for (int i = 0; i < count; i++) {
                syncResult.stats.numDeletes += results[i].count;
            }

        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationApplicationException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.databaseError = true;
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            syncResult.stats.numAuthExceptions++;
        }
    }
}