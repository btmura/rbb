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

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import com.btmura.android.reddit.database.CommentActions;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Result;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

class CommentSyncer implements Syncer {

    private static final String[] COMMENT_PROJECTION = {
            CommentActions._ID,
            CommentActions.COLUMN_ACTION,
            CommentActions.COLUMN_TEXT,
            CommentActions.COLUMN_THING_ID,
            CommentActions.COLUMN_SYNC_FAILURES,
    };

    private static final int COMMENT_ID = 0;
    private static final int COMMENT_ACTION = 1;
    private static final int COMMENT_TEXT = 2;
    private static final int COMMENT_THING_ID = 3;
    private static final int COMMENT_SYNC_FAILURES = 4;

    @Override
    public String getTag() {
        return "c";
    }

    @Override
    public Cursor query(ContentProviderClient provider, String accountName) throws RemoteException {
        return provider.query(ThingProvider.COMMENT_ACTIONS_URI,
                COMMENT_PROJECTION,
                SharedColumns.SELECT_BY_ACCOUNT,
                Array.of(accountName),
                SharedColumns.SORT_BY_ID);
    }

    @Override
    public int getSyncFailures(Cursor c) {
        return c.getInt(COMMENT_SYNC_FAILURES);
    }

    @Override
    public Result sync(Context context, Cursor c, String cookie, String modhash)
            throws IOException {
        int action = c.getInt(COMMENT_ACTION);
        String thingId = c.getString(COMMENT_THING_ID);
        String text = c.getString(COMMENT_TEXT);
        switch (action) {
            case CommentActions.ACTION_INSERT:
                return RedditApi.comment(thingId, text, cookie, modhash);

            case CommentActions.ACTION_DELETE:
                return RedditApi.delete(thingId, cookie, modhash);

            case CommentActions.ACTION_EDIT:
                return RedditApi.edit(thingId, text, cookie, modhash);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void addRemoveAction(String accountName, Cursor c, Ops ops) {
        long id = c.getLong(COMMENT_ID);
        ops.addDelete(ContentProviderOperation.newDelete(ThingProvider.COMMENT_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .build());
        ops.addUpdate(ContentProviderOperation.newUpdate(ThingProvider.COMMENTS_URI)
                .withSelection(Comments.SELECT_BY_COMMENT_ACTION_ID, Array.of(id))
                .withValue(Comments.COLUMN_CREATED_UTC, System.currentTimeMillis() / 1000)
                .build());
    }

    @Override
    public void addSyncFailure(String accountName, Cursor c, Ops ops) {
        long id = c.getLong(COMMENT_ID);
        int failures = getSyncFailures(c) + 1;
        ops.addUpdate(ContentProviderOperation.newUpdate(ThingProvider.COMMENT_ACTIONS_URI)
                .withSelection(ThingProvider.ID_SELECTION, Array.of(id))
                .withValue(CommentActions.COLUMN_SYNC_FAILURES, failures)
                .build());
    }

    @Override
    public int getEstimatedOpCount(int count) {
        return count * 2;
    }
}
