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

import android.accounts.Account;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.DrawerFragment.OnDrawerEventListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.UriHelper;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.AccountAdapter;
import com.btmura.android.reddit.widget.AccountFilterAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;

public class BrowserActivity extends AbstractBrowserActivity
        implements OnNavigationListener, OnDrawerEventListener {

    /** Requested subreddit from intent data to view. */
    private String requestedSubreddit;

    /** Requested thing bundle from intent data. */
    private ThingBundle requestedThingBundle;

    private boolean hasSubredditList;

    private ActionBarDrawerToggle drawerToggle;
    private AccountFilterAdapter adapter;
    private AccountAdapter mailAdapter;

    private LoaderCallbacks<Cursor> mailLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return AccountAdapter.getLoader(BrowserActivity.this);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            mailAdapter.swapCursor(cursor);
            refreshMessagesIcon();
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            mailAdapter.swapCursor(null);
        }
    };

    private MenuItem newPostItem;
    private MenuItem addSubredditItem;
    private MenuItem profileItem;
    private MenuItem savedItem;
    private MenuItem messagesItem;
    private MenuItem accountsItem;
    private MenuItem switchThemesItem;

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.browser);
    }

    @Override
    protected boolean skipSetup() {
        // Process the intent's data if available.
        Uri data = getIntent().getData();
        if (data != null) {
            requestedSubreddit = UriHelper.getSubreddit(data);
            requestedThingBundle = UriHelper.getThingBundle(data);
        }

        // TODO: Do more sanity checks on the url data.

        // TODO: The line below hides the subreddit list but there is still one. Fix this to not
        // actually build the subreddit list.

        // Hide the subreddit list when previewing another subreddit or link.
        hasSubredditList = TextUtils.isEmpty(requestedSubreddit);

        // Single pane browser only shows subreddits, so start another activity
        // and finish this one.
        if (isSinglePane) {
            if (requestedThingBundle != null) {
                selectThing(null, requestedThingBundle, ThingActivity.FLAG_INSERT_HOME,
                        ThingPagerAdapter.TYPE_LINK);
                finish();
                return true;
            } else if (!TextUtils.isEmpty(requestedSubreddit)) {
                selectSubreddit(null,
                        requestedSubreddit,
                        Subreddits.isRandom(requestedSubreddit),
                        ThingListActivity.FLAG_INSERT_HOME);
                finish();
                return true;
            }
        }

        return false;
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        adapter = new AccountFilterAdapter(this);
        mailAdapter = new AccountAdapter(this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                    ThemePrefs.getDrawerIcon(this), R.string.drawer_open, R.string.drawer_close);
            drawerLayout.setDrawerListener(drawerToggle);
            drawerLayout.setDrawerShadow(ThemePrefs.getDrawerShadow(this), GravityCompat.START);
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
        getSupportLoaderManager().initLoader(1, null, mailLoaderCallbacks);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true, true);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        if (!isSinglePane) {
            adapter.addSubredditFilters(this);
        }
        adapter.setOnlyFilters();
        adapter.setFilter(result.getLastSubredditFilter(this));
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
        return hasSubredditList;
    }

    @Override
    protected void refreshActionBar(String subreddit, ThingBundle thingBundle) {
        adapter.setSubreddit(subreddit);
    }

    @Override
    public void onDrawerAccountSelected(View view, String accountName) {
        drawerLayout.closeDrawer(view);
        adapter.setAccountName(accountName);
        updateFragments();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        adapter.updateState(itemPosition);
        updateFragments();
        return true;
    }

    private void updateFragments() {
        String accountName = adapter.getAccountName();
        AccountPrefs.setLastAccount(this, accountName);

        int filter = adapter.getFilter();
        AccountPrefs.setLastSubredditFilter(this, filter);

        AccountSubredditListFragment slf = getAccountSubredditListFragment();
        ThingListFragment<?> tlf = getThingListFragment();

        if (slf == null || !Objects.equals(slf.getAccountName(), accountName)) {
            // Set the subreddit to be the account's last visited subreddit.
            String subreddit = AccountPrefs.getLastSubreddit(this, accountName);

            // Reference to thingBundle that will often be null.
            ThingBundle thingBundle = null;

            // Override the subreddit and thing to the one requested by the
            // intent. Single pane activities have launched another activity to
            // handle intent URIs already.
            if (!isSinglePane && !TextUtils.isEmpty(requestedSubreddit)) {
                subreddit = requestedSubreddit;
                thingBundle = requestedThingBundle;
                requestedSubreddit = null;
                requestedThingBundle = null;
            }

            // Check name to see if this is the random subreddit. We avoid the
            // problem where the last visited subreddit is the resolved random
            // subreddit, because we don't save the subreddit preference when
            // changing filters or on resolving the subreddit!
            boolean isRandom = Subreddits.isRandom(subreddit);
            setAccountSubredditListNavigation(R.id.subreddit_list_container, subreddit, isRandom,
                    thingBundle);
        } else if (tlf != null && tlf.getFilter() != filter) {
            replaceThingListFragmentMultiPane();
        }

        // Check mail if the user is using an account.
        checkMailIfHasAccount();

        // Invalidate action bar icons when switching accounts.
        invalidateOptionsMenu();
    }

    @Override
    public void onSubredditSelected(View view, String subreddit) {
        super.onSubredditSelected(view, subreddit);
        AccountPrefs.setLastSubreddit(this, getAccountName(), subreddit);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkMailIfHasAccount();
    }

    private void checkMailIfHasAccount() {
        final String accountName = getAccountName();
        if (AccountUtils.isAccount(accountName)) {
            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    Account account = AccountUtils.getAccount(getApplicationContext(), accountName);
                    ContentResolver.requestSync(account, AccountProvider.AUTHORITY, Bundle.EMPTY);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browser_menu, menu);
        newPostItem = menu.findItem(R.id.menu_browser_new_post);
        addSubredditItem = menu.findItem(R.id.menu_browser_add_subreddit);
        profileItem = menu.findItem(R.id.menu_profile);
        savedItem = menu.findItem(R.id.menu_profile_saved);
        messagesItem = menu.findItem(R.id.menu_messages);
        accountsItem = menu.findItem(R.id.menu_accounts);
        switchThemesItem = menu.findItem(R.id.menu_switch_themes);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPrepareOptionsMenu");
        }
        super.onPrepareOptionsMenu(menu);
        if (addSubredditItem == null) {
            return true; // Check that onCreateOptionsMenu was called.
        }

        boolean showAccountItems = hasSubredditList && !hasThing();
        menu.setGroupVisible(R.id.menu_group_account_items, showAccountItems);

        boolean hasAccount = hasAccount();
        newPostItem.setVisible(isSinglePane && hasAccount);
        addSubredditItem.setVisible(isSinglePane);
        profileItem.setVisible(showAccountItems && hasAccount);
        savedItem.setVisible(showAccountItems && hasAccount);
        messagesItem.setVisible(showAccountItems && hasAccount);
        accountsItem.setVisible(showAccountItems);
        switchThemesItem.setVisible(showAccountItems);

        refreshMessagesIcon();
        return true;
    }

    private boolean hasAccount() {
        return adapter != null && AccountUtils.isAccount(adapter.getAccountName());
    }

    private void refreshMessagesIcon() {
        if (messagesItem != null) {
            int icon = hasUnreadMessages()
                    ? ThemePrefs.getUnreadMessagesIcon(this)
                    : ThemePrefs.getMessagesIcon(this);
            messagesItem.setIcon(icon);
        }
    }

    private int getMessagesFilter() {
        return hasUnreadMessages() ? FilterAdapter.MESSAGE_UNREAD : -1;
    }

    private boolean hasUnreadMessages() {
        return adapter != null
                && mailAdapter != null
                && mailAdapter.hasMessages(adapter.getAccountName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome(item);
                return true;

            case R.id.menu_profile:
                handleProfile();
                return true;

            case R.id.menu_profile_saved:
                handleProfileSaved();
                return true;

            case R.id.menu_messages:
                handleMessages();
                return true;

            case R.id.menu_accounts:
                handleAccounts();
                return true;

            case R.id.menu_switch_themes:
                handleSwitchThemes();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void handleHome(MenuItem item) {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            drawerToggle.onOptionsItemSelected(item);
        } else {
            super.onOptionsItemSelected(item);
        }
    }

    private void handleProfile() {
        MenuHelper.startSelfProfileActivity(this, getAccountName(), -1);
    }

    private void handleProfileSaved() {
        MenuHelper.startSelfProfileActivity(this, getAccountName(), FilterAdapter.PROFILE_SAVED);
    }

    private void handleMessages() {
        MenuHelper.startMessageActivity(this, getAccountName(), getMessagesFilter());
    }

    private void handleAccounts() {
        MenuHelper.startAccountListActivity(this);
    }

    private void handleSwitchThemes() {
        ThemePrefs.switchTheme(this);
        recreate();
    }
}
