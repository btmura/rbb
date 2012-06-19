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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.provider.SubredditProvider.Subreddits;

public class AccountLoader extends AsyncTaskLoader<AccountResult> implements OnAccountsUpdateListener {

    public static final String TAG = "AccountLoader";

    public static class AccountResult {
        public String[] accountNames;
        public int selectedAccount;

        private AccountResult(String[] accountNames, int selectedAccount) {
            this.accountNames = accountNames;
            this.selectedAccount = selectedAccount;
        }
    }

    private static Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
        public int compare(Account lhs, Account rhs) {
            return lhs.name.compareToIgnoreCase(rhs.name);
        }
    };

    private static final String PREFS = "accountPreferences";
    private static final String PREF_LAST_ACCOUNT = "lastAccount";

    private SharedPreferences prefs;
    private AccountManager manager;

    private AccountResult result;

    public AccountLoader(Context context) {
        super(context);
        prefs = getContext().getSharedPreferences(PREFS, 0);
        manager = AccountManager.get(getContext());
        manager.addOnAccountsUpdatedListener(this, null, false);
    }

    @Override
    public AccountResult loadInBackground() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "loadInBackground id: " + getId());
        }

        Context context = getContext();

        // Get the accounts and sort them.
        Account[] accounts = manager.getAccountsByType(AccountAuthenticator.getAccountType(context));
        Arrays.sort(accounts, ACCOUNT_COMPARATOR);

        // Convert to strings and prepend the no account at the top.
        String[] accountNames = new String[accounts.length + 1];
        accountNames[0] = Subreddits.ACCOUNT_NONE;
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i + 1] = accounts[i].name;
        }

        // Find which account was selected last.
        int selectedAccount = 0;
        String lastAccount = prefs.getString(PREF_LAST_ACCOUNT, null);
        Log.d(TAG, "lastAccount: " + lastAccount);
        for (int i = 0; i < accountNames.length; i++) {
            if (accountNames[i].equals(lastAccount)) {
                selectedAccount = i;
                break;
            }
        }

        return new AccountResult(accountNames, selectedAccount);
    }

    @Override
    public void deliverResult(AccountResult newResult) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "deliverResult (id " + getId() + ")");
        }
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
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onStartLoading (id " + getId() + ")");
        }
        if (result != null) {
            deliverResult(result);
        }
        if (takeContentChanged() || result == null) {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onReset (id " + getId() + ")");
        }
        super.onReset();
        onStopLoading();
        result = null;
    }

    public void onAccountsUpdated(Account[] accounts) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onAccountsUpdated");
        }
        onContentChanged();
    }

    public static void setLastAccount(Context context, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);
        Editor editor = prefs.edit();
        editor.putString(AccountLoader.PREF_LAST_ACCOUNT, name);
        editor.apply();
    }
}
