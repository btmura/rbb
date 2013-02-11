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
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.GlobalMenuFragment.OnSearchQuerySubmittedListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.SearchPagerAdapter;

public class SearchActivity extends AbstractBrowserActivity implements
        TabListener,
        OnSearchQuerySubmittedListener,
        OnPageChangeListener,
        AccountResultHolder {

    public static final String TAG = "SearchActivity";

    /** Optional string subreddit to additionally search within a subreddit. */
    public static final String EXTRA_SUBREDDIT = "subreddit";

    /** Required string search query. */
    public static final String EXTRA_QUERY = "query";

    private static final String STATE_SELECTED_TAB = "selectedTab";

    private AccountResult accountResult;
    private String accountName;
    private String subreddit;
    private Tab tabPosts;
    private Tab tabSubreddits;
    private Tab tabInSubreddit;
    private ViewPager searchPager;
    private boolean tabListenerEnabled;

    @Override
    protected void setContentView() {
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
        if (isSinglePane) {
            searchPager = (ViewPager) findViewById(R.id.search_pager);
            searchPager.setOnPageChangeListener(this);
        }
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        bar.setTitle(getQuery());
        bar.setDisplayHomeAsUpEnabled(true);

        // TODO: Remove code logic duplication with SearchPagerAdapter for tabs.

        tabPosts = bar.newTab().setText(R.string.tab_posts).setTabListener(this);
        bar.addTab(tabPosts);

        if (hasValidIntentSubreddit()) {
            subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
            String tabTitle = MenuHelper.getSubredditTitle(this, subreddit);
            tabInSubreddit = bar.newTab().setText(tabTitle).setTabListener(this);
            bar.addTab(tabInSubreddit);
        }

        tabSubreddits = bar.newTab().setText(R.string.tab_subreddits).setTabListener(this);
        bar.addTab(tabSubreddits);

        // Prevent listener being called twice after a configuration change.
        tabListenerEnabled = savedInstanceState == null
                || savedInstanceState.getInt(STATE_SELECTED_TAB) == 0;
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (!tabListenerEnabled) {
            tabListenerEnabled = true;
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_TAB));
        }
    }

    private boolean hasValidIntentSubreddit() {
        String subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        return !TextUtils.isEmpty(subreddit)
                && !Subreddits.isFrontPage(subreddit)
                && !Subreddits.isAll(subreddit)
                && !Subreddits.isRandom(subreddit);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true, false);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountResult = result;
        accountName = result.getLastAccount();
        if (isSinglePane) {
            submitSearchQuerySinglePane(getQuery());
        } else {
            SubredditListFragment sf = getSubredditListFragment();
            if (sf != null && sf.getAccountName() == null) {
                sf.setAccountName(accountName);
                sf.loadIfPossible();
            }

            ThingListFragment tf = getThingListFragment();
            if (tf != null && tf.getAccountName() == null) {
                tf.setAccountName(accountName);
                tf.loadIfPossible();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        accountName = null;
    }

    public AccountResult getAccountResult() {
        return accountResult;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    protected int getFilter() {
        return FilterAdapter.SUBREDDIT_HOT;
    }

    @Override
    protected boolean hasSubredditList() {
        return bar.getSelectedTab() == tabSubreddits;
    }

    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onTabSelected tab:" + tab.getText() + " enabled:" + tabListenerEnabled);
        }
        if (tabListenerEnabled) {
            selectTab(tab);
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onTabReselected tab:" + tab.getText() + " enabled:" + tabListenerEnabled);
        }
        if (tabListenerEnabled && !isSinglePane) {
            getFragmentManager().popBackStack();
        }
    }

    private void selectTab(Tab tab) {
        if (searchPager != null) {
            searchPager.setCurrentItem(tab.getPosition());
        } else {
            setNavigationFragments(tab);
        }
    }

    private void setNavigationFragments(Tab tab) {
        String query = getQuery();
        if (tab == tabSubreddits) {
            refreshSubredditList(query);
        } else if (tab == tabPosts) {
            refreshThingList(null, query);
        } else if (tab == tabInSubreddit) {
            refreshThingList(subreddit, query);
        }
    }

    private void refreshSubredditList(String query) {
        if (isSubredditListDifferent(query)) {
            setSubredditListNavigation(null, false, query, null);
        } else {
            refreshSubredditListVisibility();
        }
    }

    private void refreshThingList(String subreddit, String query) {
        if (isThingListDifferent(subreddit, query)) {
            setQueryThingListNavigation(subreddit, query);
        } else {
            refreshSubredditListVisibility();
        }
    }

    private boolean isSubredditListDifferent(String query) {
        SubredditListFragment frag = getSubredditListFragment();
        return frag == null || !Objects.equals(query, frag.getQuery());
    }

    private boolean isThingListDifferent(String subreddit, String query) {
        ThingListFragment frag = getThingListFragment();
        return frag == null || !Objects.equals(subreddit, frag.getSubreddit())
                || !Objects.equals(query, frag.getQuery());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
        bar.setTitle(getQuery());
    }

    public boolean onSearchQuerySubmitted(String query) {
        setQuery(query);
        refreshActionBar(null, null);
        if (isSinglePane) {
            submitSearchQuerySinglePane(query);
        } else {
            submitSearchQueryMultiPane(query);
        }
        return true;
    }

    private void submitSearchQuerySinglePane(String query) {
        searchPager.setAdapter(new SearchPagerAdapter(getFragmentManager(),
                accountName, subreddit, query));
        searchPager.setCurrentItem(bar.getSelectedNavigationIndex());
    }

    private void submitSearchQueryMultiPane(String query) {
        setNavigationFragments(bar.getSelectedTab());
    }

    public void onPageSelected(int position) {
        if (isSinglePane) {
            bar.setSelectedNavigationItem(position);
        }
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, bar.getSelectedNavigationIndex());
    }

    private boolean hasQuery() {
        return getIntent().hasExtra(EXTRA_QUERY);
    }

    private String getQuery() {
        return getIntent().getStringExtra(EXTRA_QUERY);
    }

    private void setQuery(String query) {
        getIntent().putExtra(EXTRA_QUERY, query);
    }
}
