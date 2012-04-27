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

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.ControlFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;

public class BrowserActivity extends AbstractBrowserActivity {

    public static final String TAG = "BrowserActivity";

    public static final String EXTRA_SUBREDDIT_NAME = "s";
    public static final String EXTRA_HOME_UP_ENABLED = "h";
    public static final String EXTRA_SHOW_ADD_BUTTON = "a";

    private ActionBar bar;
    private FilterAdapter filterAdapter;

    private View singleContainer;

    private boolean homeUpEnabled;
    private boolean showAddButton;

    public BrowserActivity() {
        super(R.layout.browser);
    }

    @Override
    protected void initPrereqs(Bundle savedInstanceState) {
        homeUpEnabled = getIntent().getBooleanExtra(EXTRA_HOME_UP_ENABLED, false);
        showAddButton = getIntent().getBooleanExtra(EXTRA_SHOW_ADD_BUTTON, false);
        singleContainer = findViewById(R.id.single_container);
    }

    @Override
    protected boolean isSinglePane() {
        return singleContainer != null;
    }

    @Override
    protected void initSinglePaneLayout(Bundle savedInstanceState) {
        getActionBar().setDisplayHomeAsUpEnabled(homeUpEnabled);
        if (getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            finish();
            Subreddit s = Subreddit.newInstance(getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME));
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, s);
            intent.putExtra(ThingListActivity.EXTRA_INSERT_HOME_ACTIVITY, true);
            intent.putExtra(ThingListActivity.EXTRA_SHOW_ADD_BUTTON, showAddButton);
            startActivity(intent);
        } else if (savedInstanceState == null) {
            SubredditListFragment slf = SubredditListFragment.newInstance(null, false);
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
            ft.commit();
        }
    }

    @Override
    protected void initMultiPaneLayout(Bundle savedInstanceState, int filter) {
        filterAdapter = new FilterAdapter(this);

        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if (savedInstanceState == null) {
            initFragments(filter);
        }
    }

    private void initFragments(int lastFilter) {
        Subreddit s = null;
        if (getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            s = Subreddit.newInstance(getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME));
        }

        ControlFragment cf = ControlFragment.newInstance(s, null, -1, lastFilter);
        GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
        SubredditListFragment slf = SubredditListFragment.newInstance(s, true);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(gmf, GlobalMenuFragment.TAG);
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);

        if (s != null) {
            refreshActionBar(s, null, lastFilter);
            Fragment tlf = ThingListFragment.newInstance(s, lastFilter, showAddButton, true);
            ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        }

        ft.commit();
    }

    @Override
    protected void refreshActionBar(Subreddit subreddit, Thing thing, int filter) {
        if (thing != null) {
            filterAdapter.setTitle(thing.assureTitle(this).title);
        } else if (subreddit != null) {
            filterAdapter.setTitle(subreddit.getTitle(this));
        } else {
            filterAdapter.setTitle(getString(R.string.app_name));
        }

        bar.setDisplayHomeAsUpEnabled(homeUpEnabled
                || getFragmentManager().getBackStackEntryCount() > 0);
    }
}
