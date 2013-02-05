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

package com.btmura.android.reddit.content;

import java.util.Arrays;
import java.util.Comparator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.accounts.AccountPreferences;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;

public class AccountLoader extends AsyncTaskLoader<AccountResult> implements
        OnAccountsUpdateListener {

    public static final String TAG = "AccountLoader";

    public static class AccountResult {
        public String[] accountNames;
        public String[] karmaCounts;
        public SharedPreferences prefs;

        private AccountResult(String[] accountNames, String[] karmaCounts,
                SharedPreferences prefs) {
            this.accountNames = accountNames;
            this.karmaCounts = karmaCounts;
            this.prefs = prefs;
        }

        public String getLastAccount() {
            String accountName = AccountPreferences.getLastAccount(prefs, Subreddits.ACCOUNT_NONE);
            int numAccounts = accountNames.length;
            for (int i = 0; i < numAccounts; i++) {
                if (accountNames[i].equals(accountName)) {
                    return accountName;
                }
            }
            return numAccounts > 0 ? accountNames[0] : null;
        }

        // TODO: Get rid of these methods since they are just wrappers.

        public int getLastMessageFilter() {
            return AccountPreferences.getLastMessageFilter(prefs, 0);
        }

        public int getLastSubredditFilter() {
            return AccountPreferences.getLastSubredditFilter(prefs, 0);
        }
    }

    private static Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
        public int compare(Account lhs, Account rhs) {
            return lhs.name.compareToIgnoreCase(rhs.name);
        }
    };

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(SelectAccountBroadcast.EXTRA_ACCOUNT);
            AccountPreferences.setLastAccount(prefs, accountName);
            onContentChanged();
        }
    };

    private SharedPreferences prefs;
    private AccountManager manager;
    private boolean includeNoAccount;
    private boolean includeKarmaCounts;
    private boolean listening;
    private AccountResult result;

    public AccountLoader(Context context, boolean includeNoAccount, boolean includeKarmaCounts) {
        super(context);
        this.prefs = AccountPreferences.getPreferences(context);
        this.manager = AccountManager.get(getContext());
        this.includeNoAccount = includeNoAccount;
        this.includeKarmaCounts = includeKarmaCounts;
    }

    @Override
    public AccountResult loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        Context context = getContext();

        // Get the accounts and sort them.
        Account[] accounts = manager
                .getAccountsByType(AccountAuthenticator.getAccountType(context));
        Arrays.sort(accounts, ACCOUNT_COMPARATOR);

        // Convert to strings and prepend the no account at the top.
        int offset = includeNoAccount ? 1 : 0;
        int length = includeNoAccount ? accounts.length + 1 : accounts.length;
        String[] accountNames = new String[length];
        if (includeNoAccount) {
            accountNames[0] = Subreddits.ACCOUNT_NONE;
        }
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i + offset] = accounts[i].name;
        }

        String[] karmaCounts = null;
        if (includeKarmaCounts) {
            karmaCounts = new String[length];
            if (includeNoAccount) {
                karmaCounts[0] = null;
            }
            for (int i = 0; i < accounts.length; i++) {
                karmaCounts[i + offset] = "1337";
            }
        }

        // Get a preference to make sure the loading thread is done.
        AccountPreferences.getLastAccount(prefs, null);

        return new AccountResult(accountNames, karmaCounts, prefs);
    }

    @Override
    public void deliverResult(AccountResult newResult) {
        if (isReset()) {
            return;
        }

        this.result = newResult;

        if (isStarted()) {
            super.deliverResult(newResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (result != null) {
            deliverResult(result);
        }
        if (!listening) {
            manager.addOnAccountsUpdatedListener(this, null, false);
            SelectAccountBroadcast.registerReceiver(getContext(), receiver);
            listening = true;
        }
        if (takeContentChanged() || result == null) {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        result = null;
        if (listening) {
            manager.removeOnAccountsUpdatedListener(this);
            SelectAccountBroadcast.unregisterReceiver(getContext(), receiver);
            listening = false;
        }
    }

    public void onAccountsUpdated(Account[] accounts) {
        onContentChanged();
    }
}
