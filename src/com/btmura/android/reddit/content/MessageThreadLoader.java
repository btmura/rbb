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

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.ThingProvider;

public class MessageThreadLoader extends AbstractSessionLoader {

    private static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_KIND,
            Messages.COLUMN_NEW,
            Messages.COLUMN_SUBJECT,
            Messages.TABLE_NAME + "." + Messages.COLUMN_THING_ID,
    };

    public static final int INDEX_AUTHOR = 1;
    public static final int INDEX_BODY = 2;
    public static final int INDEX_CREATED_UTC = 3;
    public static final int INDEX_KIND = 4;
    public static final int INDEX_NEW = 5;
    public static final int INDEX_SUBJECT = 6;
    public static final int INDEX_THING_ID = 7;

    private final String accountName;
    private final String thingId;

    public MessageThreadLoader(Context context,
            String accountName,
            String thingId,
            Bundle cursorExtras) {
        super(context,
                ThingProvider.MESSAGES_URI,
                PROJECTION,
                Messages.SELECT_BY_SESSION_ID,
                null,
                cursorExtras,
                null);
        this.accountName = accountName;
        this.thingId = thingId;
    }

    @Override
    protected Bundle createSession(long sessionId, String more) {
        return ThingProvider.getMessageThreadSession(getContext(),
                accountName,
                thingId,
                sessionId);
    }
}
