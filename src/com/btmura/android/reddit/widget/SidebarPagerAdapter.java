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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.btmura.android.reddit.app.SidebarFragment;

public class SidebarPagerAdapter extends FragmentPagerAdapter {

    private final String[] subreddits;
    private final boolean showHeaderButtons;

    public SidebarPagerAdapter(FragmentManager fm, String[] subreddits, boolean showHeaderButtons) {
        super(fm);
        this.subreddits = subreddits;
        this.showHeaderButtons = showHeaderButtons;
    }

    @Override
    public int getCount() {
        return subreddits.length;
    }

    @Override
    public Fragment getItem(int position) {
        return SidebarFragment.newInstance(subreddits[position], showHeaderButtons);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return subreddits[position];
    }
}
