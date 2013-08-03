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
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.UriHelper;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.util.StringUtil;
import com.btmura.android.reddit.widget.FilterAdapter;

public class BrowserActivity extends AbstractBrowserActivity implements
        LoaderCallbacks<AccountResult>,
        OnNavigationListener {

    public static final String EXTRA_SUBREDDIT = "subreddit";

    /** Requested subreddit from intent data to view. */
    private String requestedSubreddit;

    /** Requested thing bundle from intent data. */
    private ThingBundle requestedThingBundle;

    private boolean hasLeftFragment;
    private boolean showDrawer;

    private FilterAdapter filterAdapter;
    private ActionBarDrawerToggle drawerToggle;

    private MenuItem accountsItem;
    private MenuItem switchThemesItem;

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.browser);
    }

    @Override
    protected boolean skipSetup() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_SUBREDDIT)) {
            requestedSubreddit = StringUtil.emptyToNull(intent.getStringExtra(EXTRA_SUBREDDIT));
        } else if (intent.getData() != null) {
            Uri data = intent.getData();
            requestedSubreddit = StringUtil.emptyToNull(UriHelper.getSubreddit(data));
            requestedThingBundle = UriHelper.getThingBundle(data);
        }

        hasLeftFragment = !isSinglePane && drawerLayout == null && requestedSubreddit == null;
        showDrawer = drawerLayout != null && requestedSubreddit == null;

        if (isSinglePane && requestedSubreddit != null && requestedThingBundle != null) {
            selectThing(null, requestedSubreddit, requestedThingBundle);
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void setupViews() {
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        if (drawerLayout != null) {
            if (hasLeftFragment || !showDrawer) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                drawerToggle = new ActionBarDrawerToggle(this,
                        drawerLayout,
                        ThemePrefs.getDrawerIcon(this),
                        R.string.drawer_open,
                        R.string.drawer_close);
                drawerLayout.setDrawerListener(drawerToggle);
                drawerLayout.setDrawerShadow(ThemePrefs.getDrawerShadow(this), GravityCompat.START);
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }

        filterAdapter = new FilterAdapter(this);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if (savedInstanceState == null) {
            if (requestedSubreddit != null) {
                getSupportLoaderManager().initLoader(0, null, this);
            } else {
                setBrowserFragments();
            }
        }
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true, false);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        String accountName = result.getLastAccount(this);
        int filter = result.getLastSubredditFilter(this);
        setSubredditFragments(accountName,
                requestedSubreddit,
                requestedThingBundle,
                filter);
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int newFilter = filterAdapter.getFilter(itemPosition);

        NavigationFragment navFrag = getNavigationFragment();
        if (navFrag != null && navFrag.getFilter() != newFilter) {
            navFrag.setFilter(newFilter);
            return true;
        }

        ControlFragment controlFrag = getControlFragment();
        if (controlFrag != null && controlFrag.getFilter() != newFilter) {
            setSubredditFragments(controlFrag.getAccountName(),
                    controlFrag.getSubreddit(),
                    controlFrag.getThingBundle(),
                    newFilter);
            return true;
        }

        return false;
    }

    @Override
    protected void refreshActionBar(ControlFragment controlFrag) {
        bar.setDisplayHomeAsUpEnabled(isSinglePane
                || drawerLayout != null
                || controlFrag.getThingBundle() != null);
        switch (controlFrag.getNavigation()) {
            case ControlFragment.NAVIGATION_SUBREDDIT:
                updateSubredditActionBar(controlFrag);
                break;

            case ControlFragment.NAVIGATION_PROFILE:
            case ControlFragment.NAVIGATION_SAVED:
                updateProfileActionBar(controlFrag);
                break;

            case ControlFragment.NAVIGATION_MESSAGES:
                updateMessagesActionBar(controlFrag);
                break;
        }
    }

    private void updateSubredditActionBar(ControlFragment controlFrag) {
        String accountName = controlFrag.getAccountName();
        setActionBarTitle(accountName, Subreddits.getTitle(this, controlFrag.getSubreddit()));

        filterAdapter.clear();
        filterAdapter.addSubredditFilters(this);
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setSelectedNavigationItem(filterAdapter.findFilter(controlFrag.getFilter()));
    }

    private void updateProfileActionBar(ControlFragment controlFrag) {
        String accountName = controlFrag.getAccountName();
        setActionBarTitle(accountName, getString(R.string.subtitle_profile));

        filterAdapter.clear();
        filterAdapter.addProfileFilters(this, AccountUtils.isAccount(accountName));
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setSelectedNavigationItem(filterAdapter.findFilter(controlFrag.getFilter()));
    }

    private void updateMessagesActionBar(ControlFragment controlFrag) {
        String accountName = controlFrag.getAccountName();
        setActionBarTitle(accountName, getString(R.string.subtitle_messages));

        filterAdapter.clear();
        filterAdapter.addMessageFilters(this);
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setSelectedNavigationItem(filterAdapter.findFilter(controlFrag.getFilter()));
    }

    private void setActionBarTitle(String accountName, String subtitle) {
        String title = !TextUtils.isEmpty(accountName) ? accountName : getString(R.string.app_name);
        filterAdapter.setTitle(title);
        filterAdapter.setSubtitle(subtitle);
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
    protected boolean hasLeftFragment() {
        return hasLeftFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browser_menu, menu);
        accountsItem = menu.findItem(R.id.menu_accounts);
        switchThemesItem = menu.findItem(R.id.menu_switch_themes);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (accountsItem == null) {
            return true; // Check that onCreateOptionsMenu was called.
        }

        boolean showAccountItems = hasLeftFragment && !hasThing();
        accountsItem.setVisible(showAccountItems);
        switchThemesItem.setVisible(showAccountItems);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome(item);
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
        if (drawerToggle != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            // TODO: Use return value when support library is fixed to not always return false.
            drawerToggle.onOptionsItemSelected(item);
        } else {
            super.onOptionsItemSelected(item);
        }
    }

    private void handleAccounts() {
        MenuHelper.startAccountListActivity(this);
    }

    private void handleSwitchThemes() {
        ThemePrefs.switchTheme(this);
        recreate();
    }
}
