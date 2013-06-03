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
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

public class SearchThingLoader extends AbstractThingLoader {

    private static final String TAG = "SearchThingLoader";

    private final String accountName;
    private final String subreddit;
    private final String query;
    private Bundle sessionData;

    public SearchThingLoader(Context context, String accountName, String subreddit, String query) {
        super(context);
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.query = query;

        setUri(ThingProvider.THINGS_WITH_ACTIONS_URI);
        setProjection(PROJECTION);
        setSelection(HideActions.SELECT_UNHIDDEN_BY_SESSION_ID);
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        if (sessionData == null) {
            sessionData = ThingProvider.getThingSearchSession(getContext(), accountName,
                    subreddit, query);
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
