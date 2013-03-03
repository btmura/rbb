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
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.UriHelper;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.widget.AccountAdapter;
import com.btmura.android.reddit.widget.AccountFilterAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;

public class BrowserActivity extends AbstractBrowserActivity implements OnNavigationListener {

    /** Requested subreddit from intent data to view. */
    private String requestedSubreddit;

    /** Requested thing bundle from intent data. */
    private Bundle requestedThingBundle;

    private boolean hasSubredditList;

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
    private MenuItem profileSavedItem;
    private MenuItem messagesItem;
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
                selectThing(requestedThingBundle, ThingActivity.FLAG_INSERT_HOME,
                        ThingPagerAdapter.TYPE_LINK);
                finish();
                return true;
            } else if (!TextUtils.isEmpty(requestedSubreddit)) {
                selectSubreddit(requestedSubreddit,
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
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
        getLoaderManager().initLoader(1, null, mailLoaderCallbacks);
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
        adapter.setAccountInfo(result.accountNames, result.linkKarma, result.hasMail);

        String accountName = result.getLastAccount(this);
        adapter.setAccountName(accountName);
        adapter.setFilter(result.getLastSubredditFilter(this));

        int index = adapter.findAccountName(accountName);

        // If the selected navigation index is the same, then the action bar
        // won't fire onNavigationItemSelected. Resetting the adapter and then
        // calling setSelectedNavigationItem again seems to unjam it.
        if (bar.getSelectedNavigationIndex() == index) {
            bar.setListNavigationCallbacks(adapter, this);
        }

        bar.setSelectedNavigationItem(index);
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
    protected void refreshActionBar(String subreddit, Bundle thingBundle) {
        bar.setDisplayHomeAsUpEnabled(!hasSubredditList || thingBundle != null);
        adapter.setSubreddit(subreddit);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        adapter.updateState(itemPosition);

        final String accountName = adapter.getAccountName();
        AccountPrefs.setLastAccount(this, accountName);

        int filter = adapter.getFilter();
        AccountPrefs.setLastSubredditFilter(this, filter);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNavigationItemSelected itemPosition:" + itemPosition
                    + " accountName:" + accountName
                    + " filter:" + filter);
        }

        SubredditListFragment slf = getSubredditListFragment();
        ThingListFragment tlf = getThingListFragment();

        if (slf == null || !slf.getAccountName().equals(accountName)) {
            // Set the subreddit to be the account's last visited subreddit.
            String subreddit = AccountPrefs.getLastSubreddit(this, accountName);

            // Reference to thingBundle that will often be null.
            Bundle thingBundle = null;

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

            setSubredditListNavigation(subreddit, isRandom, null, thingBundle);
        } else if (tlf != null && tlf.getFilter() != filter) {
            replaceThingListFragmentMultiPane();
        }

        // Check mail if the user is using an account.
        checkMailIfHasAccount();

        // Invalidate action bar icons when switching accounts.
        invalidateOptionsMenu();

        return true;
    }

    @Override
    public void onSubredditSelected(String subreddit) {
        super.onSubredditSelected(subreddit);
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
        profileSavedItem = menu.findItem(R.id.menu_profile_saved);
        messagesItem = menu.findItem(R.id.menu_messages);
        switchThemesItem = menu.findItem(R.id.menu_switch_themes);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPrepareOptionsMenu");
        }
        super.onPrepareOptionsMenu(menu);
        if (newPostItem == null) {
            return true; // Check that onCreateOptionsMenu was called.
        }

        boolean showAccountItems = hasSubredditList && !hasThing();
        menu.setGroupVisible(R.id.menu_group_account_items, showAccountItems);

        boolean hasAccount = hasAccount();
        newPostItem.setVisible(isSinglePane && hasAccount);
        addSubredditItem.setVisible(isSinglePane);
        profileItem.setVisible(showAccountItems && hasAccount);
        profileSavedItem.setVisible(showAccountItems && hasAccount);
        messagesItem.setVisible(showAccountItems && hasAccount);
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
