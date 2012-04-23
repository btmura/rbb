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

import com.btmura.android.reddit.fragment.SubredditDetailsListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;

public class SearchPagerAdapter extends FragmentStatePagerAdapter {

    private final String query;

    public SearchPagerAdapter(FragmentManager fm, String query) {
        super(fm);
        this.query = query;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return SubredditDetailsListFragment.newInstance(query, false);

            case 1:
                return ThingListFragment.newSearchInstance(query, false);

            default:
                throw new IllegalArgumentException();
        }
    }
}
