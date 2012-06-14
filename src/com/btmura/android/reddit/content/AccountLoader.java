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
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;

public class AccountLoader extends AsyncTaskLoader<AccountResult> implements OnAccountsUpdateListener {

    public static final String TAG = "AccountLoader";

    public static class AccountResult {
        public SharedPreferences prefs;
        public Account[] accounts;
        public int selectedAccount;

        private AccountResult(SharedPreferences prefs, Account[] accounts, int selectedAccount) {
            this.prefs = prefs;
            this.accounts = accounts;
            this.selectedAccount = selectedAccount;
        }
    }

    public static final String PREF_LAST_LOGIN = "lastLogin";

    private static Comparator<Account> ACCOUNT_COMPARATOR = new Comparator<Account>() {
        public int compare(Account lhs, Account rhs) {
            return lhs.name.compareToIgnoreCase(rhs.name);
        }
    };

    private static final String PREFS = "prefs";

    private AccountResult result;

    private AccountManager manager;

    public AccountLoader(Context context) {
        super(context.getApplicationContext());
        manager = AccountManager.get(context.getApplicationContext());
        //manager.addOnAccountsUpdatedListener(this, null, false);
    }

    @Override
    public AccountResult loadInBackground() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "loadInBackground (id " + getId() + ")");
        }

        Context context = getContext();

        // Getting preferences launches a thread to load them so start early.
        SharedPreferences prefs = context.getSharedPreferences(PREFS, 0);

        // Get the list of accounts to show in the spinner.
        Account[] accounts = manager.getAccountsByType(AccountAuthenticator.getAccountType(context));
        Arrays.sort(accounts, ACCOUNT_COMPARATOR);

        // Find which account was selected last.
        int selectedAccount = 0;
        int numAccounts = accounts.length;
        if (numAccounts != 0) {
            String lastLogin = prefs.getString(PREF_LAST_LOGIN, null);
            for (int i = 0; i < numAccounts; i++) {
                if (accounts[i].name.equals(lastLogin)) {
                    selectedAccount = i;
                    break;
                }
            }
        }

        return new AccountResult(prefs, accounts, selectedAccount);
    }

    @Override
    public void deliverResult(AccountResult newResult) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "deliverResult (id " + getId() + ")");
        }
        if (isReset()) {
            closeResult(newResult);
            return;
        }

        AccountResult oldResult = this.result;
        this.result = newResult;

        if (isStarted()) {
            super.deliverResult(newResult);
        }

        if (oldResult != null && oldResult != newResult) {
            closeResult(oldResult);
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
    protected void onStopLoading() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onStopLoading (id " + getId() + ")");
        }
        cancelLoad();
    }

    @Override
    public void onCanceled(AccountResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onCanceled (id " + getId() + ")");
        }
        closeResult(result);
    }

    @Override
    protected void onReset() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onReset (id " + getId() + ")");
        }
        super.onReset();
        onStopLoading();
        closeResult(result);
        result = null;
    }

    private void closeResult(AccountResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "closeResult");
        }
    }

    public void onAccountsUpdated(Account[] accounts) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onAccountsUpdated");
        }
        onContentChanged();
    }
}
