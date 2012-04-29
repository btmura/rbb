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
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.AddSubredditFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.ThingMenuFragment;
import com.btmura.android.reddit.widget.ThingPagerAdapter;

public class ThingActivity extends GlobalMenuActivity implements
        ThingMenuFragment.ThingPagerHolder,
        AddSubredditFragment.SubredditNameHolder,
        ViewPager.OnPageChangeListener {

    public static final String EXTRA_THING = "t";
    public static final String EXTRA_FLAGS = "f";

    private Thing thing;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing);

        thing = getIntent().getParcelableExtra(EXTRA_THING);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new ThingPagerAdapter(getFragmentManager(), thing));
        pager.setOnPageChangeListener(this);

        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setTitle(thing.assureTitle(this).title);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.add(ThingMenuFragment.newInstance(thing), ThingMenuFragment.TAG);
            ft.commit();
        }
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

    public ViewPager getPager() {
        return pager;
    }

    public String getSubredditName() {
        return thing.subreddit;
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }
}
