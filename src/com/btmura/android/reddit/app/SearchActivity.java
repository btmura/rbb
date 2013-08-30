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
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.GlobalMenuFragment.SearchQueryHandler;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.widget.FilterAdapter;

public class SearchActivity extends AbstractBrowserActivity implements
        LoaderCallbacks<AccountResult>,
        TabListener,
        SearchQueryHandler,
        AccountResultHolder {

    public static final String TAG = "SearchActivity";

    /** Optional string subreddit to additionally search within a subreddit. */
    public static final String EXTRA_SUBREDDIT = "subreddit";

    /** Required string search query. */
    public static final String EXTRA_QUERY = "query";

    private static final String STATE_SELECTED_TAB_INDEX = "selectedTabIndex";

    private AccountResult accountResult;
    private String accountName;

    private int savedSelectedTabIndex;
    private boolean tabListenerDisabled;
    private Tab tabPosts;
    private Tab tabSubreddits;
    private Tab tabInSubreddit;

    public SearchActivity() {
        super(SearchThingActivity.class);
    }

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.search);
    }

    @Override
    protected boolean skipSetup() {
        return false;
    }

    @Override
    protected void setupViews() {
        if (!hasQuery()) {
            setQuery("android");
        }
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        bar.setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState != null) {
            savedSelectedTabIndex = savedInstanceState.getInt(STATE_SELECTED_TAB_INDEX);
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
        if (Subreddits.hasSidebar(getSubreddit())) {
            tabInSubreddit = addTab(MenuHelper.getSubredditTitle(this, getSubreddit()));
        }
        tabPosts = addTab(getString(R.string.tab_posts));
        tabSubreddits = addTab(getString(R.string.tab_subreddits));

        // TODO: Extract this code into a tab controller to reduce code up with SidebarActivity.
        tabListenerDisabled = savedSelectedTabIndex != 0;
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        tabListenerDisabled = false;
        if (savedSelectedTabIndex != 0) {
            bar.setSelectedNavigationItem(savedSelectedTabIndex);
        }
    }

    private Tab addTab(CharSequence text) {
        Tab tab = bar.newTab().setText(text).setTabListener(this);
        bar.addTab(tab);
        return tab;
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        selectTab(tab);
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        if (!isSinglePane) {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void selectTab(Tab tab) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "selectTab tab: " + tab.getText() + " disabled: " + tabListenerDisabled);
        }
        if (!tabListenerDisabled) {
            if (tab == tabInSubreddit) {
                refreshThingList(getSubreddit(), getQuery());
            } else if (tab == tabPosts) {
                refreshThingList(null, getQuery());
            } else if (tab == tabSubreddits) {
                refreshSubredditList(getQuery());
            }
        }
    }

    private void refreshSubredditList(String query) {
        setSearchSubredditsFragments(accountName, query, FilterAdapter.SUBREDDIT_HOT);
    }

    private void refreshThingList(String subreddit, String query) {
        setSearchThingsFragments(accountName, subreddit, query, FilterAdapter.SUBREDDIT_HOT);
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    protected void refreshActionBar(ControlFragment controlFrag) {
        bar.setTitle(getQuery());
    }

    @Override
    protected boolean hasLeftFragment() {
        return bar.getSelectedTab() == tabSubreddits;
    }

    @Override
    public boolean submitQuery(String query) {
        setQuery(query);
        refreshActionBar(null);
        selectTab(bar.getSelectedTab());
        return true;
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

    private String getSubreddit() {
        return getIntent().getStringExtra(EXTRA_SUBREDDIT);
    }

    @Override
    public String getQuery() {
        return getIntent().getStringExtra(EXTRA_QUERY);
    }

    private boolean hasQuery() {
        return getIntent().hasExtra(EXTRA_QUERY);
    }

    private void setQuery(String query) {
        getIntent().putExtra(EXTRA_QUERY, query);
    }
}
