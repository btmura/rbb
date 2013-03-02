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

package com.btmura.android.reddit.widget;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.text.TextUtils;

import com.btmura.android.reddit.app.SubredditListFragment;
import com.btmura.android.reddit.app.ThingListFragment;

public class SearchPagerAdapter extends FragmentStatePagerAdapter {

    private static final int TYPE_ALL_POSTS = 0;
    private static final int TYPE_SUBREDDIT_POSTS = 1;
    private static final int TYPE_SUBREDDITS = 2;

    private static final int[] PAGES_ALL = {
            TYPE_SUBREDDIT_POSTS,
            TYPE_ALL_POSTS,
            TYPE_SUBREDDITS,
    };

    private static final int[] PAGE_SOME = {
            TYPE_ALL_POSTS,
            TYPE_SUBREDDITS,
    };

    private final String accountName;
    private final String subreddit;
    private final String query;
    private final int[] pages;

    public SearchPagerAdapter(FragmentManager fm, String accountName, String subreddit, String query) {
        super(fm);
        this.accountName = accountName;
        this.subreddit = subreddit;
        this.query = query;
        this.pages = !TextUtils.isEmpty(subreddit) ? PAGES_ALL : PAGE_SOME;
    }

    @Override
    public int getCount() {
        return pages.length;
    }

    @Override
    public Fragment getItem(int position) {
        switch (pages[position]) {
            case TYPE_ALL_POSTS:
                return ThingListFragment.newQueryInstance(accountName, null, query, 0);

            case TYPE_SUBREDDIT_POSTS:
                return ThingListFragment.newQueryInstance(accountName, subreddit, query, 0);

            case TYPE_SUBREDDITS:
                return SubredditListFragment.newInstance(accountName, null, query, 0);

            default:
                throw new IllegalArgumentException();
        }
    }
}
