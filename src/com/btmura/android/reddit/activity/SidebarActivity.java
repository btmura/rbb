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

package com.btmura.android.reddit.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditNameHolder;
import com.btmura.android.reddit.widget.SidebarPagerAdapter;

public class SidebarActivity extends Activity implements SubredditNameHolder {

    public static final String EXTRA_SUBREDDIT = "s";

    private SidebarPagerAdapter adapter;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sidebar);

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);

            if (savedInstanceState == null) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
                ft.commit();
            }
        }

        Subreddit subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);
        setTitle(subreddit.getTitle(this));

        String[] subreddits = subreddit.name.split("\\+");
        adapter = new SidebarPagerAdapter(getFragmentManager(), subreddits);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        View pagerStrip = findViewById(R.id.pager_strip);
        pagerStrip.setVisibility(subreddits.length > 1 ? View.VISIBLE : View.GONE);
    }

    public CharSequence getSubredditName() {
        return adapter.getPageTitle(pager.getCurrentItem());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
