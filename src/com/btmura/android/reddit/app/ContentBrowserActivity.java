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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.ContentUriListFragment.OnUriClickListener;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;

public class ContentBrowserActivity extends FragmentActivity
    implements OnUriClickListener {

  private static final String[] AUTHORITIES = {
      AccountProvider.AUTHORITY,
      SubredditProvider.AUTHORITY,
      ThingProvider.AUTHORITY,
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(ThemePrefs.getTheme(this));
    setContentView(R.layout.content_browser);
    getActionBar().setDisplayHomeAsUpEnabled(true);

    if (savedInstanceState == null) {
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.replace(R.id.content_browser_container,
          ContentUriListFragment.newInstance());
      ft.commit();
    }
  }

  public void onUriClick(Uri uri) {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    ft.replace(R.id.content_browser_container,
        ContentRowListFragment.newInstance(uri));
    ft.addToBackStack(null);
    ft.commit();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    getMenuInflater().inflate(R.menu.debug_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;

      case R.id.menu_app_info:
        handleAppInfo();
        return true;

      case R.id.menu_sync_settings:
        handleSyncSettings();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void handleAppInfo() {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.parse("package:com.btmura.android.reddit"));
    startActivity(intent);
  }

  private void handleSyncSettings() {
    Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
    intent.putExtra(Settings.EXTRA_AUTHORITIES, AUTHORITIES);
    startActivity(intent);
  }
}
