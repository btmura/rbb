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
import android.app.Activity;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.LoaderIds;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.ControlFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.fragment.SubredditNameHolder;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.fragment.ThingMenuFragment;
import com.btmura.android.reddit.fragment.ThingMenuFragment.ThingPagerHolder;
import com.btmura.android.reddit.widget.AccountSpinnerAdapter;
import com.btmura.android.reddit.widget.ThingPagerAdapter;

public class LoginBrowserActivity extends Activity implements
        LoaderCallbacks<AccountResult>,
        OnNavigationListener,
        OnSubredditSelectedListener,
        OnThingSelectedListener,
        OnBackStackChangedListener,
        SubredditNameHolder,
        ThingPagerHolder {

    public static final String TAG = "LoginBrowserActivity";

    private ActionBar bar;
    private AccountSpinnerAdapter adapter;

    private boolean isSinglePane;
    private ViewPager thingPager;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_STRICT_MODE) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        setInitialFragments(savedInstanceState);
        setActionBar();
        setViews();
        getLoaderManager().initLoader(LoaderIds.ACCOUNTS, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(GlobalMenuFragment.FLAG_SHOW_MANAGE_ACCOUNTS),
                    GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar() {
        bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        adapter = new AccountSpinnerAdapter(this);
        bar.setListNavigationCallbacks(adapter, this);
    }

    private void setViews() {
        isSinglePane = findViewById(R.id.thing_list_container) == null;
        if (!isSinglePane) {
            thingPager = (ViewPager) findViewById(R.id.thing_pager);
            getFragmentManager().addOnBackStackChangedListener(this);
        }
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        adapter.setAccountNames(result.accountNames);
        prefs = result.prefs;
        int index = AccountLoader.getLastAccountIndex(prefs, result.accountNames);
        bar.setSelectedNavigationItem(index);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (Debug.DEBUG_ACTIVITY) {
            Log.d(TAG, "onNavigationItemSelected itemPosition:" + itemPosition);
        }
        String accountName = adapter.getItem(itemPosition);
        AccountLoader.setLastAccount(prefs, accountName);

        SubredditListFragment f = getSubredditListFragment();
        if (f == null || !f.getAccountName().equals(accountName)) {
            f = SubredditListFragment.newInstance(accountName, null, 0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.subreddit_list_container, f, SubredditListFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }

        return true;
    }

    public void onSubredditLoaded(Subreddit subreddit) {
        if (!isSinglePane) {
            loadSubredditMultiPane(subreddit);
        }
    }

    protected void loadSubredditMultiPane(Subreddit subreddit) {
        ThingListFragment tf = getThingListFragment();
        if (tf == null) {
            ControlFragment cf = ControlFragment.newInstance(subreddit, null, -1, 0);
            tf = ThingListFragment.newInstance(getAccountName(), subreddit, 0, 0);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(cf, ControlFragment.TAG);
            ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }
    }

    public void onSubredditSelected(Subreddit subreddit) {
        if (isSinglePane) {
            selectSubredditSinglePane(subreddit);
        } else {
            selectSubredditMultiPane(subreddit);
        }
    }

    protected void selectSubredditSinglePane(Subreddit subreddit) {
        Intent intent = new Intent(this, ThingListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(ThingListActivity.EXTRA_FLAGS, 0);
        startActivity(intent);
    }

    protected void selectSubredditMultiPane(Subreddit subreddit) {
        getFragmentManager().popBackStack();

        ControlFragment cf = ControlFragment.newInstance(subreddit, null, -1, 0);
        ThingListFragment tf = ThingListFragment.newInstance(getAccountName(), subreddit, 0, 0);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    public void onThingSelected(Thing thing, int position) {
        if (isSinglePane) {
            selectThingSinglePane(thing);
        } else {
            selectThingMultiPane(thing, position);
        }
    }

    protected void selectThingSinglePane(Thing thing) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingActivity.EXTRA_THING, thing);
        intent.putExtra(ThingActivity.EXTRA_FLAGS, 0);
        startActivity(intent);
    }

    protected void selectThingMultiPane(Thing thing, int thingPosition) {
        getFragmentManager().popBackStack();

        ControlFragment cf = getControlFragment();
        cf = ControlFragment.newInstance(cf.getSubreddit(), thing, thingPosition, 0);
        ThingMenuFragment tf = ThingMenuFragment.newInstance(thing);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.add(tf, ThingMenuFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();

        ThingPagerAdapter adapter = new ThingPagerAdapter(getFragmentManager(), thing);
        thingPager.setAdapter(adapter);
    }

    public int getThingBodyWidth() {
        return 0;
    }

    public void onBackStackChanged() {
        boolean hasThing = getThingMenuFragment() != null;
        bar.setDisplayHomeAsUpEnabled(hasThing);
        thingPager.setVisibility(hasThing ? View.VISIBLE : View.GONE);
    }

    public CharSequence getSubredditName() {
        return null;
    }

    public ViewPager getPager() {
        return thingPager;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String getAccountName() {
        return adapter.getAccountName(bar.getSelectedNavigationIndex());
    }

    private ControlFragment getControlFragment() {
        return (ControlFragment) getFragmentManager().findFragmentByTag(ControlFragment.TAG);
    }

    private SubredditListFragment getSubredditListFragment() {
        return (SubredditListFragment) getFragmentManager().findFragmentByTag(SubredditListFragment.TAG);
    }

    private ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager().findFragmentByTag(ThingListFragment.TAG);
    }

    private ThingMenuFragment getThingMenuFragment() {
        return (ThingMenuFragment) getFragmentManager().findFragmentByTag(ThingMenuFragment.TAG);
    }
}
