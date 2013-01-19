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
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
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
import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
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
    private SharedPreferences prefs;

    private MenuItem unreadItem;
    private MenuItem postItem;
    private MenuItem aboutItem;
    private MenuItem profileItem;
    private MenuItem messagesItem;

    private LoaderCallbacks<Cursor> mailLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return AccountAdapter.getLoader(BrowserActivity.this);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            mailAdapter.swapCursor(cursor);
            refreshUnreadMessagesMenuItem();
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            mailAdapter.swapCursor(null);
        }
    };

    @Override
    protected void setContentView() {
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

        // TODO: The line below hides the subreddit list but there is still one.
        // Fix this to not actually build the subreddit list.

        // Hide the subreddit list when previewing another subreddit or link.
        hasSubredditList = TextUtils.isEmpty(requestedSubreddit);

        // Single pane browser only shows subreddits, so start another activity
        // and finish this one.
        if (isSinglePane) {
            if (requestedThingBundle != null) {
                selectThing(requestedThingBundle, ThingActivity.FLAG_INSERT_HOME);
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
        adapter = new AccountFilterAdapter(this, !isSinglePane);
        mailAdapter = new AccountAdapter(this);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(adapter, this);
        getLoaderManager().initLoader(1, null, mailLoaderCallbacks);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        prefs = result.prefs;
        adapter.setAccountNames(result.accountNames);

        String accountName = result.getLastAccount();
        adapter.setAccountName(accountName);
        adapter.setFilter(result.getLastSubredditFilter());

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
        adapter.setAccountNames(null);
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
        AccountPreferences.setLastAccount(prefs, accountName);

        int filter = adapter.getFilter();
        AccountPreferences.setLastSubredditFilter(prefs, filter);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNavigationItemSelected itemPosition:" + itemPosition
                    + " accountName:" + accountName
                    + " filter:" + filter);
        }

        // Quickly sync to check whether the user has new messages.
        if (AccountUtils.isAccount(accountName)) {
            // requestSync can trigger a strict mode warning by writing to disk.
            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    Account account = AccountUtils.getAccount(getApplicationContext(), accountName);
                    ContentResolver.requestSync(account, AccountProvider.AUTHORITY, Bundle.EMPTY);
                }
            });
        }

        SubredditListFragment slf = getSubredditListFragment();
        ThingListFragment tlf = getThingListFragment();

        if (slf == null || !slf.getAccountName().equals(accountName)) {
            // Set the subreddit to be the account's last visited subreddit.
            String subreddit = AccountPreferences.getLastSubreddit(prefs, accountName);

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

        // Invalidate mail icon when switching accounts.
        refreshUnreadMessagesMenuItem();

        return true;
    }

    @Override
    public void onSubredditSelected(String subreddit) {
        super.onSubredditSelected(subreddit);
        AccountPreferences.setLastSubreddit(prefs, getAccountName(), subreddit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browser_menu, menu);
        unreadItem = menu.findItem(R.id.menu_unread_messages);
        postItem = menu.findItem(R.id.menu_new_post);
        aboutItem = menu.findItem(R.id.menu_about_subreddit);
        profileItem = menu.findItem(R.id.menu_profile);
        messagesItem = menu.findItem(R.id.menu_messages);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPrepareOptionsMenu");
        }
        super.onPrepareOptionsMenu(menu);

        refreshUnreadMessagesMenuItem();

        boolean showThingless = isSinglePane || !hasThing();
        menu.setGroupVisible(R.id.thingless, showThingless);
        if (showThingless) {
            boolean hasAccount = adapter != null
                    && AccountUtils.isAccount(adapter.getAccountName());
            if (postItem != null) {
                postItem.setVisible(hasAccount);
            }

            if (aboutItem != null) {
                boolean showAbout = Subreddits.hasSidebar(getSubredditName());
                aboutItem.setVisible(showAbout);
            }

            if (profileItem != null) {
                profileItem.setVisible(hasAccount);
            }

            if (messagesItem != null) {
                messagesItem.setVisible(hasAccount);
            }
        }

        return true;
    }

    void refreshUnreadMessagesMenuItem() {
        if (unreadItem != null) {
            unreadItem.setVisible(adapter != null
                    && mailAdapter != null
                    && mailAdapter.hasMessages(adapter.getAccountName()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_unread_messages:
                return handleUnreadMessages();

            case R.id.menu_profile:
                return handleProfile();

            case R.id.menu_messages:
                return handleMessages();

            case R.id.menu_accounts:
                return handleAccounts();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleUnreadMessages() {
        MenuHelper.startMessageActivity(this, getAccountName(), FilterAdapter.MESSAGE_UNREAD);
        return true;
    }

    private boolean handleProfile() {
        MenuHelper.startProfileActivity(this, getAccountName(), -1);
        return true;
    }

    private boolean handleMessages() {
        MenuHelper.startMessageActivity(this, getAccountName(), -1);
        return true;
    }

    private boolean handleAccounts() {
        startActivity(new Intent(this, AccountListActivity.class));
        return true;
    }
}
