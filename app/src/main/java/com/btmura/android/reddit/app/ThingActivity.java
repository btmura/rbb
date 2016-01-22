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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.Loader;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.util.Strings;

public class ThingActivity extends GlobalMenuActivity implements
    LoaderCallbacks<AccountResult>,
    OnThingEventListener,
    AccountNameHolder,
    SubredditHolder {

  public static final String EXTRA_THING_BUNDLE = "thingBundle";

  private static final String THING_FRAGMENT_TAG = "thing";

  private String accountName;
  private ThingBundle thingBundle;
  private final MarkdownFormatter formatter = new MarkdownFormatter();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(ThemePrefs.getTheme(this));
    setContentView(R.layout.thing);
    setupPrereqs();
    setupFragments(savedInstanceState);
    setupActionBar();
    getSupportLoaderManager().initLoader(0, null, this);
  }

  private void setupPrereqs() {
    thingBundle = getIntent().getParcelableExtra(EXTRA_THING_BUNDLE);
  }

  private void setupFragments(Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
      ft.commit();
    }
  }

  private void setupActionBar() {
    ActionBar bar = getActionBar();
    bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
        | ActionBar.DISPLAY_HOME_AS_UP
        | ActionBar.DISPLAY_SHOW_TITLE);
    refreshTitle(thingBundle.hasLinkId()
        ? thingBundle.getLinkTitle()
        : thingBundle.getTitle());
  }

  private void refreshTitle(String title) {
    setTitle(Strings.toString(formatter.formatAll(this, title)));
  }

  @Override
  public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
    return new AccountLoader(this, true, false);
  }

  @Override
  public void onLoadFinished(
      Loader<AccountResult> loader,
      AccountResult result) {
    this.accountName = result.getLastAccount(this);
    ThingFragment frag = ThingFragment.newInstance(accountName, thingBundle);
    if (!Objects.equals(frag, getThingFragment())) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.replace(R.id.thing_container, frag, THING_FRAGMENT_TAG);
      ft.commitAllowingStateLoss();
      invalidateOptionsMenu();
    }
  }

  @Override
  public void onLoaderReset(Loader<AccountResult> loader) {
  }

  @Override
  public void onThingTitleDiscovery(String title) {
    refreshTitle(title);
  }

  @Override
  public String getAccountName() {
    return accountName;
  }

  @Override
  public String getSubreddit() {
    return thingBundle.getSubreddit();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        handleHome();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void handleHome() {
    Intent upIntent = NavUtils.getParentActivityIntent(this);
    upIntent.putExtra(BrowserActivity.EXTRA_SUBREDDIT, getSubreddit());
    if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
      TaskStackBuilder.create(this)
          .addNextIntentWithParentStack(upIntent)
          .startActivities();
    } else {
      NavUtils.navigateUpFromSameTask(this);
    }
  }

  private ThingFragment getThingFragment() {
    return (ThingFragment) getSupportFragmentManager()
        .findFragmentByTag(THING_FRAGMENT_TAG);
  }
}
