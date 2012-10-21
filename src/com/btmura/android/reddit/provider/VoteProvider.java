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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;

import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class VoteProvider extends BaseProvider {

    public static final String TAG = "VoteProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.votes";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_ACTIONS = "actions";
    public static final Uri ACTIONS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACTIONS);

    /** Whether to notify ThingProvider and CommentProvider about a vote. */
    public static final String PARAM_NOTIFY_OTHERS = "notifyOthers";

    public VoteProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri, boolean isQuery) {
        return Votes.TABLE_NAME;
    }

    @Override
    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values) {
        // No uri commands supported in this provider.
    }

    @Override
    protected void notifyChange(Uri uri) {
        super.notifyChange(uri);
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY_OTHERS, false)) {
            ContentResolver cr = getContext().getContentResolver();
            cr.notifyChange(ThingProvider.THINGS_URI, null);
            cr.notifyChange(CommentProvider.COMMENTS_URI, null);
        }
    }

    public static void voteInBackground(final Context context, final String accountName,
            final String thingId, final int likes) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                Uri uri = ACTIONS_URI.buildUpon()
                        .appendQueryParameter(PARAM_NOTIFY_OTHERS, Boolean.toString(true))
                        .appendQueryParameter(PARAM_SYNC, Boolean.toString(true))
                        .build();

                ContentValues values = new ContentValues(3);
                values.put(Votes.COLUMN_ACCOUNT, accountName);
                values.put(Votes.COLUMN_THING_ID, thingId);
                values.put(Votes.COLUMN_VOTE, likes);

                String[] selectionArgs = Array.of(accountName, thingId);
                if (cr.update(uri, values, Votes.SELECT_BY_ACCOUNT_AND_THING_ID, selectionArgs) == 0) {
                    cr.insert(uri, values);
                }
            }
        });
    }
}
