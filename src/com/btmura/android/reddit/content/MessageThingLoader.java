/*
 * Copyright (C) 2013 Brian Muramatsu
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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

public class MessageThingLoader extends CursorLoader {

    private static final String TAG = "MessageThingLoader";

    public static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CONTEXT,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_DESTINATION,
            Messages.COLUMN_KIND,
            Messages.COLUMN_NEW,
            Messages.COLUMN_SUBJECT,
            Messages.COLUMN_SUBREDDIT,
            Messages.COLUMN_THING_ID,
            Messages.COLUMN_WAS_COMMENT,

            // Following columns are from joined tables.
            MessageActions.COLUMN_ACTION,
    };

    public static final int MESSAGE_AUTHOR = 1;
    public static final int MESSAGE_BODY = 2;
    public static final int MESSAGE_CONTEXT = 3;
    public static final int MESSAGE_CREATED_UTC = 4;
    public static final int MESSAGE_DESTINATION = 5;
    public static final int MESSAGE_KIND = 6;
    public static final int MESSAGE_NEW = 7;
    public static final int MESSAGE_SUBJECT = 8;
    public static final int MESSAGE_SUBREDDIT = 9;
    public static final int MESSAGE_THING_ID = 10;
    public static final int MESSAGE_WAS_COMMENT = 11;
    public static final int MESSAGE_ACTION = 12;

    private final String accountName;
    private final int filter;
    private final String more;

    public MessageThingLoader(Context context, String accountName, int filter, String more) {
        super(context);
        this.accountName = accountName;
        this.filter = filter;
        this.more = more;
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        Bundle result = ThingProvider.getMessageSession(getContext(), accountName, filter, more);
        long sessionId = result.getLong(ThingProvider.EXTRA_SESSION_ID);
        setSelectionArgs(Array.of(sessionId));
        return new CursorExtrasWrapper(super.loadInBackground(), result);
    }
}