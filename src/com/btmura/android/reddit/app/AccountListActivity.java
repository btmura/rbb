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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.Button;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.AccountListFragment.OnAccountSelectedListener;
import com.btmura.android.reddit.content.SelectAccountBroadcast;
import com.btmura.android.reddit.content.ThemePrefs;

public class AccountListActivity extends FragmentActivity
    implements OnAccountSelectedListener, OnClickListener {

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
      ft.replace(R.id.account_list_container,
          AccountListFragment.newInstance());
      ft.commit();
    }
  }

  @Override
  public void onAccountSelected(String accountName) {
    SelectAccountBroadcast.sendBroadcast(this, accountName);
    finish();
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
