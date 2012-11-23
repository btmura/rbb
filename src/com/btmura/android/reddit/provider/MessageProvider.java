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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.database.Votes;

public class MessageProvider extends SessionProvider {

    public static final String TAG = "MessageProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.messages";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_MESSAGES = "messages";
    public static final Uri MESSAGES_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_MESSAGES);

    private static final String TABLE_NAME_WITH_VOTES = Messages.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Messages.COLUMN_THING_ID + ")";

    public MessageProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri, boolean isQuery) {
        return isQuery ? TABLE_NAME_WITH_VOTES : Messages.TABLE_NAME;
    }

    @Override
    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values) {
    }
}
