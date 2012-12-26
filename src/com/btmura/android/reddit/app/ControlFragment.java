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

import android.app.Fragment;
import android.os.Bundle;

import com.btmura.android.reddit.database.Subreddits;

public class ControlFragment extends Fragment {

    public static final String TAG = "ControlFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_IS_RANDOM = "isRandom";
    private static final String ARG_THING_BUNDLE = "thingBundle";
    private static final String ARG_FILTER = "filter";

    private String accountName;
    private String subreddit;
    private boolean isRandom;
    private Bundle thingBundle;
    private int filter;

    public static ControlFragment newInstance(String accountName, String subreddit,
            boolean isRandom, Bundle thingBundle, int filter) {
        Bundle b = new Bundle(5);
        b.putString(ARG_ACCOUNT_NAME, accountName);
        b.putString(ARG_SUBREDDIT, subreddit);
        b.putBoolean(ARG_IS_RANDOM, isRandom);
        b.putBundle(ARG_THING_BUNDLE, thingBundle);
        b.putInt(ARG_FILTER, filter);

        ControlFragment frag = new ControlFragment();
        frag.setArguments(b);
        return frag;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public boolean isRandom() {
        return isRandom;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
        this.isRandom = Subreddits.NAME_RANDOM.equalsIgnoreCase(subreddit);
    }

    public void setIsRandom(boolean isRandom) {
        this.isRandom = isRandom;
    }

    public Bundle getThingBundle() {
        return thingBundle;
    }

    public void setThingBundle(Bundle thingBundle) {
        this.thingBundle = thingBundle;
    }

    public int getFilter() {
        return filter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = savedInstanceState != null ? savedInstanceState : getArguments();
        accountName = b.getString(ARG_ACCOUNT_NAME);
        subreddit = b.getString(ARG_SUBREDDIT);
        isRandom = b.getBoolean(ARG_IS_RANDOM);
        thingBundle = b.getBundle(ARG_THING_BUNDLE);
        filter = b.getInt(ARG_FILTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_ACCOUNT_NAME, accountName);
        outState.putString(ARG_SUBREDDIT, subreddit);
        outState.putBoolean(ARG_IS_RANDOM, isRandom);
        outState.putBundle(ARG_THING_BUNDLE, thingBundle);
        outState.putInt(ARG_FILTER, filter);
    }
}
