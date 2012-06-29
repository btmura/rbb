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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.FragmentManager;
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
import android.view.View.OnClickListener;

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

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SIDE_NAV = 2;
    private static final int ANIMATION_CLOSE_SIDE_NAV = 3;
    private static final int ANIMATION_EXPAND_SUBREDDIT_NAV = 4;
    private static final int ANIMATION_EXPAND_THING_NAV = 5;

    private static final int NAV_LAYOUT_ORIGINAL = 0;
    private static final int NAV_LAYOUT_SIDENAV = 1;

    private ActionBar bar;
    private AccountSpinnerAdapter adapter;

    private boolean isSinglePane;
    private int slfFlags;
    private int tlfFlags;
    private ViewPager thingPager;

    private View navContainer;
    private View subredditListContainer;
    private int subredditListWidth;
    private int thingBodyWidth;

    private View thingClickAbsorber;
    private int fullNavWidth;
    private int sideNavWidth;
    private int duration;

    private AnimatorSet openNavAnimator;
    private AnimatorSet closeNavAnimator;
    private AnimatorSet openSideNavAnimator;
    private AnimatorSet closeSideNavAnimator;
    private AnimatorSet expandSubredditNavAnimator;
    private AnimatorSet expandThingNavAnimator;

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
            subredditListContainer = findViewById(R.id.subreddit_list_container);

            Resources r = getResources();
            DisplayMetrics dm = r.getDisplayMetrics();
            subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            int padding = r.getDimensionPixelSize(R.dimen.element_padding);
            if (navContainer != null) {
                thingBodyWidth = dm.widthPixels / 2 - padding * 4;
            } else {
                thingBodyWidth = dm.widthPixels / 2 - padding * 3 - subredditListWidth;
            }

            if (navContainer != null) {
                thingClickAbsorber = findViewById(R.id.thing_click_absorber);
                if (thingClickAbsorber != null) {
                    thingClickAbsorber.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            runAnimation(ANIMATION_CLOSE_SIDE_NAV, null);
                        }
                    });
                }

                fullNavWidth = dm.widthPixels;
                sideNavWidth = fullNavWidth / 2;
                duration = r.getInteger(android.R.integer.config_shortAnimTime);

                openNavAnimator = createOpenNavAnimator();
                closeNavAnimator = createCloseNavAnimator();
                openSideNavAnimator = createSideNavAnimator(true);
                closeSideNavAnimator = createSideNavAnimator(false);
                expandSubredditNavAnimator = createExpandNavAnimator(true);
                expandThingNavAnimator = createExpandNavAnimator(false);
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
        safePopBackStackImmediate();

        ControlFragment cf = ControlFragment.newInstance(subreddit, null, -1, 0);
        ThingListFragment tlf = ThingListFragment.newInstance(getAccountName(), subreddit, 0,
                tlfFlags);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        refreshViews(null);
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
        safePopBackStackImmediate();

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

    private void safePopBackStackImmediate() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }
    }

    public int onMeasureThingBody() {
        return thingBodyWidth;
    }

    public void onBackStackChanged() {
        Thing thing = getControlFragment().getThing();
        refreshViews(thing);
        refreshCheckedItems();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (!isSinglePane && savedInstanceState != null) {
            Thing thing = getControlFragment().getThing();
            refreshThingPager(thing);
            refreshViews(thing);
            refreshCheckedItems();
        }
    }

    private void refreshViews(Thing thing) {
        boolean hasThing = thing != null;
        bar.setDisplayHomeAsUpEnabled(hasThing);
        if (navContainer != null) {
            int currVisibility = navContainer.getVisibility();
            int nextVisibility = !hasThing ? View.VISIBLE : View.GONE;
            if (currVisibility != nextVisibility) {
                if (nextVisibility == View.VISIBLE) {
                    runAnimation(ANIMATION_OPEN_NAV, null);
                } else {
                    runAnimation(ANIMATION_CLOSE_NAV, null);
                }
            } else if (isSideNavShowing()) {
                if (hasSubredditList()) {
                    runAnimation(ANIMATION_EXPAND_SUBREDDIT_NAV, null);
                } else {
                    runAnimation(ANIMATION_EXPAND_THING_NAV, null);
                }
            }
        } else {
            thingPager.setVisibility(hasThing ? View.VISIBLE : View.GONE);
            if (!hasThing) {
                // Avoid nested executePendingTransactions that would occur by
                // doing popBackStack. This is a hack to get around stale
                // adapter issues with the ViewPager after orientation changes.
                thingPager.post(new Runnable() {
                    public void run() {
                        refreshThingPager(null);
                    }
                });
            }
        }
    }

    private void refreshCheckedItems() {
        ControlFragment cf = getControlFragment();
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
                handleHome();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleHome() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            if (navContainer != null && !isSideNavShowing()) {
                runAnimation(ANIMATION_OPEN_SIDE_NAV, null);
            } else {
                fm.popBackStack();
            }
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

    private void runAnimation(int type, AnimatorListener listener) {
        navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        navContainer.setVisibility(View.VISIBLE);
        thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        thingPager.setVisibility(View.VISIBLE);
        AnimatorSet as = getAnimator(type);
        if (listener != null) {
            as.addListener(listener);
        }
        as.start();
    }

    private AnimatorSet getAnimator(int type) {
        switch (type) {
            case ANIMATION_OPEN_NAV:
                return openNavAnimator;
            case ANIMATION_CLOSE_NAV:
                return closeNavAnimator;
            case ANIMATION_OPEN_SIDE_NAV:
                return openSideNavAnimator;
            case ANIMATION_CLOSE_SIDE_NAV:
                return closeSideNavAnimator;
            case ANIMATION_EXPAND_SUBREDDIT_NAV:
                return expandSubredditNavAnimator;
            case ANIMATION_EXPAND_THING_NAV:
                return expandThingNavAnimator;
            default:
                throw new IllegalArgumentException();
        }
    }

    private AnimatorSet createOpenNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX",
                -fullNavWidth, 0).setDuration(duration);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", 0,
                fullNavWidth).setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                changeNavContainerLayout(NAV_LAYOUT_ORIGINAL);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.GONE);
                refreshThingPager(null);
            }
        });
        return as;
    }

    private AnimatorSet createCloseNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0,
                -subredditListWidth).setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                thingPager.setVisibility(View.GONE);
                thingPager.setTranslationX(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                navContainer.setVisibility(View.GONE);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }

    private AnimatorSet createSideNavAnimator(final boolean open) {
        ObjectAnimator ncTransX;
        ObjectAnimator tpTransX;
        if (open) {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", -sideNavWidth, 0);
            tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", 0, sideNavWidth);
        } else {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0, -sideNavWidth);
            tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", sideNavWidth, 0);
        }
        ncTransX.setDuration(duration);
        tpTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                changeNavContainerLayout(NAV_LAYOUT_SIDENAV);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                if (!open) {
                    navContainer.setVisibility(View.GONE);
                }
            }
        });
        return as;
    }

    private AnimatorSet createExpandNavAnimator(boolean showSubreddits) {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0,
                subredditListWidth);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", sideNavWidth,
                fullNavWidth);
        ncTransX.setDuration(duration);
        tpTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        if (showSubreddits) {
            as.play(ncTransX).with(tpTransX);
        } else {
            as.play(tpTransX);
        }

        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                changeNavContainerLayout(NAV_LAYOUT_ORIGINAL);
                navContainer.setTranslationX(0);

                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.GONE);
                refreshThingPager(null);
            }
        });
        return as;
    }

    private boolean isSideNavShowing() {
        return thingClickAbsorber != null && thingClickAbsorber.isShown();
    }

    private void changeNavContainerLayout(int layout) {
        int subredditListVisibility;
        int clickAbsorberVisibility;
        switch (layout) {
            case NAV_LAYOUT_ORIGINAL:
                subredditListVisibility = hasSubredditList() ? View.VISIBLE : View.GONE;
                clickAbsorberVisibility = View.GONE;
                break;

            case NAV_LAYOUT_SIDENAV:
                subredditListVisibility = View.GONE;
                clickAbsorberVisibility = View.VISIBLE;
                break;

            default:
                throw new IllegalStateException();
        }

        subredditListContainer.setVisibility(subredditListVisibility);
        thingClickAbsorber.setVisibility(clickAbsorberVisibility);
    }

    private boolean hasSubredditList() {
        return true;
    }
}
