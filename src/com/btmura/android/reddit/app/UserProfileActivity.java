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

import android.app.Activity;
import android.content.Loader;
import android.os.Bundle;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.widget.FilterAdapter;

/**
 * {@link Activity} for viewing a user's profile.
 */
public class UserProfileActivity extends AbstractBrowserActivity {

    /** Required string extra that is the user's name. */
    public static final String EXTRA_USER = "user";

    private String accountName;

    @Override
    protected void setContentView() {
        setContentView(R.layout.user_profile);
    }

    @Override
    protected boolean skipSetup() {
        return false;
    }

    @Override
    protected void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            setThingListNavigation(null, getUserName());
        }
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        bar.setTitle(getUserName());
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private String getUserName() {
        String user = getIntent().getStringExtra(EXTRA_USER);
        // TODO: Remove this fallback once things become more stable.
        if (user == null) {
            user = "rbbtest1";
        }
        return user;
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountName = result.getLastAccount();
        ThingListFragment tlf = getThingListFragment();
        if (tlf != null && tlf.getAccountName() == null) {
            tlf.setAccountName(accountName);
            tlf.loadIfPossible();
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        accountName = null;
    }

    @Override
    protected String getAccountName() {
        return accountName;
    }

    @Override
    protected int getFilter() {
        return FilterAdapter.SUBREDDIT_HOT;
    }

    @Override
    protected boolean hasSubredditList() {
        return false;
    }

    @Override
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
    }
}
