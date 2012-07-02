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
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.GlobalMenuFragment.OnSearchQuerySubmittedListener;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.widget.SearchPagerAdapter;

public class SearchActivity extends AbstractBrowserActivity implements TabListener,
        OnSearchQuerySubmittedListener {

    public static final String TAG = "SearchActivity";
    public static final boolean DEBUG = Debug.DEBUG;

    public static final String EXTRA_QUERY = "q";

    private static final String STATE_SELECTED_TAB = "st";

    private String accountName;
    private Tab tabPosts;
    private Tab tabSubreddits;
    private ViewPager searchPager;
    private boolean tabListenerEnabled;

    @Override
    protected void setContentView() {
        setContentView(R.layout.search);
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

        tabPosts = bar.newTab().setText(R.string.tab_posts).setTabListener(this);
        tabSubreddits = bar.newTab().setText(R.string.tab_subreddits).setTabListener(this);
        bar.addTab(tabPosts);
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

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountName = AccountLoader.getLastAccount(result.prefs, result.accountNames);
        if (!isSinglePane) {
            SubredditListFragment slf = getSubredditListFragment();
            if (slf != null && slf.getAccountName() == null) {
                slf.setAccountName(accountName);
                slf.loadIfPossible();
            }

            ThingListFragment tlf = getThingListFragment();
            if (tlf != null && tlf.getAccountName() == null) {
                tlf.setAccountName(accountName);
                tlf.loadIfPossible();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        accountName = null;
    }

    @Override
    protected String getAccountName() {
        return accountName;
    }

    @Override
    protected boolean hasSubredditList() {
        return bar.getSelectedTab() == tabSubreddits;
    }

    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        if (DEBUG) {
            Log.d(TAG, "onTabSelected t:" + tab.getText() + " e:" + tabListenerEnabled);
        }
        if (tabListenerEnabled) {
            selectTab(tab);
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        if (DEBUG) {
            Log.d(TAG, "onTabReselected t:" + tab.getText() + " e:" + tabListenerEnabled);
        }
        if (tabListenerEnabled) {
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
            SubredditListFragment f = getSubredditListFragment();
            if (f == null || !query.equals(f.getQuery())) {
                setSubredditListNavigation(query);
            } else {
                refreshSubredditListVisibility();
            }
        } else {
            ThingListFragment f = getThingListFragment();
            if (f == null || !query.equals(f.getQuery())) {
                setThingListNavigation(query);
            } else {
                refreshSubredditListVisibility();
            }
        }
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    protected void refreshActionBar(Thing thing) {
        bar.setTitle(getQuery());
    }

    public boolean onSearchQuerySubmitted(String query) {
        setQuery(query);
        refreshActionBar(null);
        if (isSinglePane) {
            submitSearchQuerySinglePane(query);
        } else {
            submitSearchQueryMultiPane(query);
        }
        return true;
    }

    private void submitSearchQuerySinglePane(String query) {
        searchPager.setAdapter(new SearchPagerAdapter(getFragmentManager(), accountName, query));
        searchPager.setCurrentItem(bar.getSelectedNavigationIndex());
    }

    private void submitSearchQueryMultiPane(String query) {
        setNavigationFragments(bar.getSelectedTab());
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        bar.setSelectedNavigationItem(position);
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
