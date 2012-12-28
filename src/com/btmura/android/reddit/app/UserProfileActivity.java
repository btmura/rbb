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
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.FilterAdapter;

/**
 * {@link Activity} for viewing a user's profile.
 */
public class UserProfileActivity extends AbstractBrowserActivity implements OnNavigationListener {

    private static final String STATE_NAVIGATION_INDEX = "navigationIndex";

    private String requestedUser;
    private int requestedFilter = -1;

    private FilterAdapter adapter;
    private String accountName;
    private SharedPreferences prefs;

    @Override
    protected void setContentView() {
        setContentView(R.layout.user_profile);
    }

    @Override
    protected boolean skipSetup() {
        // Get the user from the intent data or extra.
        Uri data = getIntent().getData();
        if (data != null) {
            requestedUser = UriHelper.getUser(data);
            requestedFilter = UriHelper.getUserFilter(data);
        }

        // Quit if there is no profile to view.
        if (TextUtils.isEmpty(requestedUser)) {
            finish();
            return true;
        }

        // Continue on since we have some user.
        return false;
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        adapter = new FilterAdapter(this);
        adapter.setTitle(requestedUser);

        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_NAVIGATION_INDEX));
        }
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        prefs = result.prefs;
        accountName = result.getLastAccount();
        adapter.addProfileFilters(this, Objects.equals(accountName, requestedUser));
        if (requestedFilter != -1) {
            bar.setSelectedNavigationItem(requestedFilter);
        } else {
            bar.setSelectedNavigationItem(result.getLastProfileFilter());
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    protected String getAccountName() {
        return accountName;
    }

    @Override
    protected int getFilter() {
        return adapter.getFilter(bar.getSelectedNavigationIndex());
    }

    @Override
    protected boolean hasSubredditList() {
        return false;
    }

    @Override
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int filter = getFilter();
        if (requestedFilter != -1) {
            requestedFilter = -1;
        } else {
            AccountPreferences.setLastProfileFilter(prefs, filter);
        }

        ThingListFragment frag = getThingListFragment();
        if (frag == null || !Objects.equals(frag.getAccountName(), accountName)
                || frag.getFilter() != filter) {
            setProfileThingListNavigation(requestedUser);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NAVIGATION_INDEX, bar.getSelectedNavigationIndex());
    }
}
