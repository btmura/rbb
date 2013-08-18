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

import android.os.Bundle;

import com.btmura.android.reddit.util.ComparableFragments;

public class SubredditThingListFragment
        extends ThingListFragment<SubredditThingListController, ThingTableActionModeController> {

    public static SubredditThingListFragment newInstance(String accountName, String subreddit,
            int filter, boolean singleChoice) {
        Bundle args = new Bundle(5);
        args.putString(SubredditThingListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(SubredditThingListController.EXTRA_PARENT_SUBREDDIT, subreddit);
        args.putString(SubredditThingListController.EXTRA_SUBREDDIT, subreddit);
        args.putInt(SubredditThingListController.EXTRA_FILTER, filter);
        args.putBoolean(SubredditThingListController.EXTRA_SINGLE_CHOICE, singleChoice);

        SubredditThingListFragment frag = new SubredditThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    protected SubredditThingListController createController() {
        return new SubredditThingListController(getActivity(), getArguments(), this);
    }

    @Override
    protected ThingTableActionModeController createActionModeController(
            SubredditThingListController controller) {
        return new ThingTableActionModeController(getActivity(), controller.getAccountName(),
                controller.getAdapter());
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o)
                && ComparableFragments.equalStrings(this, o,
                        SubredditThingListController.EXTRA_ACCOUNT_NAME)
                && ComparableFragments.equalStrings(this, o,
                        SubredditThingListController.EXTRA_SUBREDDIT)
                && ComparableFragments.equalInts(this, o,
                        SubredditThingListController.EXTRA_FILTER);
    }
}
