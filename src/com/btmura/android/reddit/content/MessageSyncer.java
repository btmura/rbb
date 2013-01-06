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

import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.RedditApi.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

class MessageSyncer implements Syncer {

    private static final String[] MESSAGE_PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_THING_ID,
            MessageActions.COLUMN_TEXT,
    };

    private static final int MESSAGE_ID = 0;
    private static final int MESSAGE_ACTION = 1;
    private static final int MESSAGE_THING_ID = 2;
    private static final int MESSAGE_TEXT = 3;

    public Cursor query(ContentProviderClient provider, String accountName) throws RemoteException {
        return provider.query(ThingProvider.MESSAGE_ACTIONS_URI, MESSAGE_PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT, Array.of(accountName), SharedColumns.SORT_BY_ID);
    }

    public Result sync(Context context, Cursor c, String cookie, String modhash) throws IOException {
        int action = c.getInt(MESSAGE_ACTION);
        String thingId = c.getString(MESSAGE_THING_ID);
        String text = c.getString(MESSAGE_TEXT);

        switch (action) {
            case MessageActions.ACTION_INSERT:
                return RedditApi.comment(thingId, text, cookie, modhash);

            case MessageActions.ACTION_DELETE:
                return RedditApi.delete(thingId, cookie, modhash);

            default:
                throw new IllegalArgumentException();
        }
    }

    public int getOpCount(int count) {
        return count * 2;
    }

    public void addOps(String accountName, Cursor c, ArrayList<ContentProviderOperation> ops) {
        long id = c.getLong(MESSAGE_ID);
        ops.add(ContentProviderOperation.newDelete(ThingProvider.MESSAGE_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .build());
        ops.add(ContentProviderOperation.newUpdate(ThingProvider.MESSAGES_NOTIFY_URI)
                .withSelection(Messages.SELECT_BY_MESSAGE_ACTION_ID, Array.of(id))
                .withValue(Things.COLUMN_CREATED_UTC, System.currentTimeMillis() / 1000)
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
