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

import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

class SaveSyncer implements Syncer {

    private static final String[] SAVE_PROJECTION = {
            SaveActions._ID,
            SaveActions.COLUMN_ACTION,
            SaveActions.COLUMN_THING_ID,
            SaveActions.COLUMN_SYNC_FAILURES,
    };

    private static final int SAVE_ID = 0;
    private static final int SAVE_ACTION = 1;
    private static final int SAVE_THING_ID = 2;
    private static final int SAVE_SYNC_FAILURES = 3;

    @Override
    public String getTag() {
        return "s";
    }

    @Override
    public Cursor query(ContentProviderClient provider, String accountName) throws RemoteException {
        return provider.query(ThingProvider.SAVE_ACTIONS_URI,
                SAVE_PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT,
                Array.of(accountName),
                null);
    }

    @Override
    public int getSyncFailures(Cursor c) {
        return c.getInt(SAVE_SYNC_FAILURES);
    }

    @Override
    public Result sync(Context context, Cursor c, String cookie, String modhash) throws IOException {
        String thingId = c.getString(SAVE_THING_ID);
        int action = c.getInt(SAVE_ACTION);
        boolean saved = action == SaveActions.ACTION_SAVE;
        return RedditApi.save(thingId, saved, cookie, modhash);
    }

    @Override
    public void addRemoveAction(String accountName, Cursor c, Ops ops) {
        long id = c.getLong(SAVE_ID);
        ops.addDelete(ContentProviderOperation.newDelete(ThingProvider.SAVE_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .build());
    }

    @Override
    public void addSyncFailure(String accountName, Cursor c, Ops ops) {
        long id = c.getLong(SAVE_ID);
        int failures = getSyncFailures(c) + 1;
        ops.addUpdate(ContentProviderOperation.newUpdate(ThingProvider.SAVE_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .withValue(SaveActions.COLUMN_SYNC_FAILURES, failures)
                .build());
    }

    @Override
    public int getEstimatedOpCount(int count) {
        return count * 2;
    }
}
