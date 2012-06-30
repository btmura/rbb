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
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.LoaderIds;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
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

    public static final String EXTRA_SUBREDDIT = "s";
    public static final String EXTRA_FLAGS = "f";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_NAVIGATION_INDEX = "ni";

    private ActionBar bar;
    private FilterAdapter adapter;
    private String accountName;

    private Subreddit subreddit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list);
        setInitialFragments(savedInstanceState);
        setActionBar(savedInstanceState);
        getLoaderManager().initLoader(LoaderIds.ACCOUNTS, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar(Bundle savedInstanceState) {
        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);
        adapter = new FilterAdapter(this);
        adapter.setTitle(subreddit.getTitle(this));
        bar.setListNavigationCallbacks(adapter, this);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_NAVIGATION_INDEX));
        }
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountName = AccountLoader.getLastAccount(result.prefs, result.accountNames);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        accountName = null;
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int filter = adapter.getFilter(itemPosition);
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

    public void onThingSelected(Thing thing, int position) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingActivity.EXTRA_THING, thing);
        startActivity(intent);
    }

    public int onMeasureThingBody() {
        return 0;
    }

    public String getSubredditName() {
        return subreddit.name;
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
        finish();
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
