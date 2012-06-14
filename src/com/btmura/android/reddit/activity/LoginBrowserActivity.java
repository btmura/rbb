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

import android.accounts.Account;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.widget.AccountSwitcherAdapter;

public class LoginBrowserActivity extends Activity implements
        LoaderCallbacks<AccountResult>,
        OnNavigationListener {

    public static final String TAG = "LoginBrowserActivity";

    private ActionBar bar;
    private AccountSwitcherAdapter adapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_STRICT_MODE) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        setInitialFragments(savedInstanceState);
        setActionBar();
        getLoaderManager().initLoader(0, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar() {
        bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        adapter = new AccountSwitcherAdapter(this);
        bar.setListNavigationCallbacks(adapter, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onLoadFinished (id " + loader.getId() + ") "
                    + "(count " + result.accounts.length + ")");
        }
        prefs = result.prefs;
        adapter.setAccounts(result.accounts);
        bar.setSelectedNavigationItem(result.selectedAccount);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onLoaderReset (id " + loader.getId() + ")");
        }
        adapter.setAccounts(null);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (Debug.DEBUG_ACTIVITY) {
            Log.d(TAG, "onNavigationItemSelected (itemPosition " + itemPosition + ")");
        }
        Account account = adapter.getItem(itemPosition);
        prefs.edit().putString(AccountLoader.PREF_LAST_LOGIN, account.name).apply();
        return false;
    }
}
