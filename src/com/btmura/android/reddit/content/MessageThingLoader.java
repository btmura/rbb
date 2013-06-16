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
import android.os.Bundle;

import com.btmura.android.reddit.database.MessageActions;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.ThingProvider;

public class MessageThingLoader extends AbstractSessionLoader {

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

    public static final int INDEX_AUTHOR = 1;
    public static final int INDEX_BODY = 2;
    public static final int INDEX_CONTEXT = 3;
    public static final int INDEX_CREATED_UTC = 4;
    public static final int INDEX_DESTINATION = 5;
    public static final int INDEX_KIND = 6;
    public static final int INDEX_NEW = 7;
    public static final int INDEX_SUBJECT = 8;
    public static final int INDEX_SUBREDDIT = 9;
    public static final int INDEX_THING_ID = 10;
    public static final int INDEX_WAS_COMMENT = 11;
    public static final int INDEX_ACTION = 12;

    private final String accountName;
    private final int filter;

    public MessageThingLoader(Context context, String accountName, int filter, String more,
            long sessionId) {
        super(context, ThingProvider.MESSAGES_WITH_ACTIONS_URI, PROJECTION,
                Messages.SELECT_BY_SESSION_ID, null, sessionId, more);
        this.accountName = accountName;
        this.filter = filter;
    }

    @Override
    protected Bundle createSession(long sessionId, String more) {
        return ThingProvider.getMessageSession(getContext(), accountName, filter, more,
                sessionId);
    }
}
