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

import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.provider.ThingProvider;

public class SubredditSearchLoader extends AbstractSessionLoader {

    public static final int INDEX_NAME = 1;
    public static final int INDEX_SUBSCRIBERS = 2;
    public static final int INDEX_OVER_18 = 3;

    private static final String[] PROJECTION = {
            SubredditResults._ID,
            SubredditResults.COLUMN_NAME,
            SubredditResults.COLUMN_SUBSCRIBERS,
            SubredditResults.COLUMN_OVER_18,
    };

    private final String accountName;
    private final String query;

    public SubredditSearchLoader(Context context, String accountName, String query,
            Bundle cursorExtras) {
        super(context, ThingProvider.SUBREDDITS_URI, PROJECTION,
                SubredditResults.SELECT_BY_SESSION_ID, SubredditResults.SORT_BY_NAME, cursorExtras,
                null);
        this.accountName = accountName;
        this.query = query;
    }

    @Override
    protected Bundle createSession(long sessionId, String more) {
        return ThingProvider.getSubredditSearchSession(getContext(), accountName, query);
    }
}
