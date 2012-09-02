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

package com.btmura.android.reddit.fragment;

import android.app.Fragment;
import android.os.Bundle;

public class ControlFragment extends Fragment {

    public static final String TAG = "ControlFragment";

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_SUBREDDIT = "s";
    private static final String ARG_THING_BUNDLE = "tb";
    private static final String ARG_THING_POSITION = "tp";
    private static final String ARG_FILTER = "f";

    private String accountName;
    private String subreddit;
    private Bundle thingBundle;
    private int thingPosition;
    private int filter;

    public static ControlFragment newInstance(String accountName, String sr, Bundle thingBundle,
            int thingPosition, int filter) {
        Bundle b = new Bundle(5);
        b.putString(ARG_ACCOUNT_NAME, accountName);
        b.putString(ARG_SUBREDDIT, sr);
        b.putBundle(ARG_THING_BUNDLE, thingBundle);
        b.putInt(ARG_THING_POSITION, thingPosition);
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

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public Bundle getThingBundle() {
        return thingBundle;
    }

    public int getThingPosition() {
        return thingPosition;
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
        thingBundle = b.getBundle(ARG_THING_BUNDLE);
        thingPosition = b.getInt(ARG_THING_POSITION);
        filter = b.getInt(ARG_FILTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_ACCOUNT_NAME, accountName);
        outState.putString(ARG_SUBREDDIT, subreddit);
        outState.putBundle(ARG_THING_BUNDLE, thingBundle);
        outState.putInt(ARG_THING_POSITION, thingPosition);
        outState.putInt(ARG_FILTER, filter);
    }
}
