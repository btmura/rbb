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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.StringUtil;

public class ThingActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnThingEventListener,
        AccountNameHolder,
        SubredditNameHolder {

    public static final String TAG = "ThingActivity";

    public static final String EXTRA_THING_BUNDLE = "thingBundle";
    public static final String EXTRA_PAGE_TYPE = "pageType";
    public static final String EXTRA_FLAGS = "flags";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_THING_BUNDLE = EXTRA_THING_BUNDLE;

    private final Formatter formatter = new Formatter();
    private ThingBundle thingBundle;
    private String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.thing);
        setupPrereqs(savedInstanceState);
        setupFragments(savedInstanceState);
        setupActionBar(savedInstanceState);
        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void setupPrereqs(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            thingBundle = getIntent().getParcelableExtra(EXTRA_THING_BUNDLE);
        } else {
            thingBundle = savedInstanceState.getParcelable(STATE_THING_BUNDLE);
        }
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setupActionBar(Bundle savedInstanceState) {
        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        refreshTitle(thingBundle.hasLinkId() ? thingBundle.getLinkTitle() : thingBundle.getTitle());
    }

    private void refreshTitle(String title) {
        setTitle(StringUtil.safeString(formatter.formatAll(this, title)));
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true, false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountName = result.getLastAccount(this);

        // Commit the fragment here to avoid some menu item jank.
        if (getThingFragment() == null) {
            Fragment thingFrag = ThingFragment.newInstance(accountName, thingBundle);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.thing_container, thingFrag, ThingFragment.TAG);
            ft.commitAllowingStateLoss();
        }

        invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onThingTitleDiscovery(String title) {
        refreshTitle(title);
    }

    public String getAccountName() {
        return accountName;
    }

    public String getSubredditName() {
        return thingBundle.getSubreddit();
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
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_SUBREDDIT, getSubredditName());
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
        outState.putParcelable(STATE_THING_BUNDLE, thingBundle);
    }

    private ThingFragment getThingFragment() {
        return (ThingFragment) getSupportFragmentManager().findFragmentByTag(ThingFragment.TAG);
    }
}
