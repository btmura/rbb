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
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.MenuItem;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;

public class SidebarActivity extends AbstractBrowserActivity implements
        LoaderCallbacks<AccountResult>,
        TabListener,
        AccountResultHolder,
        SubredditNameHolder {

    private static final String TAG = "SidebarActivity";

    public static final String EXTRA_SUBREDDIT = "subreddit";

    private static final String STATE_SELECTED_TAB_INDEX = "selectedTabIndex";

    private static final int TAB_RELATED = 1;

    private AccountResult accountResult;
    private String accountName;

    private int selectedTabIndex;
    private boolean tabListenerDisabled;
    private Tab tabDescription;
    private Tab tabRelated;

    public SidebarActivity() {
        super(SidebarThingActivity.class);
    }

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.sidebar);
    }

    @Override
    protected boolean skipSetup() {
        return false;
    }

    @Override
    protected void setupViews() {
        if (!hasSubredditName()) {
            setSubredditName("android");
        }
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        bar.setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState != null) {
            selectedTabIndex = savedInstanceState.getInt(STATE_SELECTED_TAB_INDEX);
        }
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true, false);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountResult = result;
        accountName = result.getLastAccount(this);
        if (bar.getNavigationMode() != ActionBar.NAVIGATION_MODE_TABS) {
            setupTabs();
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        accountResult = null;
        accountName = null;
    }

    private void setupTabs() {
        tabDescription = addTab(getString(R.string.tab_description));
        tabRelated = addTab(getString(R.string.tab_related));

        tabListenerDisabled = selectedTabIndex != 0;
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        tabListenerDisabled = false;
        if (selectedTabIndex != 0) {
            bar.setSelectedNavigationItem(selectedTabIndex);
        }
    }

    private Tab addTab(CharSequence text) {
        Tab tab = bar.newTab().setText(text).setTabListener(this);
        bar.addTab(tab);
        return tab;
    }

    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
        selectTab(tab);
    }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
        if (!isSinglePane) {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void selectTab(Tab tab) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "selectTab tab: " + tab.getText() + " disabled: " + tabListenerDisabled);
        }
        if (!tabListenerDisabled) {
            selectedTabIndex = tab.getPosition();
            if (tab == tabDescription) {
                refreshSidebarFragments();
            } else if (tab == tabRelated) {
                refreshRelatedSubredditFragments();
            }
        }
    }

    private void refreshSidebarFragments() {
        setSidebarFragments(accountName, getSubredditName());
    }

    private void refreshRelatedSubredditFragments() {
        setRelatedSubredditsFragments(accountName, getSubredditName());
    }

    @Override
    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
    }

    @Override
    protected void refreshActionBar(ControlFragment controlFrag) {
        bar.setTitle(Subreddits.getTitle(this, getSubredditName()));
    }

    @Override
    protected boolean hasLeftFragment() {
        return selectedTabIndex == TAB_RELATED;
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB_INDEX, bar.getSelectedNavigationIndex());
    }

    @Override
    public AccountResult getAccountResult() {
        return accountResult;
    }

    @Override
    public String getSubredditName() {
        return getIntent().getStringExtra(EXTRA_SUBREDDIT);
    }

    private boolean hasSubredditName() {
        return getIntent().hasExtra(EXTRA_SUBREDDIT);
    }

    private void setSubredditName(String subreddit) {
        getIntent().putExtra(EXTRA_SUBREDDIT, subreddit);
    }
}
