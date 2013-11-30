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
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;

public class SearchActivity extends AbstractBrowserActivity implements
        LoaderCallbacks<AccountResult>,
        TabListener,
        SearchQueryHandler,
        AccountResultHolder {

    private static final String TAG = "SearchActivity";

    /** Optional string subreddit to additionally search within a subreddit. */
    public static final String EXTRA_SUBREDDIT = "subreddit";

    /** Required string search query. */
    public static final String EXTRA_QUERY = "query";

    private AccountResult accountResult;
    private String accountName;

    private TabController tabController;
    private Tab tabPostsInSubreddit;
    private Tab tabPosts;
    private Tab tabSubreddits;

    public SearchActivity() {
        super(SearchThingActivity.class);
    }

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.search);
    }

    @Override
    protected boolean skipSetup(Bundle savedInstanceState) {
        tabController = new TabController(bar, savedInstanceState);
        if (Subreddits.hasSidebar(getSubredditArgument())) {
            tabPostsInSubreddit = newTab(MenuHelper
                    .getSubredditTitle(this, getSubredditArgument()));
            tabController.addTab(tabPostsInSubreddit);
        }
        tabPosts = tabController.addTab(newTab(getString(R.string.tab_posts)));
        tabSubreddits = tabController.addTab(newTab(getString(R.string.tab_subreddits)));
        return false;
    }

    private Tab newTab(CharSequence text) {
        Tab tab = bar.newTab().setText(text).setTabListener(this);
        return tab;
    }

    @Override
    protected void doSetup(Bundle savedInstanceState) {
        if (!hasQuery()) {
            setQuery("android");
        }
        bar.setDisplayHomeAsUpEnabled(true);
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
        tabController.setupTabs();
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        accountResult = null;
        accountName = null;
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
            Log.d(TAG, "selectTab tab: " + tab.getText());
        }
        if (tabController.selectTab(tab)) {
            if (tab == tabPostsInSubreddit) {
                refreshThingList(getSubredditArgument());
            } else if (tab == tabPosts) {
                refreshThingList(null);
            } else if (tab == tabSubreddits) {
                refreshSubredditList();
            }
        }
    }

    private void refreshSubredditList() {
        setSearchSubredditsFragments(accountName, getQuery(), Filter.SUBREDDIT_HOT);
    }

    private void refreshThingList(String subreddit) {
        // TODO(btmura): don't load the preferences here
        int filter = AccountPrefs.getLastSearchFilter(this, Filter.SEARCH_RELEVANCE);
        setSearchThingsFragments(accountName,
                subreddit,
                getQuery(),
                filter);
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
        return tabController.isTabSelected(tabSubreddits);
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
        tabController.saveInstanceState(outState);
    }

    @Override
    public AccountResult getAccountResult() {
        return accountResult;
    }

    private String getSubredditArgument() {
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
