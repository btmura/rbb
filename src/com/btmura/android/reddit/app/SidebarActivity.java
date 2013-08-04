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
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.database.Subreddits;

public class SidebarActivity extends AbstractBrowserActivity
        implements TabListener, SubredditNameHolder {

    public static final String EXTRA_SUBREDDIT = "subreddit";

    private Tab aboutTab;
    private Tab relatedTab;

    @Override
    protected void setContentView() {
        setTheme(ThemePrefs.getTheme(this));
        setContentView(R.layout.sidebar);
    }

    @Override
    protected boolean skipSetup() {
        return false;
    }

    @Override
    protected void setupViews() {
        if (!hasSubredditName()) {
            setSubredditName("android");
        }
    }

    @Override
    protected void setupActionBar(Bundle savedInstanceState) {
        bar.setDisplayHomeAsUpEnabled(true);
        setupTabs(savedInstanceState);
    }

    private void setupTabs(Bundle savedInstanceState) {
        aboutTab = addTab(getString(R.string.tab_about));
        relatedTab = addTab(getString(R.string.tab_related));
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    private Tab addTab(CharSequence text) {
        Tab tab = bar.newTab().setText(text).setTabListener(this);
        bar.addTab(tab);
        return tab;
    }

    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
        if (tab == aboutTab) {
            setSidebarFragments(Subreddits.ACCOUNT_NONE, getSubredditName());
        }
    }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
    }

    @Override
    protected void refreshActionBar(ControlFragment controlFrag) {
        setTitle(Subreddits.getTitle(this, getSubredditName()));
    }

    @Override
    protected boolean hasLeftFragment() {
        return false;
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
    public String getSubredditName() {
        return getIntent().getStringExtra(EXTRA_SUBREDDIT);
    }

    private boolean hasSubredditName() {
        return getIntent().hasExtra(EXTRA_SUBREDDIT);
    }

    private void setSubredditName(String subreddit) {
        getIntent().putExtra(EXTRA_SUBREDDIT, subreddit);
    }
}
