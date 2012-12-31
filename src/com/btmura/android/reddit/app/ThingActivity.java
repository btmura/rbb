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
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnThingEventListener,
        AccountNameHolder,
        SubredditNameHolder {

    public static final String TAG = "ThingActivity";

    public static final String EXTRA_THING_BUNDLE = "thingBundle";
    public static final String EXTRA_FLAGS = "flags";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_THING_BUNDLE = EXTRA_THING_BUNDLE;

    private ViewPager pager;
    private Bundle thingBundle;
    private String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing);
        setupPrereqs(savedInstanceState);
        setupFragments(savedInstanceState);
        setupActionBar(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    private void setupPrereqs(Bundle savedInstanceState) {
        pager = (ViewPager) findViewById(R.id.pager);
        if (savedInstanceState == null) {
            thingBundle = getIntent().getBundleExtra(EXTRA_THING_BUNDLE);
        } else {
            thingBundle = savedInstanceState.getBundle(STATE_THING_BUNDLE);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
            ft.add(ThingMenuFragment.newInstance(ThingBundle.getSubreddit(thingBundle),
                    ThingBundle.getThingId(thingBundle)), ThingMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setupActionBar(Bundle savedInstanceState) {
        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        refreshTitle();
    }

    private void refreshTitle() {
        setTitle(ThingBundle.getTitle(thingBundle));
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountName = result.getLastAccount();
        pager.setAdapter(new ThingPagerAdapter(getFragmentManager(), accountName, thingBundle));
        invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    public void onTitleDiscovery(String thingId, String title) {
        if (Objects.equals(thingId, ThingBundle.getThingId(thingBundle))
                && !ThingBundle.hasTitle(thingBundle)) {
            ThingBundle.putTitle(thingBundle, title);
            refreshTitle();
        }
    }

    public void onLinkDiscovery(String thingId, String url) {
        if (Objects.equals(thingId, ThingBundle.getThingId(thingBundle))
                && !ThingBundle.hasLinkUrl(thingBundle)) {
            ThingBundle.putLinkUrl(thingBundle, url);
            ThingPagerAdapter adapter = (ThingPagerAdapter) pager.getAdapter();
            adapter.addPage(0, ThingPagerAdapter.TYPE_LINK);
            pager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
        }
    }

    public void onSavedDiscovery(String thingId, boolean saved) {
        if (Objects.equals(thingId, ThingBundle.getThingId(thingBundle))) {
            ThingMenuFragment mf = getThingMenuFragment();
            mf.setSaved(saved);
        }
    }

    public void onLinkMenuItemClick() {
        pager.setCurrentItem(ThingPagerAdapter.PAGE_LINK);
    }

    public void onCommentMenuItemClick() {
        pager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
    }

    public String getAccountName() {
        return accountName;
    }

    public String getSubredditName() {
        return ThingBundle.getSubreddit(thingBundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleHome() {
        if (insertBackStack()) {
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, getSubredditName());
            intent.putExtra(ThingListActivity.EXTRA_FLAGS, ThingListActivity.FLAG_INSERT_HOME);
            startActivity(intent);
        }
        finish();
    }

    private boolean insertBackStack() {
        int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
        return Flag.isEnabled(flags, FLAG_INSERT_HOME);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_THING_BUNDLE, thingBundle);
    }

    private ThingMenuFragment getThingMenuFragment() {
        return (ThingMenuFragment) getFragmentManager().findFragmentByTag(ThingMenuFragment.TAG);
    }
}
