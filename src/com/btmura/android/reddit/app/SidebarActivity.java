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

import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.MenuItem;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;

public class SidebarActivity extends AbstractBrowserActivity implements
    LoaderCallbacks<AccountResult>,
    TabListener,
    AccountResultHolder,
    SubredditHolder {

  private static final String TAG = "SidebarActivity";

  public static final String EXTRA_SUBREDDIT = "subreddit";

  private AccountResult accountResult;
  private String accountName;

  private TabController tabController;
  private Tab tabDescription;
  private Tab tabRelated;

  public SidebarActivity() {
    super(SidebarThingActivity.class);
  }

  @Override
  protected void setContentView() {
    setTheme(ThemePrefs.getTheme(this));
    setContentView(R.layout.sidebar);
  }

  @Override
  protected boolean skipSetup(Bundle savedInstanceState) {
    tabController = new TabController(bar, savedInstanceState);
    tabDescription = tabController.addTab(
        newTab(getString(R.string.tab_description)));
    tabRelated = tabController.addTab(newTab(getString(R.string.tab_related)));
    return false;
  }

  private Tab newTab(CharSequence text) {
    return bar.newTab().setText(text).setTabListener(this);
  }

  @Override
  protected void doSetup(Bundle savedInstanceState) {
    if (!hasSubredditName()) {
      setSubredditName("android");
    }
    bar.setDisplayHomeAsUpEnabled(true);
    getSupportLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
    return new AccountLoader(this, true, false);
  }

  @Override
  public void onLoadFinished(
      Loader<AccountResult> loader,
      AccountResult result) {
    accountResult = result;
    accountName = result.getLastAccount(this);
    tabController.setupTabs();
  }

  @Override
  public void onLoaderReset(Loader<AccountResult> loader) {
    accountResult = null;
    accountName = null;
  }

  @Override
  public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
    selectTab(tab);
  }

  @Override
  public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
    if (!isSinglePane) {
      getSupportFragmentManager().popBackStack();
    }
  }

  private void selectTab(Tab tab) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "selectTab tab: " + tab.getText());
    }
    if (tabController.selectTab(tab)) {
      if (tab == tabDescription) {
        refreshSidebarFragments();
      } else if (tab == tabRelated) {
        refreshRelatedSubredditFragments();
      }
    }
  }

  private void refreshSidebarFragments() {
    setSidebarFragments(accountName, getSubreddit());
  }

  private void refreshRelatedSubredditFragments() {
    setRelatedSubredditsFragments(accountName, getSubreddit());
  }

  @Override
  public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
  }

  @Override
  protected void refreshActionBar(ControlFragment controlFrag) {
    bar.setTitle(Subreddits.getTitle(this, getSubreddit()));
  }

  @Override
  protected boolean hasLeftFragment() {
    return tabController.isTabSelected(tabRelated);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    tabController.saveInstanceState(outState);
  }

  @Override
  public AccountResult getAccountResult() {
    return accountResult;
  }

  @Override
  public String getSubreddit() {
    return getIntent().getStringExtra(EXTRA_SUBREDDIT);
  }

  private boolean hasSubredditName() {
    return getIntent().hasExtra(EXTRA_SUBREDDIT);
  }

  private void setSubredditName(String subreddit) {
    getIntent().putExtra(EXTRA_SUBREDDIT, subreddit);
  }
}
