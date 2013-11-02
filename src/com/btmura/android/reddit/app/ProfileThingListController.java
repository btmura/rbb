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

package com.btmura.android.reddit.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.btmura.android.reddit.content.ProfileThingLoader;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.ThingView.OnThingViewClickListener;

class ProfileThingListController extends ThingTableListController {

    static final String EXTRA_PROFILE_USER = "profileUser";
    static final String EXTRA_FILTER = "filter";

    private final String profileUser;
    private final int swipeAction;

    ProfileThingListController(Context context, Bundle args, OnThingViewClickListener listener) {
        super(context, args, listener);
        this.profileUser = getProfileUserExtra(args);
        this.swipeAction = getSwipeActionExtra(args);
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new ProfileThingLoader(context,
                getAccountName(),
                profileUser,
                getFilter(),
                getMoreId(),
                getCursorExtras());
    }

    public String getProfileUser() {
        return profileUser;
    }

    @Override
    public int getSwipeAction() {
        return swipeAction;
    }

    private String getProfileUserExtra(Bundle extras) {
        return extras.getString(EXTRA_PROFILE_USER);
    }

    private int getSwipeActionExtra(Bundle extras) {
        switch (getFilterExtra(extras)) {
            case FilterAdapter.PROFILE_HIDDEN:
                return SWIPE_ACTION_UNHIDE;

            default:
                return SWIPE_ACTION_NONE;
        }
    }
}
