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

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditNameHolder;
import com.btmura.android.reddit.fragment.ThingMenuFragment;
import com.btmura.android.reddit.fragment.ThingMenuFragment.ThingPagerHolder;
import com.btmura.android.reddit.widget.ThingPagerAdapter;

public class ThingActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        ThingPagerHolder,
        SubredditNameHolder,
        OnPageChangeListener {

    public static final String TAG = "ThingActivity";

    public static final String EXTRA_THING_BUNDLE = "tb";

    private Bundle thingBundle;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOnPageChangeListener(this);

        thingBundle = getIntent().getBundleExtra(EXTRA_THING_BUNDLE);
        setInitialFragments(savedInstanceState);
        setActionBar(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
            ft.add(ThingMenuFragment.newInstance(thingBundle), ThingMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar(Bundle savedInstanceState) {
        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        bar.setTitle(Things.getTitle(thingBundle));
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        String accountName = result.getLastAccount();
        pager.setAdapter(new ThingPagerAdapter(getFragmentManager(), accountName, thingBundle));
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
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
        return Things.getSubreddit(thingBundle);
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }
}
