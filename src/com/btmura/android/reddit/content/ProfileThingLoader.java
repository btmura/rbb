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
import com.btmura.android.reddit.widget.FilterAdapter;

public class ProfileThingLoader extends AbstractThingLoader {

    private final String accountName;
    private final String profileUser;
    private final int filter;

    public ProfileThingLoader(Context context, String accountName, String profileUser, int filter,
            String more, long sessionId) {
        super(context, ThingProvider.THINGS_WITH_ACTIONS_URI, PROJECTION,
                getSelectionStatement(filter), sessionId, more);
        this.accountName = accountName;
        this.profileUser = profileUser;
        this.filter = filter;
    }

    private static String getSelectionStatement(int filter) {
        return filter == FilterAdapter.PROFILE_HIDDEN
                ? HideActions.SELECT_HIDDEN_BY_SESSION_ID
                : HideActions.SELECT_UNHIDDEN_BY_SESSION_ID;
    }

    @Override
    protected Bundle createSession(long sessionId, String more) {
        return ThingProvider.getProfileSession(getContext(), accountName, profileUser,
                filter, more, sessionId);
    }
}
