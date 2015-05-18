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
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Toast;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.app.AccountListFragment.OnAccountEventListener;
import com.btmura.android.reddit.content.SelectAccountBroadcast;
import com.btmura.android.reddit.content.ThemePrefs;

import java.io.IOException;

public class AccountListActivity extends FragmentActivity implements OnAccountEventListener,
        OnClickListener {

    public static final String TAG = "AccountListActivity";

    private Button addAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.account_list);
        setupViews(savedInstanceState);
    }

    private void setupViews(Bundle savedInstanceState) {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        } else {
            ViewStub vs = (ViewStub) findViewById(R.id.button_bar_stub);
            View buttonBar = vs.inflate();

            addAccount = (Button) buttonBar.findViewById(R.id.ok);
            addAccount.setText(R.string.add_account);
            addAccount.setOnClickListener(this);

            View cancel = findViewById(R.id.cancel);
            cancel.setOnClickListener(this);
        }

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.account_list_container, AccountListFragment.newInstance());
            ft.commit();
        }
    }

    @Override
    public void onAccountSelected(String accountName) {
        SelectAccountBroadcast.sendBroadcast(this, accountName);
        finish();
    }

    @Override
    public void onAccountsRemoved(final String[] accountNames) {
        final Context appContext = getApplicationContext();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voidRay) {
                AccountManager manager = AccountManager.get(appContext);
                String accountType = AccountAuthenticator.getAccountType(appContext);
                int length = accountNames.length;
                int removed = 0;
                for (int i = 0; i < length; i++) {
                    Account account = new Account(accountNames[i], accountType);
                    AccountManagerFuture<Boolean> result =
                            manager.removeAccount(account, null, null);
                    try {
                        if (result.getResult()) {
                            removed++;
                        }
                    } catch (OperationCanceledException e) {
                        Log.e(TAG, e.getMessage(), e);
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, e.getMessage(), e);
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
                return removed;
            }

            @Override
            protected void onPostExecute(Integer suceeded) {
                String text = getResources().getQuantityString(R.plurals.accounts,
                        suceeded, suceeded);
                Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    @Override
    public void onClick(View v) {
        if (v == addAccount) {
            handleAddAccount();
        } else {
            finish();
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
        MenuHelper.startAddAccountActivity(this);
    }
}
