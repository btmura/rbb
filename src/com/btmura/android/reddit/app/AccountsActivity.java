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

import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.provider.SubredditProvider;

public class AccountsActivity extends PreferenceActivity implements LoaderCallbacks<AccountResult> {

    public static final String TAG = "AccountsActivity";

    private static final String[] AUTHORITIES = {
            SubredditProvider.AUTHORITY,
    };

    private AccountResult result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        getLoaderManager().initLoader(0, null, this);
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        this.result = result;
        invalidateHeaders();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        this.result = null;
        invalidateHeaders();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        if (result != null) {
            int length = result.accountNames.length;
            for (int i = 0; i < length; i++) {
                String accountName = result.accountNames[i];
                Bundle args = new Bundle(1);
                args.putString(AccountPreferenceFragment.ARG_ACCOUNT_NAME, accountName);
                addHeader(0, accountName, AccountPreferenceFragment.class, args, target);
            }
        }
    }

    private void addHeader(int titleRes, String title, Class<? extends Fragment> fragClass,
            Bundle fragArgs, List<Header> target) {
        Header header = new Header();
        header.titleRes = header.breadCrumbTitleRes = header.breadCrumbShortTitleRes = titleRes;
        header.title = header.breadCrumbTitle = header.breadCrumbShortTitle = title;
        header.fragment = fragClass.getName();
        header.fragmentArguments = fragArgs;
        target.add(header);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.accounts_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_add_account:
                handleAddAccount();
                return true;

            case R.id.menu_sync_settings:
                handleSyncSettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAddAccount() {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, AUTHORITIES);
        startActivity(intent);
    }

    private void handleSyncSettings() {
        Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {SubredditProvider.AUTHORITY});
        startActivity(intent);
    }
}
