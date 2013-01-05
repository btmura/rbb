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

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

class ReadSyncer implements Syncer {

    private static final String[] READ_PROJECTION = {
            ReadActions._ID,
            ReadActions.COLUMN_ACTION,
            ReadActions.COLUMN_THING_ID,
    };

    private static final int READ_ID = 0;
    private static final int READ_ACTION = 1;
    private static final int READ_THING_ID = 2;

    public Cursor query(ContentProviderClient provider, String accountName) throws RemoteException {
        return provider.query(ThingProvider.READ_ACTIONS_URI, READ_PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT, Array.of(accountName), null);
    }

    public Result sync(Context context, Cursor c, String cookie, String modhash) throws IOException {
        String thingId = c.getString(READ_THING_ID);
        boolean read = c.getInt(READ_ACTION) == ReadActions.ACTION_READ;
        return RedditApi.readMessage(thingId, read, cookie, modhash);
    }

    public int getOpCount(int count) {
        return count * 2;
    }

    public void addOps(String accountName, Cursor c, ArrayList<ContentProviderOperation> ops) {
        long id = c.getLong(READ_ID);
        String thingId = c.getString(READ_THING_ID);
        int action = c.getInt(READ_ACTION);

        // Delete the row corresponding to the pending read.
        ops.add(ContentProviderOperation.newDelete(ThingProvider.READ_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .build());

        // Update the tables that join with the reads table
        // since we will delete the pending read rows.
        String[] selectionArgs = Array.of(accountName, thingId);
        ops.add(ContentProviderOperation.newUpdate(ThingProvider.MESSAGES_URI)
                .withSelection(Things.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs)
                .withValue(Messages.COLUMN_NEW, action == ReadActions.ACTION_UNREAD)
                .build());
    }

    public void tallyOpResults(ContentProviderResult[] results, SyncResult syncResult) {
        int count = results.length;
        for (int i = 0; i < count;) {
            syncResult.stats.numDeletes += results[i++].count;
            syncResult.stats.numUpdates += results[i++].count;
        }
    }
}
