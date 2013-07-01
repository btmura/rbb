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

import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.provider.ThingProvider;

public class SearchThingLoader extends AbstractSessionLoader implements ThingProjection {

    private final String accountName;
    private final String subreddit;
    private final String query;

    public SearchThingLoader(Context context, String accountName, String subreddit, String query,
            long sessionId) {
        super(context, ThingProvider.THINGS_WITH_ACTIONS_URI, PROJECTION,
                HideActions.SELECT_UNHIDDEN_BY_SESSION_ID, null, sessionId, null);
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.query = query;
    }

    @Override
    protected Bundle createSession(long sessionId, String more) {
        return ThingProvider.getThingSearchSession(getContext(), accountName, subreddit, query,
                sessionId);
    }
}
