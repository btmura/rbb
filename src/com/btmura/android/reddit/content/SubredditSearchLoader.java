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
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

public class SubredditSearchLoader extends CursorLoader {

    private static final String TAG = "SubredditSearchLoader";

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
    private Bundle sessionData;

    public SubredditSearchLoader(Context context, String accountName, String query) {
        super(context);
        this.accountName = accountName;
        this.query = query;

        setUri(ThingProvider.SUBREDDITS_URI);
        setProjection(PROJECTION);
        setSelection(SubredditResults.SELECT_BY_SESSION_ID);
        setSortOrder(SubredditResults.SORT_BY_NAME);
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        if (sessionData == null) {
            sessionData = ThingProvider.getSubredditSearchSession(getContext(), accountName, query);
        }
        long sessionId = sessionData.getLong(ThingProvider.EXTRA_SESSION_ID);
        setSelectionArgs(Array.of(sessionId));

        Cursor cursor = super.loadInBackground();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sessionId: " + sessionId + " count: " + cursor.getCount());
        }
        return new CursorExtrasWrapper(cursor, sessionData);
    }
}
