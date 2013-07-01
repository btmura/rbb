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

public class SearchThingListFragment extends ThingListFragment {

    public static SearchThingListFragment newInstance(String accountName, String subreddit,
            String query, boolean singleChoice) {
        Bundle args = new Bundle(6);
        args.putInt(ARG_TYPE, TYPE_SEARCH);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_PARENT_SUBREDDIT, subreddit);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_QUERY, query);
        args.putBoolean(ARG_SINGLE_CHOICE, singleChoice);

        SearchThingListFragment frag = new SearchThingListFragment();
        frag.setArguments(args);
        return frag;
    }
}
