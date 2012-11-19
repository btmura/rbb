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
import android.app.ActionBar.OnNavigationListener;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.widget.AccountSpinnerAdapter;

public class BrowserActivity extends AbstractBrowserActivity implements OnNavigationListener {

    public static final String EXTRA_SUBREDDIT_NAME = "sn";

    private AccountSpinnerAdapter adapter;
    private SharedPreferences prefs;

    @Override
    protected void setContentView() {
        setContentView(R.layout.browser);
    }

    @Override
    protected boolean skipSetup() {
        if (isSinglePane && getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            String name = getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME);
            selectSubredditSinglePane(name, ThingListActivity.FLAG_INSERT_HOME);
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void setupFragments(Bundle savedInstanceState) {
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        adapter = new AccountSpinnerAdapter(this, !isSinglePane);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        prefs = result.prefs;
        adapter.setAccountNames(result.accountNames);

        String accountName = result.getLastAccount();
        adapter.setAccountName(accountName);
        adapter.setFilter(result.getLastFilter());

        int index = adapter.findAccountName(accountName);
        bar.setSelectedNavigationItem(index);
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    @Override
    protected String getAccountName() {
        return adapter.getAccountName();
    }

    @Override
    protected int getFilter() {
        return adapter.getFilter();
    }

    @Override
    protected boolean hasSubredditList() {
        return true;
    }

    @Override
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
        bar.setDisplayHomeAsUpEnabled(thingBundle != null);
        adapter.setSubreddit(subreddit);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        adapter.updateState(itemPosition);

        String accountName = adapter.getAccountName();
        AccountPreferences.setLastAccount(prefs, accountName);

        int filter = adapter.getFilter();
        AccountPreferences.setLastFilter(prefs, filter);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNavigationItemSelected i:" + itemPosition
                    + " an:" + accountName + " f:" + filter);
        }

        SubredditListFragment slf = getSubredditListFragment();
        ThingListFragment tlf = getThingListFragment();
        if (slf == null || !slf.getAccountName().equals(accountName)) {
            String name = getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME);
            String subreddit;
            if (!isSinglePane && !TextUtils.isEmpty(name)) {
                subreddit = name;
            } else {
                subreddit = AccountPreferences.getLastSubreddit(prefs, accountName);
            }
            setSubredditListNavigation(subreddit, null);
        } else if (tlf != null && tlf.getFilter() != filter) {
            replaceThingListFragmentMultiPane();
        }

        return true;
    }

    @Override
    public void onSubredditSelected(String subreddit) {
        super.onSubredditSelected(subreddit);
        AccountPreferences.setLastSubreddit(prefs, getAccountName(), subreddit);
    }
}
