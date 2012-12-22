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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.app.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingBundle;

abstract class AbstractBrowserActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnSubredditSelectedListener,
        OnThingSelectedListener,
        OnThingEventListener,
        OnBackStackChangedListener,
        SubredditNameHolder {

    public static final String TAG = "AbstractBrowserActivity";

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SUBREDDIT_LIST = 2;
    private static final int ANIMATION_CLOSE_SUBREDDIT_LIST = 3;

    protected ActionBar bar;

    protected boolean isSinglePane;
    private ViewPager thingPager;
    private int slfFlags;
    private int tlfFlags;

    private View navContainer;
    private View subredditListContainer;
    private View thingListContainer;
    private int subredditListWidth;
    private int thingBodyWidth;

    private int fullNavWidth;
    private int duration;

    private AnimatorSet openNavAnimator;
    private AnimatorSet closeNavAnimator;
    private AnimatorSet openSubredditListAnimator;
    private AnimatorSet closeSubredditListAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView();
        setupPrereqs();
        if (!skipSetup()) {
            setupCommonFragments(savedInstanceState);
            setupCommonViews();
            setupViews();
            setupActionBar(savedInstanceState);
            getLoaderManager().initLoader(0, null, this);
        }
    }

    protected abstract void setContentView();

    private void setupPrereqs() {
        bar = getActionBar();
        isSinglePane = findViewById(R.id.thing_pager) == null;
    }

    protected abstract boolean skipSetup();

    private void setupCommonFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(),
                    GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setupCommonViews() {
        if (!isSinglePane) {
            getFragmentManager().addOnBackStackChangedListener(this);

            slfFlags |= SubredditListFragment.FLAG_SINGLE_CHOICE;
            tlfFlags |= ThingListFragment.FLAG_SINGLE_CHOICE;

            navContainer = findViewById(R.id.nav_container);
            subredditListContainer = findViewById(R.id.subreddit_list_container);
            thingListContainer = findViewById(R.id.thing_list_container);
            thingPager = (ViewPager) findViewById(R.id.thing_pager);

            Resources r = getResources();
            subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            duration = r.getInteger(android.R.integer.config_shortAnimTime);
            fullNavWidth = r.getDisplayMetrics().widthPixels;

            refreshSubredditListVisibility();
        }
    }

    // TODO: Do we need this method or can it be rolled into setupPrereqs?
    protected abstract void setupViews();

    protected abstract void setupActionBar(Bundle savedInstanceState);

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this, true);
    }

    public abstract void onLoadFinished(Loader<AccountResult> loader, AccountResult result);

    public abstract void onLoaderReset(Loader<AccountResult> loader);

    protected abstract String getAccountName();

    protected abstract int getFilter();

    protected abstract boolean hasSubredditList();

    protected void setSubredditListNavigation(String subreddit, String query) {
        if (isSinglePane) {
            setSubredditListNavigationSinglePane(query);
        } else {
            setSubredditListNavigationMultiPane(subreddit, query);
        }
    }

    private void setSubredditListNavigationSinglePane(String query) {
        SubredditListFragment slf = SubredditListFragment.newInstance(getAccountName(), null,
                query, slfFlags);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setSubredditListNavigationMultiPane(String subreddit, String query) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setSubredditListNavigation subreddit:" + subreddit + " query: " + query);
        }
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        ControlFragment cf = ControlFragment.newInstance(accountName, subreddit, null, filter);
        SubredditListFragment slf = SubredditListFragment.newInstance(accountName, subreddit,
                query, slfFlags);
        ThingListFragment tlf = ThingListFragment.newSubredditInstance(accountName, subreddit,
                filter, tlfFlags);
        ThingMenuFragment tmf = getThingMenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        if (tmf != null) {
            ft.remove(tmf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshSubredditListVisibility();
        refreshActionBar(subreddit, null);
        refreshViews(null);
    }

    protected void setQueryThingListNavigation(String query) {
        setThingListNavigation(query, null, null);
    }

    protected void setProfileThingListNavigation(String profileUser) {
        setThingListNavigation(null, profileUser, null);
    }

    protected void setMessageThingListNavigation(String messageUser) {
        setThingListNavigation(null, null, messageUser);
    }

    private void setThingListNavigation(String query, String profileUser, String messageUser) {
        if (isSinglePane) {
            setThingListNavigationSinglePane(query, profileUser, messageUser);
        } else {
            setThingListNavigationMultiPane(query, profileUser, messageUser);
        }
    }

    private void setThingListNavigationSinglePane(String query, String profileUser,
            String messageUser) {
        String accountName = getAccountName();
        int filter = getFilter();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationSinglePane accountName: " + accountName
                    + " query: " + query
                    + " profileUser: " + profileUser
                    + " messageUser: " + messageUser
                    + " filter: " + filter);
        }

        ThingListFragment tlf = ThingListFragment.newInstance(accountName, query, profileUser,
                messageUser, filter, tlfFlags);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setThingListNavigationMultiPane(String query, String profileUser,
            String messageUser) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationMultiPane accountName: " + accountName
                    + " query: " + query
                    + " profileUser: " + profileUser
                    + " messageUser: " + messageUser
                    + " filter: " + filter);
        }

        ControlFragment cf = ControlFragment.newInstance(accountName, null, null, filter);
        SubredditListFragment slf = getSubredditListFragment();
        ThingListFragment tlf = ThingListFragment.newInstance(accountName, query, profileUser,
                messageUser, filter, tlfFlags);
        ThingMenuFragment tmf = getThingMenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        if (slf != null) {
            ft.remove(slf);
        }
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        if (tmf != null) {
            ft.remove(tmf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshSubredditListVisibility();
        refreshActionBar(null, null);
        refreshViews(null);
    }

    public void onInitialSubredditSelected(String subreddit, boolean error) {
        if (!isSinglePane) {
            selectInitialSubredditMultiPane(subreddit, error);
        }
    }

    protected void selectInitialSubredditMultiPane(String subreddit, boolean error) {
        ControlFragment cf = getControlFragment();
        if (cf != null && cf.getSubreddit() == null) {
            cf.setSubreddit(subreddit);

            SubredditListFragment slf = getSubredditListFragment();
            slf.setSelectedSubreddit(subreddit);

            ThingListFragment tlf = getThingListFragment();
            if (subreddit != null) {
                tlf.setSubreddit(subreddit);
                tlf.loadIfPossible();
            } else {
                tlf.setEmpty(error);
            }

            refreshActionBar(subreddit, null);
        }
    }

    public void onSubredditSelected(String subreddit) {
        if (isSinglePane) {
            selectSubredditSinglePane(subreddit, 0);
        } else {
            selectSubredditMultiPane(subreddit);
        }
    }

    protected void selectSubredditSinglePane(String subreddit, int flags) {
        Intent intent = new Intent(this, ThingListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(ThingListActivity.EXTRA_FLAGS, flags);
        startActivity(intent);
    }

    protected void selectSubredditMultiPane(String subreddit) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        ControlFragment cf = ControlFragment.newInstance(accountName, subreddit, null, filter);
        ThingListFragment tlf = ThingListFragment.newSubredditInstance(accountName, subreddit,
                filter, tlfFlags);
        ThingMenuFragment tmf = getThingMenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        if (tmf != null) {
            ft.remove(tmf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshActionBar(subreddit, null);
        refreshViews(null);
    }

    protected void replaceThingListFragmentMultiPane() {
        selectSubredditMultiPane(getControlFragment().getSubreddit());
    }

    public void onThingSelected(Bundle thingBundle, int position) {
        if (isSinglePane) {
            selectThingSinglePane(thingBundle, 0);
        } else {
            selectThingMultiPane(thingBundle, position);
        }
    }

    protected void selectThingSinglePane(Bundle thingBundle, int flags) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        intent.putExtra(ThingActivity.EXTRA_FLAGS, flags);
        startActivity(intent);
    }

    protected void selectThingMultiPane(Bundle thingBundle, int thingPosition) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        ControlFragment cf = getControlFragment();
        cf = ControlFragment.newInstance(accountName, cf.getSubreddit(), thingBundle, filter);
        ThingMenuFragment tf = ThingMenuFragment.newInstance(
                ThingBundle.getSubreddit(thingBundle));

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.add(tf, ThingMenuFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();

        refreshThingPager(thingBundle);
    }

    public void onLinkDiscovery(String thingId, String title, String url) {
        ControlFragment cf = getControlFragment();
        Bundle thingBundle = cf.getThingBundle();
        if (Objects.equals(thingId, ThingBundle.getThingId(thingBundle))
                && !ThingBundle.hasLinkUrl(thingBundle)) {
            ThingBundle.putLinkUrl(thingBundle, url);
            ThingBundle.putTitle(thingBundle, title);
            cf.setThingBundle(thingBundle);

            ThingPagerAdapter adapter = (ThingPagerAdapter) thingPager.getAdapter();
            adapter.addPage(0, ThingPagerAdapter.TYPE_LINK);
            thingPager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
        }
    }

    public void onLinkMenuItemClick() {
        thingPager.setCurrentItem(ThingPagerAdapter.PAGE_LINK);
    }

    public void onCommentMenuItemClick() {
        thingPager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
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
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            String subreddit = cf.getSubreddit();
            Bundle thingBundle = cf.getThingBundle();
            refreshActionBar(subreddit, thingBundle);
            refreshViews(thingBundle);
            refreshCheckedItems();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (!isSinglePane && savedInstanceState != null) {
            ControlFragment cf = getControlFragment();
            if (cf != null) {
                String subreddit = cf.getSubreddit();
                Bundle thingBundle = cf.getThingBundle();
                refreshThingPager(thingBundle);
                refreshActionBar(subreddit, thingBundle);
                refreshViews(thingBundle);
                refreshCheckedItems();
            }
        }
    }

    private void refreshViews(Bundle thingBundle) {
        boolean hasThing = thingBundle != null;
        if (navContainer != null) {
            int currVisibility = navContainer.getVisibility();
            int nextVisibility = !hasThing ? View.VISIBLE : View.GONE;
            if (currVisibility != nextVisibility) {
                if (nextVisibility == View.VISIBLE) {
                    runAnimation(ANIMATION_OPEN_NAV);
                } else {
                    runAnimation(ANIMATION_CLOSE_NAV);
                }
            }
        } else {
            if (hasSubredditList() && subredditListContainer != null) {
                int currVisibility = subredditListContainer.getVisibility();
                int nextVisibility = hasThing ? View.GONE : View.VISIBLE;
                if (currVisibility != nextVisibility) {
                    if (nextVisibility == View.VISIBLE) {
                        runAnimation(ANIMATION_OPEN_SUBREDDIT_LIST);
                    } else {
                        runAnimation(ANIMATION_CLOSE_SUBREDDIT_LIST);
                    }
                } else {
                    // There may be no change in visibility if we are just
                    // starting, but we should set the correct visibility, since
                    // some activities change modes.
                    thingPager.setVisibility(hasThing ? View.VISIBLE : View.GONE);
                }
            } else {
                thingPager.setVisibility(hasThing ? View.VISIBLE : View.GONE);
            }
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

    protected abstract void refreshActionBar(String subreddit, Bundle thingBundle);

    private void refreshCheckedItems() {
        ControlFragment cf = getControlFragment();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "refreshCheckedItems subreddit:" + cf.getSubreddit());
        }

        SubredditListFragment slf = getSubredditListFragment();
        if (slf != null) {
            slf.setSelectedSubreddit(cf.getSubreddit());
        }

        ThingListFragment tlf = getThingListFragment();
        if (tlf != null) {
            tlf.setSelectedThing(ThingBundle.getThingId(cf.getThingBundle()),
                    ThingBundle.getLinkId(cf.getThingBundle()));
        }
    }

    private void refreshThingPager(Bundle thingBundle) {
        if (thingBundle != null) {
            ThingPagerAdapter adapter = new ThingPagerAdapter(getFragmentManager(),
                    getAccountName(), thingBundle);
            thingPager.setAdapter(adapter);
        } else {
            thingPager.setAdapter(null);
        }
    }

    protected void refreshSubredditListVisibility() {
        boolean showSubreddits = hasSubredditList();
        if (subredditListContainer != null) {
            subredditListContainer.setVisibility(showSubreddits ? View.VISIBLE : View.GONE);
        }
        refreshThingBodyWidthMeasurement();
    }

    private void refreshThingBodyWidthMeasurement() {
        int newWidth = hasSubredditList() ? subredditListWidth : 0;
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int padding = r.getDimensionPixelSize(R.dimen.element_padding);
        if (navContainer != null) {
            thingBodyWidth = dm.widthPixels - newWidth - padding * 2;
        } else {
            thingBodyWidth = dm.widthPixels / 5 * 2 - padding * 3;
        }
    }

    public CharSequence getSubredditName() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            String subreddit = cf.getSubreddit();
            if (subreddit != null) {
                return subreddit;
            }
            return ThingBundle.getSubreddit(cf.getThingBundle());
        }
        return null;
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
            fm.popBackStack();
        } else if ((bar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            finish();
        }
    }

    protected ControlFragment getControlFragment() {
        return (ControlFragment) getFragmentManager()
                .findFragmentByTag(ControlFragment.TAG);
    }

    protected SubredditListFragment getSubredditListFragment() {
        return (SubredditListFragment) getFragmentManager()
                .findFragmentByTag(SubredditListFragment.TAG);
    }

    protected ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager()
                .findFragmentByTag(ThingListFragment.TAG);
    }

    protected ThingMenuFragment getThingMenuFragment() {
        return (ThingMenuFragment) getFragmentManager()
                .findFragmentByTag(ThingMenuFragment.TAG);
    }

    private void runAnimation(int type) {
        getAnimator(type).start();
    }

    private AnimatorSet getAnimator(int type) {
        switch (type) {
            case ANIMATION_OPEN_NAV:
                if (openNavAnimator == null) {
                    openNavAnimator = createOpenNavAnimator();
                }
                return openNavAnimator;

            case ANIMATION_CLOSE_NAV:
                if (closeNavAnimator == null) {
                    closeNavAnimator = createCloseNavAnimator();
                }
                return closeNavAnimator;

            case ANIMATION_OPEN_SUBREDDIT_LIST:
                if (openSubredditListAnimator == null) {
                    openSubredditListAnimator = createOpenSubredditListAnimator();
                }
                return openSubredditListAnimator;

            case ANIMATION_CLOSE_SUBREDDIT_LIST:
                if (closeSubredditListAnimator == null) {
                    closeSubredditListAnimator = createCloseSubredditListAnimator();
                }
                return closeSubredditListAnimator;

            default:
                throw new IllegalArgumentException();
        }
    }

    private AnimatorSet createOpenNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer,
                "translationX", -fullNavWidth, 0);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager,
                "translationX", 0, fullNavWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(duration).play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                navContainer.setVisibility(View.VISIBLE);
                thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingPager.setVisibility(View.VISIBLE);
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
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer,
                "translationX", 0, -subredditListWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(duration).play(ncTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                navContainer.setVisibility(View.VISIBLE);
                thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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

    private AnimatorSet createOpenSubredditListAnimator() {
        ObjectAnimator slTransX = ObjectAnimator.ofFloat(subredditListContainer,
                "translationX", -subredditListWidth, 0);
        ObjectAnimator tlTransX = ObjectAnimator.ofFloat(thingListContainer,
                "translationX", -subredditListWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(duration).play(slTransX).with(tlTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                subredditListContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                subredditListContainer.setVisibility(View.VISIBLE);
                thingListContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingListContainer.setVisibility(View.VISIBLE);
                thingPager.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                subredditListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        return as;
    }

    private AnimatorSet createCloseSubredditListAnimator() {
        ObjectAnimator slTransX = ObjectAnimator.ofFloat(subredditListContainer,
                "translationX", 0, -subredditListWidth);
        ObjectAnimator tlTransX = ObjectAnimator.ofFloat(thingListContainer,
                "translationX", 0, -subredditListWidth);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingPager,
                "translationX", subredditListWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(duration).play(slTransX).with(tlTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                subredditListContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                subredditListContainer.setVisibility(View.VISIBLE);
                thingListContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingListContainer.setVisibility(View.VISIBLE);
                thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                subredditListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                subredditListContainer.setVisibility(View.GONE);
                thingListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingListContainer.setTranslationX(0);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }
}
