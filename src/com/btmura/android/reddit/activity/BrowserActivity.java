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
import android.app.ActionBar.OnNavigationListener;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.SubredditListFragment;
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
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        adapter = new AccountSpinnerAdapter(this);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        prefs = result.prefs;
        adapter.setAccountNames(result.accountNames);
        int index = AccountLoader.getLastAccountIndex(result.prefs, result.accountNames);
        bar.setSelectedNavigationItem(index);
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    @Override
    protected String getAccountName() {
        return adapter.getAccountName(bar.getSelectedNavigationIndex());
    }

    @Override
    protected boolean hasSubredditList() {
        return true;
    }

    @Override
    protected void refreshActionBar(Thing thing) {
        bar.setDisplayHomeAsUpEnabled(thing != null);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (DEBUG) {
            Log.d(TAG, "onNavigationItemSelected itemPosition:" + itemPosition);
        }
        String accountName = adapter.getItem(itemPosition);
        AccountLoader.setLastAccount(prefs, accountName);

        SubredditListFragment f = getSubredditListFragment();
        if (f == null || !f.getAccountName().equals(accountName)) {
            setSubredditListNavigation(null);
        }

        return true;
    }
}
