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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.ControlFragment;
import com.btmura.android.reddit.browser.FilterAdapter;
import com.btmura.android.reddit.browser.Subreddit;
import com.btmura.android.reddit.browser.SubredditListFragment;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.browser.ThingListFragment;
import com.btmura.android.reddit.browser.ThingPagerAdapter;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.ThingMenuFragment;

public class BrowserActivity extends GlobalMenuActivity implements
        ActionBar.OnNavigationListener,
        SubredditListFragment.OnSubredditSelectedListener,
        ThingListFragment.OnThingSelectedListener,
        FragmentManager.OnBackStackChangedListener,
        ViewPager.OnPageChangeListener,
        ThingMenuFragment.ThingPagerHolder {

    public static final String TAG = "BrowserActivity";

    public static final String EXTRA_SUBREDDIT_NAME = "s";
    public static final String EXTRA_HOME_UP_ENABLED = "h";

    private static final String STATE_LAST_SELECTED_FILTER = "lastSelectedFilter";

    private static final int NAV_LAYOUT_ORIGINAL = 0;
    private static final int NAV_LAYOUT_SIDENAV = 1;

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SIDE_NAV = 2;
    private static final int ANIMATION_CLOSE_SIDE_NAV = 3;
    private static final int ANIMATION_EXPAND_NAV = 4;

    private ActionBar bar;
    private FilterAdapter filterAdapter;
    private int lastSelectedFilter;

    private View singleContainer;
    private View navContainer;
    private View subredditListContainer;
    private View thingClickAbsorber;
    private ViewPager thingPager;

    private int duration;
    private int fullNavWidth;
    private int sideNavWidth;
    private int subredditListWidth;
    private int thingBodyWidth;

    private AnimatorSet openNavAnimator;
    private AnimatorSet closeNavAnimator;
    private AnimatorSet openSideNavAnimator;
    private AnimatorSet closeSideNavAnimator;
    private AnimatorSet expandNavAnimator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        singleContainer = findViewById(R.id.single_container);
        if (singleContainer != null) {
            initSingleContainer(savedInstanceState);
        } else {
            initContainers(savedInstanceState);
        }
    }

    private void initSingleContainer(Bundle savedInstanceState) {
        Intent i = getIntent();
        getActionBar().setDisplayHomeAsUpEnabled(i.getBooleanExtra(EXTRA_HOME_UP_ENABLED, false));
        if (i.hasExtra(EXTRA_SUBREDDIT_NAME)) {
            finish();
            Subreddit s = Subreddit.newInstance(i.getStringExtra(EXTRA_SUBREDDIT_NAME));
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, s);
            intent.putExtra(ThingListActivity.EXTRA_INSERT_HOME_ACTIVITY, true);
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

    private void initContainers(Bundle savedInstanceState) {
        getFragmentManager().addOnBackStackChangedListener(this);

        filterAdapter = new FilterAdapter(this);

        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        thingPager = (ViewPager) findViewById(R.id.thing_pager);
        thingPager.setOnPageChangeListener(this);

        thingClickAbsorber = findViewById(R.id.thing_click_absorber);
        if (thingClickAbsorber != null) {
            thingClickAbsorber.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    runAnimation(ANIMATION_CLOSE_SIDE_NAV, null);
                }
            });
        }

        subredditListContainer = findViewById(R.id.subreddit_list_container);

        navContainer = findViewById(R.id.nav_container);
        if (navContainer != null) {
            initNavContainerAnimators();
        }

        initThingBodyWidth();

        if (savedInstanceState == null) {
            initFragments();
        }
    }

    private void initThingBodyWidth() {
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int padding = r.getDimensionPixelSize(R.dimen.padding);
        if (navContainer != null) {
            thingBodyWidth = dm.widthPixels / 2 - padding * 2;
        } else if (singleContainer == null) {
            int subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            thingBodyWidth = dm.widthPixels / 2 - padding * 2 - subredditListWidth;
        }
    }

    private void initNavContainerAnimators() {
        duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        fullNavWidth = getResources().getDisplayMetrics().widthPixels;
        sideNavWidth = fullNavWidth / 2;
        subredditListWidth = getResources().getDimensionPixelSize(R.dimen.subreddit_list_width);

        openNavAnimator = createOpenNavAnimator();
        closeNavAnimator = createCloseNavAnimator();
        openSideNavAnimator = createSideNavAnimator(true);
        closeSideNavAnimator = createSideNavAnimator(false);
        expandNavAnimator = createExpandNavAnimator();
    }

    private void initFragments() {
        Subreddit s = null;
        if (getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            s = Subreddit.newInstance(getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME));
        }

        ControlFragment cf = ControlFragment.newInstance(s, null, -1, lastSelectedFilter);
        GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
        SubredditListFragment slf = SubredditListFragment.newInstance(s, true);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(gmf, GlobalMenuFragment.TAG);
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);

        if (s != null) {
            refreshActionBar(s, null, lastSelectedFilter);
            ThingListFragment tlf = ThingListFragment.newInstance(s, lastSelectedFilter, true);
            ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        }

        ft.commit();
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int newFilter = (int) itemId;
        if (lastSelectedFilter != newFilter) {
            lastSelectedFilter = newFilter;
            selectSubreddit(getSubreddit(), lastSelectedFilter);
        }
        return true;
    }

    public void onSubredditLoaded(Subreddit subreddit) {
        if (singleContainer == null && !hasFragment(ThingListFragment.TAG)) {
            getSubredditListFragment().setSelectedSubreddit(subreddit);
            selectSubreddit(subreddit, lastSelectedFilter);
        }
    }

    public void onSubredditSelected(Subreddit subreddit) {
        if (singleContainer != null) {
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, subreddit);
            startActivity(intent);
        } else {
            selectSubreddit(subreddit, lastSelectedFilter);
        }
    }

    private void selectSubreddit(Subreddit subreddit, int filter) {
        FragmentManager fm = getFragmentManager();
        fm.removeOnBackStackChangedListener(this);
        fm.popBackStackImmediate();
        fm.addOnBackStackChangedListener(this);

        refreshActionBar(subreddit, null, filter);
        refreshContainers(null);

        ControlFragment cf = ControlFragment.newInstance(subreddit, null, -1, filter);
        ThingListFragment tlf = ThingListFragment.newInstance(subreddit, filter, true);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    private void refreshActionBar(Subreddit subreddit, Thing thing, int filter) {
        if (thing != null) {
            filterAdapter.setTitle(thing.assureTitle(this).title);
        } else if (subreddit != null) {
            filterAdapter.setTitle(subreddit.getTitle(this));
        } else {
            filterAdapter.setTitle(getString(R.string.app_name));
        }

        bar.setDisplayHomeAsUpEnabled(getIntent().hasExtra(EXTRA_HOME_UP_ENABLED)
                || getFragmentManager().getBackStackEntryCount() > 0);
    }

    private void refreshContainers(Thing thing) {
        if (navContainer != null) {
            int currVisibility = navContainer.getVisibility();
            int nextVisibility = thing == null ? View.VISIBLE : View.GONE;
            if (currVisibility != nextVisibility) {
                if (nextVisibility == View.VISIBLE) {
                    runAnimation(ANIMATION_OPEN_NAV, null);
                } else {
                    runAnimation(ANIMATION_CLOSE_NAV, null);
                }
            } else if (isSideNavShowing()) {
                runAnimation(ANIMATION_EXPAND_NAV, null);
            }
        } else {
            thingPager.setVisibility(thing != null ? View.VISIBLE : View.GONE);
            if (thing == null) {
                // Avoid nested executePendingTransactions that would occur by
                // doing popBackStack.
                thingPager.post(new Runnable() {
                    public void run() {
                        updateThingPager(null);
                    }
                });
            }
        }
    }

    public void onThingSelected(final Thing thing, final int position) {
        if (navContainer != null && isSideNavShowing()) {
            runAnimation(ANIMATION_CLOSE_SIDE_NAV, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeListener(this);
                    selectThing(thing, position);
                }
            });
        } else {
            selectThing(thing, position);
        }
    }

    private void selectThing(Thing thing, int position) {
        FragmentManager fm = getFragmentManager();
        fm.removeOnBackStackChangedListener(this);
        fm.popBackStackImmediate();
        fm.addOnBackStackChangedListener(this);

        ControlFragment cf = ControlFragment.newInstance(getSubreddit(), thing, position,
                getFilter());
        ThingMenuFragment tmf = ThingMenuFragment.newInstance(thing);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.add(tmf, ThingMenuFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();

        updateThingPager(thing);
    }

    private void updateThingPager(Thing thing) {
        if (thing != null) {
            FragmentManager fm = getFragmentManager();
            ThingPagerAdapter adapter = new ThingPagerAdapter(fm, thing);
            thingPager.setAdapter(adapter);
        } else {
            thingPager.setAdapter(null);
        }
    }

    public void onBackStackChanged() {
        ControlFragment cf = getControlFragment();
        refreshActionBar(cf.getSubreddit(), cf.getThing(), getFilter());
        refreshCheckedItems();
        refreshContainers(cf.getThing());
        invalidateOptionsMenu();
    }

    private void refreshCheckedItems() {
        if (isVisible(SubredditListFragment.TAG)) {
            getSubredditListFragment().setSelectedSubreddit(getSubreddit());
        }

        if (isVisible(ThingListFragment.TAG)) {
            ControlFragment f = getControlFragment();
            getThingListFragment().setSelectedThing(f.getThing(), f.getThingPosition());
        }
    }

    private boolean hasFragment(String tag) {
        return getFragmentManager().findFragmentByTag(tag) != null;
    }

    private boolean isVisible(String tag) {
        Fragment f = getFragmentManager().findFragmentByTag(tag);
        return f != null && f.isAdded();
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_LAST_SELECTED_FILTER, lastSelectedFilter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (singleContainer != null) {
            return;
        }

        if (savedInstanceState != null) {
            lastSelectedFilter = savedInstanceState.getInt(STATE_LAST_SELECTED_FILTER);
            updateThingPager(getThing());
            onBackStackChanged();
        }
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
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            if (navContainer != null && !isSideNavShowing()) {
                runAnimation(ANIMATION_OPEN_SIDE_NAV, null);
            } else {
                fm.popBackStack();
            }
        } else {
            finish();
        }
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
            case ANIMATION_EXPAND_NAV:
                return expandNavAnimator;
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
                updateThingPager(null);
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

    private AnimatorSet createExpandNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0,
                subredditListWidth);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager, "translationX", sideNavWidth,
                fullNavWidth);
        ncTransX.setDuration(duration);
        tpTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                changeNavContainerLayout(NAV_LAYOUT_ORIGINAL);
                navContainer.setTranslationX(0);

                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.GONE);
                updateThingPager(null);
            }
        });
        return as;
    }

    private boolean isSideNavShowing() {
        return thingClickAbsorber.isShown();
    }

    private void changeNavContainerLayout(int layout) {
        int subredditListVisibility;
        int clickAbsorberVisibility;
        switch (layout) {
            case NAV_LAYOUT_ORIGINAL:
                subredditListVisibility = View.VISIBLE;
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

    public int getThingBodyWidth() {
        return thingBodyWidth;
    }

    public ViewPager getPager() {
        return thingPager;
    }

    private Subreddit getSubreddit() {
        return getControlFragment().getSubreddit();
    }

    private Thing getThing() {
        return getControlFragment().getThing();
    }

    private int getFilter() {
        return getControlFragment().getFilter();
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

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void onNothingSelected(android.widget.AdapterView<?> adapter) {
    }
}
