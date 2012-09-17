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
import com.btmura.android.reddit.provider.Provider;

public class SettingsActivity extends PreferenceActivity implements LoaderCallbacks<AccountResult> {

    public static final String TAG = "SettingsActivity";

    private static final String[] AUTHORITIES = {
            Provider.AUTHORITY,
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
        loadHeadersFromResource(R.xml.settings_headers, target);
        if (result != null) {
            String[] accountNames = result.accountNames;
            int length = accountNames.length;
            for (int i = 0; i < length; i++) {
                Header header = new Header();
                header.breadCrumbTitle = accountNames[i];
                header.breadCrumbShortTitle = accountNames[i];
                header.title = accountNames[i];
                header.fragment = AccountPreferenceFragment.class.getName();

                Bundle args = new Bundle(1);
                args.putString(AccountPreferenceFragment.ARG_ACCOUNT_NAME, accountNames[i]);
                header.fragmentArguments = args;
                target.add(header);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.settings_menu, menu);
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAddAccount() {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, AUTHORITIES);
        startActivity(intent);
    }
}
