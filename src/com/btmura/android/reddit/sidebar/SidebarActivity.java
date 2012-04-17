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

package com.btmura.android.reddit.sidebar;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.btmura.android.reddit.R;

public class SidebarActivity extends Activity {

    public static final String EXTRA_SUBREDDIT = "s";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sidebar);

        String subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        String[] subreddits = subreddit.split("\\+");

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new SidebarPagerAdapter(getFragmentManager(), subreddits));

        View pagerStrip = findViewById(R.id.pager_strip);
        pagerStrip.setVisibility(subreddits.length > 1 ? View.VISIBLE : View.GONE);
    }
}
