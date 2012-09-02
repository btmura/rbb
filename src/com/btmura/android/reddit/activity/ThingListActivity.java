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
import android.app.ActionBar.OnNavigationListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditNameHolder;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.widget.FilterAdapter;

public class ThingListActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnNavigationListener,
        OnThingSelectedListener,
        SubredditNameHolder {

    public static final String TAG = "ThingListActivity";

    public static final String EXTRA_SUBREDDIT = "subreddit";
    public static final String EXTRA_FLAGS = "flags";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_NAVIGATION_INDEX = "navigationIndex";

    private ActionBar bar;
    private FilterAdapter adapter;
    private String accountName;
    private SharedPreferences prefs;
    private String subreddit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list);
        setInitialFragments(savedInstanceState);
        setActionBar(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar(Bundle savedInstanceState) {
        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        adapter = new FilterAdapter(this);
        adapter.setTitle(Subreddit.getTitle(this, subreddit));
        bar.setListNavigationCallbacks(adapter, this);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_NAVIGATION_INDEX));
        }
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        prefs = result.prefs;
        accountName = result.getLastAccount();
        adapter.addFilters(this);
        bar.setSelectedNavigationItem(result.getLastFilter());
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        accountName = null;
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNavigationItemSelected i:" + itemPosition);
        }

        int filter = adapter.getFilter(itemPosition);
        AccountLoader.setLastFilter(prefs, filter);

        ThingListFragment f = getThingListFragment();
        if (f == null || !f.getAccountName().equals(accountName)
                || f.getFilter() != filter) {
            f = ThingListFragment.newInstance(accountName, subreddit, filter, null, 0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.thing_list_container, f, ThingListFragment.TAG);
            ft.commit();
        }
        return true;
    }

    public void onThingSelected(Bundle thingBundle, int position) {
        Intent i = new Intent(this, ThingActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        i.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        startActivity(i);
    }

    public int onMeasureThingBody() {
        return 0;
    }

    public String getSubredditName() {
        return subreddit;
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
        if (insertBackStack()) {
            Intent intent = new Intent(this, BrowserActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private boolean insertBackStack() {
        int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
        return (flags & FLAG_INSERT_HOME) == FLAG_INSERT_HOME;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NAVIGATION_INDEX, bar.getSelectedNavigationIndex());
    }

    private ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager().findFragmentByTag(ThingListFragment.TAG);
    }
}
