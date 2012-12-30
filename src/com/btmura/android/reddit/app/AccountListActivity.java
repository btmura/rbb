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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.provider.SubredditProvider;

public class AccountListActivity extends Activity {

    public static final String TAG = "AccountListActivity";

    private static final String[] AUTHORITIES = {
            SubredditProvider.AUTHORITY,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_list);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.account_list_container, AccountListFragment.newInstance());
            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.account_list_menu, menu);
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
