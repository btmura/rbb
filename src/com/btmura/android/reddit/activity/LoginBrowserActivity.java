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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
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
        OnPageChangeListener,
        SubredditNameHolder,
        ThingPagerHolder {

    public static final String TAG = "LoginBrowserActivity";

    private ActionBar bar;
    private AccountSpinnerAdapter adapter;

    private boolean isSinglePane;
    private int slfFlags;
    private int tlfFlags;
    private ViewPager thingPager;
    private View navContainer;
    private int thingBodyWidth;

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
            slfFlags |= SubredditListFragment.FLAG_SINGLE_CHOICE;
            tlfFlags |= ThingListFragment.FLAG_SINGLE_CHOICE;

            thingPager = (ViewPager) findViewById(R.id.thing_pager);
            thingPager.setOnPageChangeListener(this);

            navContainer = findViewById(R.id.nav_container);

            Resources r = getResources();
            DisplayMetrics dm = r.getDisplayMetrics();
            int padding = r.getDimensionPixelSize(R.dimen.element_padding);
            int subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            if (navContainer != null) {
                thingBodyWidth = dm.widthPixels / 2 - padding * 4;
            } else {
                thingBodyWidth = dm.widthPixels / 2 - padding * 3 - subredditListWidth;
            }

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
            f = SubredditListFragment.newInstance(accountName, null, slfFlags);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.subreddit_list_container, f, SubredditListFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        }

        return true;
    }

    public void onInitialSubredditSelected(Subreddit subreddit) {
        if (!isSinglePane) {
            selectInitialSubredditMultiPane(subreddit);
        }
    }

    protected void selectInitialSubredditMultiPane(Subreddit subreddit) {
        ThingListFragment tlf = getThingListFragment();
        if (tlf == null) {
            SubredditListFragment slf = getSubredditListFragment();
            slf.setSelectedSubreddit(subreddit);

            ControlFragment cf = ControlFragment.newInstance(subreddit, null, -1, 0);
            tlf = ThingListFragment.newInstance(getAccountName(), subreddit, 0, tlfFlags);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(cf, ControlFragment.TAG);
            ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
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
        ThingListFragment tlf = ThingListFragment.newInstance(getAccountName(), subreddit, 0,
                tlfFlags);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
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

        refreshThingPager(thing);
    }

    public int onMeasureThingBody() {
        return thingBodyWidth;
    }

    public void onBackStackChanged() {
        refresh();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (!isSinglePane && savedInstanceState != null) {
            refresh();
        }
    }

    private void refresh() {
        ControlFragment cf = getControlFragment();
        final Thing thing = cf.getThing();
        boolean hasThing = thing != null;

        bar.setDisplayHomeAsUpEnabled(hasThing);
        thingPager.setVisibility(hasThing ? View.VISIBLE : View.GONE);
        thingPager.post(new Runnable() {
            public void run() {
                refreshThingPager(thing);
            }
        });

        SubredditListFragment slf = getSubredditListFragment();
        slf.setSelectedSubreddit(cf.getSubreddit());
        ThingListFragment tlf = getThingListFragment();
        tlf.setSelectedThing(cf.getThing(), cf.getThingPosition());
    }

    private void refreshThingPager(Thing thing) {
        if (thing != null) {
            ThingPagerAdapter adapter = new ThingPagerAdapter(getFragmentManager(), thing);
            thingPager.setAdapter(adapter);
        } else {
            thingPager.setAdapter(null);
        }
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    public CharSequence getSubredditName() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            return Subreddit.getName(cf.getSubreddit());
        } else {
            return null;
        }
    }

    public ViewPager getPager() {
        return thingPager;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        hideExtraViewSidebarItems(menu);
        return true;
    }

    private void hideExtraViewSidebarItems(Menu menu) {
        MenuItem thingSidebarItem = menu.findItem(R.id.menu_view_thing_sidebar);
        if (thingSidebarItem != null) {
            MenuItem subredditSidebarItem = menu.findItem(R.id.menu_view_subreddit_sidebar);
            if (subredditSidebarItem != null) {
                subredditSidebarItem.setVisible(false);
            }
        }
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
        return (SubredditListFragment) getFragmentManager().findFragmentByTag(
                SubredditListFragment.TAG);
    }

    private ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager().findFragmentByTag(ThingListFragment.TAG);
    }
}
