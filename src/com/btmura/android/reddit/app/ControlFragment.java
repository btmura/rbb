/*
 * Copyright (C) 2012 Brian Muramatsu
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
import android.support.v4.app.Fragment;

import com.btmura.android.reddit.database.Subreddits;

public class ControlFragment extends Fragment {

    public static final int NAVIGATION_SUBREDDIT = 0;
    public static final int NAVIGATION_PROFILE = 1;
    public static final int NAVIGATION_SAVED = 2;
    public static final int NAVIGATION_MESSAGES = 3;
    public static final int NAVIGATION_SEARCH_THINGS = 4;
    public static final int NAVIGATION_SEARCH_SUBREDDITS = 5;
    public static final int NAVIGATION_USER_PROFILE = 6;
    public static final int NAVIGATION_SIDEBAR = 7;

    private static final String ARG_NAVIGATION = "navigation";
    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_IS_RANDOM = "isRandom";
    private static final String ARG_PROFILE_USER = "profileUser";
    private static final String ARG_MESSAGE_USER = "messageUser";
    private static final String ARG_QUERY = "query";
    private static final String ARG_THING_BUNDLE = "thingBundle";
    private static final String ARG_FILTER = "filter";

    public static ControlFragment newSubredditInstance(String accountName, String subreddit,
            ThingBundle thingBundle, int filter) {
        Bundle args = new Bundle(6);
        args.putInt(ARG_NAVIGATION, NAVIGATION_SUBREDDIT);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putBoolean(ARG_IS_RANDOM, Subreddits.isRandom(subreddit));
        args.putParcelable(ARG_THING_BUNDLE, thingBundle);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newProfileInstance(String accountName, int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_PROFILE);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_PROFILE_USER, accountName);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newSavedInstance(String accountName, int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_SAVED);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_PROFILE_USER, accountName);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newMessagesInstance(String accountName, int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_MESSAGES);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_MESSAGE_USER, accountName);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newSearchThingsInstance(String accountName, String subreddit,
            String query, int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_SEARCH_THINGS);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newSearchSubredditsInstance(String accountName, String query,
            int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_SEARCH_SUBREDDITS);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newUserProfileInstance(String accountName, String profileUser,
            int filter) {
        Bundle args = new Bundle(4);
        args.putInt(ARG_NAVIGATION, NAVIGATION_PROFILE);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_PROFILE_USER, profileUser);
        args.putInt(ARG_FILTER, filter);
        return newFragment(args);
    }

    public static ControlFragment newSidebarInstance(String accountName, String subreddit) {
        Bundle args = new Bundle(3);
        args.putInt(ARG_NAVIGATION, NAVIGATION_SIDEBAR);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        return newFragment(args);
    }

    public ControlFragment withThingBundle(ThingBundle thingBundle) {
        Bundle args = new Bundle(getArguments());
        args.putParcelable(ARG_THING_BUNDLE, thingBundle);
        return newFragment(args);
    }

    private static ControlFragment newFragment(Bundle args) {
        ControlFragment frag = new ControlFragment();
        frag.setArguments(args);
        return frag;
    }

    public int getNavigation() {
        return getArguments().getInt(ARG_NAVIGATION);
    }

    public String getAccountName() {
        return getArguments().getString(ARG_ACCOUNT_NAME);
    }

    public String getSubreddit() {
        return getArguments().getString(ARG_SUBREDDIT);
    }

    public boolean isRandom() {
        return getArguments().getBoolean(ARG_IS_RANDOM);
    }

    public ThingBundle getThingBundle() {
        return getArguments().getParcelable(ARG_THING_BUNDLE);
    }

    public int getFilter() {
        return getArguments().getInt(ARG_FILTER);
    }
}
