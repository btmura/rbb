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
import android.app.Activity;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.AccountFilterAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;

/**
 * {@link Activity} for viewing one's own profile.
 */
public class SelfProfileActivity extends AbstractBrowserActivity implements OnNavigationListener {

    /** Required string extra that is the user's name. */
    public static final String EXTRA_USER = "user";

    /** Optional int specifying the filter to start using. */
    public static final String EXTRA_FILTER = "filter";

    private static final String STATE_USER = EXTRA_USER;
    private static final String STATE_FILTER = EXTRA_FILTER;

    /** Requested user we should initially view from the intent. */
    private String requestedUser;

    /** Requested filter we should initially use from the intent. */
    private int requestedFilter;

    private AccountFilterAdapter adapter;
    private SharedPreferences prefs;

    @Override
    protected void setContentView() {
        setContentView(R.layout.profile);
    }

    @Override
    protected boolean skipSetup() {
        return false;
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            requestedUser = getIntent().getStringExtra(EXTRA_USER);
            requestedFilter = getIntent().getIntExtra(EXTRA_FILTER, -1);
        } else {
            requestedUser = savedInstanceState.getString(STATE_USER);
            requestedFilter = savedInstanceState.getInt(EXTRA_FILTER);
        }
        adapter = new AccountFilterAdapter(this);
        bar.setListNavigationCallbacks(adapter, this);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, false, true);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        if (Array.isEmpty(result.accountNames)) {
            finish();
            return;
        }

        prefs = result.prefs;
        adapter.addProfileFilters(this, true);
        adapter.setAccountInfo(result.accountNames, result.karmaCounts);

        String accountName;
        if (!TextUtils.isEmpty(requestedUser)) {
            accountName = requestedUser;
        } else {
            accountName = result.getLastAccount();
        }
        adapter.setAccountName(accountName);

        int filter;
        if (requestedFilter != -1) {
            filter = requestedFilter;
        } else {
            filter = AccountPreferences.getLastSelfProfileFilter(prefs,
                    FilterAdapter.PROFILE_OVERVIEW);
        }
        adapter.setFilter(filter);

        int index = adapter.findAccountName(accountName);

        // If the selected navigation index is the same, then the action bar
        // won't fire onNavigationItemSelected. Resetting the adapter and then
        // calling setSelectedNavigationItem again seems to unjam it.
        if (bar.getSelectedNavigationIndex() == index) {
            bar.setListNavigationCallbacks(adapter, this);
        }

        bar.setSelectedNavigationItem(index);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        adapter.updateState(itemPosition);

        String accountName = adapter.getAccountName();
        if (!TextUtils.isEmpty(requestedUser)) {
            requestedUser = null;
        }

        int filter = adapter.getFilter();
        if (requestedFilter != -1) {
            requestedFilter = -1;
        } else {
            AccountPreferences.setLastSelfProfileFilter(prefs, filter);
        }

        ThingListFragment frag = getThingListFragment();
        if (frag == null || !Objects.equals(frag.getAccountName(), accountName)
                || frag.getFilter() != filter) {
            setProfileThingListNavigation(accountName);
        }
        return true;
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public String getAccountName() {
        return adapter.getAccountName();
    }

    @Override
    protected int getFilter() {
        return adapter.getFilter();
    }

    @Override
    protected boolean hasSubredditList() {
        return false;
    }

    @Override
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_USER, adapter.getAccountName());
        outState.putInt(STATE_FILTER, adapter.getFilter());
    }
}
