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

/**
 * {@link AbstractThingLoader} that loads a subreddit's things.
 */
public class SubredditThingLoader extends AbstractThingLoader {

    private static final String TAG = "SubredditThingLoader";

    private final String accountName;
    private final String subreddit;
    private final int filter;
    private final String more;

    public SubredditThingLoader(Context context, String accountName, String subreddit, int filter,
            String more) {
        super(context);
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.filter = filter;
        this.more = more;

        setUri(ThingProvider.THINGS_WITH_ACTIONS_URI);
        setProjection(PROJECTION);
        setSelection(HideActions.SELECT_UNHIDDEN_BY_SESSION_ID);
    }

    @Override
    public Cursor loadInBackground() {
        Bundle result = ThingProvider.getSubredditSession(getContext(), accountName, subreddit,
                filter, more);
        long sessionId = result.getLong(ThingProvider.EXTRA_SESSION_ID);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground sessionId: " + sessionId);
        }
        setSelectionArgs(Array.of(sessionId));
        return new CursorExtrasWrapper(super.loadInBackground(), result);
    }
}
