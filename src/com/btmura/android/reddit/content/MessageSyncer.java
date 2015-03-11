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

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

import java.io.IOException;

class MessageSyncer implements Syncer {

    private static final String[] PROJECTION = {
            MessageActions._ID,
            MessageActions.COLUMN_ACTION,
            MessageActions.COLUMN_EXPIRATION,
            MessageActions.COLUMN_SYNC_FAILURES,
            MessageActions.COLUMN_THING_ID,
            MessageActions.COLUMN_TEXT,
    };

    private static final int ID = 0;
    private static final int ACTION = 1;
    private static final int EXPIRATION = 2;
    private static final int SYNC_FAILURES = 3;
    private static final int THING_ID = 4;
    private static final int TEXT = 5;

    @Override
    public String getTag() {
        return "m";
    }

    @Override
    public Cursor query(ContentProviderClient provider, String accountName) throws RemoteException {
        return provider.query(ThingProvider.MESSAGE_ACTIONS_URI,
                PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT,
                Array.of(accountName),
                SharedColumns.SORT_BY_ID);
    }

    @Override
    public long getExpiration(Cursor c) {
        return c.getLong(EXPIRATION);
    }

    @Override
    public int getSyncFailures(Cursor c) {
        return c.getInt(SYNC_FAILURES);
    }

    @Override
    public Result sync(Context context, Cursor c, String cookie, String modhash)
            throws IOException {
        int action = c.getInt(ACTION);
        String thingId = c.getString(THING_ID);
        String text = c.getString(TEXT);

        switch (action) {
            case MessageActions.ACTION_INSERT:
                return RedditApi.comment(thingId, text, cookie, modhash);

            case MessageActions.ACTION_DELETE:
                return RedditApi.delete(thingId, cookie, modhash);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void addDeleteAction(Cursor c, Ops ops) {
        long id = c.getLong(ID);
        ops.addDelete(ContentProviderOperation.newDelete(ThingProvider.MESSAGE_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .build());
        ops.addUpdate(ContentProviderOperation.newUpdate(ThingProvider.MESSAGES_URI)
                .withSelection(Messages.SELECT_BY_MESSAGE_ACTION_ID, Array.of(id))
                .withValue(Things.COLUMN_CREATED_UTC, System.currentTimeMillis() / 1000)
                .build());
    }

    @Override
    public void addUpdateAction(Cursor c,
                                Ops ops,
                                long expiration,
                                int syncFailures,
                                String syncStatus) {
        long id = c.getLong(ID);
        ops.addUpdate(ContentProviderOperation.newUpdate(ThingProvider.MESSAGE_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .withValue(MessageActions.COLUMN_EXPIRATION, expiration)
                .withValue(MessageActions.COLUMN_SYNC_FAILURES, syncFailures)
                .withValue(MessageActions.COLUMN_SYNC_STATUS, syncStatus)
                .build());
    }

    @Override
    public int getEstimatedOpCount(int count) {
        return count * 2;
    }
}
