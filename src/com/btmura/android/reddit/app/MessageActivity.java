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
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.AccountFilterAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.ThingBundle;

public class MessageActivity extends AbstractBrowserActivity implements OnNavigationListener {

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
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.message);
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

        adapter.addMessageFilters(this);
        adapter.setAccountInfo(result.accountNames, result.linkKarma, result.hasMail);

        if (currentUser == null) {
            currentUser = result.getLastAccount(this);
        }
        adapter.setAccountName(currentUser);

        if (currentFilter == -1) {
            currentFilter = FilterAdapter.MESSAGE_INBOX;
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

        ThingListFragment<?> frag = getThingListFragment();
        if (frag == null
                || !Objects.equals(frag.getAccountName(), currentUser)
                || frag.getFilter() != currentFilter) {
            setMessageThingListNavigation(R.id.thing_list_container, currentUser);
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
    protected void refreshActionBar(String subreddit, ThingBundle thingBundle) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.message_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean showThingless = isSinglePane || !hasThing();
        menu.setGroupVisible(R.id.thingless, showThingless);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_message:
                handleNewMessage();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleNewMessage() {
        MenuHelper.startComposeActivity(this, ComposeActivity.MESSAGE_TYPE_SET,
                null, null, null, null, null, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_USER, currentUser);
        outState.putInt(STATE_FILTER, currentFilter);
    }
}
