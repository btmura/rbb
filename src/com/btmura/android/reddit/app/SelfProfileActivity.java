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
import android.os.Bundle;

import com.btmura.android.reddit.R;
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

    private String currentUser;
    private int currentFilter = -1;
    private AccountFilterAdapter adapter;

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
            currentUser = getIntent().getStringExtra(EXTRA_USER);
            currentFilter = getIntent().getIntExtra(EXTRA_FILTER, -1);
        } else {
            currentUser = savedInstanceState.getString(STATE_USER);
            currentFilter = savedInstanceState.getInt(EXTRA_FILTER);
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

        adapter.addProfileFilters(this, true);
        adapter.setAccountInfo(result.accountNames, result.karmaCounts);

        if (currentUser == null) {
            currentUser = result.getLastAccount();
        }
        adapter.setAccountName(currentUser);

        if (currentFilter == -1) {
            currentFilter = FilterAdapter.PROFILE_OVERVIEW;
        }
        adapter.setFilter(currentFilter);

        int index = adapter.findAccountName(currentUser);

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
        currentUser = adapter.getAccountName();
        currentFilter = adapter.getFilter();

        ThingListFragment frag = getThingListFragment();
        if (frag == null || !Objects.equals(frag.getAccountName(), currentUser)
                || frag.getFilter() != currentFilter) {
            setProfileThingListNavigation(currentUser);
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
        outState.putString(STATE_USER, currentUser);
        outState.putInt(STATE_FILTER, currentFilter);
    }
}
