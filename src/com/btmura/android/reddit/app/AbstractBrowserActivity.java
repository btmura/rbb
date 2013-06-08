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
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.app.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingBundle;
import com.btmura.android.reddit.widget.ThingView;

abstract class AbstractBrowserActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnSubredditSelectedListener,
        OnSubredditEventListener,
        OnThingSelectedListener,
        OnThingEventListener,
        OnBackStackChangedListener,
        AccountNameHolder,
        SubredditNameHolder,
        ThingBundleHolder {

    public static final String TAG = "AbstractBrowserActivity";

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SUBREDDIT_LIST = 2;
    private static final int ANIMATION_CLOSE_SUBREDDIT_LIST = 3;

    protected ActionBar bar;

    protected boolean isSinglePane;
    private int sfFlags;
    private int tfFlags;

    private View navContainer;
    private View subredditListContainer;
    private View thingListContainer;
    private View thingContainer;
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
            getSupportLoaderManager().initLoader(0, null, this);
        }
    }

    protected abstract void setContentView();

    private void setupPrereqs() {
        bar = getActionBar();
        isSinglePane = findViewById(R.id.thing_container) == null;
    }

    protected abstract boolean skipSetup();

    private void setupCommonFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setupCommonViews() {
        if (!isSinglePane) {
            getSupportFragmentManager().addOnBackStackChangedListener(this);

            sfFlags |= SubredditListFragment.FLAG_SINGLE_CHOICE;
            tfFlags |= ThingListFragment.FLAG_SINGLE_CHOICE;

            navContainer = findViewById(R.id.nav_container);
            subredditListContainer = findViewById(R.id.subreddit_list_container);
            thingListContainer = findViewById(R.id.thing_list_container);
            thingContainer = findViewById(R.id.thing_container);

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

    public abstract Loader<AccountResult> onCreateLoader(int id, Bundle args);

    public abstract void onLoadFinished(Loader<AccountResult> loader, AccountResult result);

    public abstract void onLoaderReset(Loader<AccountResult> loader);

    public abstract String getAccountName();

    protected abstract int getFilter();

    protected abstract boolean hasSubredditList();

    protected void setSearchSubredditListNavigation(int containerId, String query) {
        setSubredditListNavigation(containerId, null, false, query, null);
    }

    protected void setSubredditListNavigation(int containerId, String subreddit, boolean isRandom,
            String query, Bundle thingBundle) {
        if (isSinglePane) {
            setSubredditListNavigationSinglePane(containerId, query);
        } else {
            setSubredditListNavigationMultiPane(containerId, subreddit, isRandom,
                    query, thingBundle);
        }
    }

    private void setSubredditListNavigationSinglePane(int containerId, String query) {
        Fragment sf = SubredditListFragment.newInstance(getAccountName(), null, query, sfFlags);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(containerId, sf, SubredditListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setSubredditListNavigationMultiPane(int containerId, String subreddit,
            boolean isRandom, String query, Bundle thingBundle) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setSubredditListNavigation subreddit:" + subreddit
                    + " isRandom: " + isRandom
                    + " query: " + query
                    + " thingBundle: " + thingBundle);
        }
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        Fragment cf = ControlFragment.newInstance(accountName, subreddit, isRandom, thingBundle,
                filter);
        Fragment slf = SubredditListFragment.newInstance(accountName, subreddit, query, sfFlags);
        Fragment tlf = ThingListFragment.newSubredditInstance(accountName, subreddit, filter,
                tfFlags);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(containerId, slf, SubredditListFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);

        // If a thing was specified by the thingBundle argument, then add the
        // ThingMenuFragment. Otherwise, make sure to remove the prior
        // ThingMenuFragment for some other thing.
        if (thingBundle != null) {
            Fragment tf = ThingFragment.newInstance(accountName, thingBundle);
            ft.replace(R.id.thing_container, tf, ThingFragment.TAG);
        } else {
            Fragment tf = getThingFragment();
            if (tf != null) {
                ft.remove(tf);
            }
        }

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);

        // Use commitAllowingStateLoss to allow changing accounts when the
        // account list activity is a dialog on large devices and we remove an
        // account causing new fragment transactions to occur.
        ft.commitAllowingStateLoss();

        refreshActionBar(subreddit, thingBundle);
        refreshThingBodyWidthMeasurement();
        refreshViews(thingBundle);
        refreshThingPager(thingBundle, -1);
    }

    protected void setSearchThingListNavigation(int containerId, String subreddit, String query) {
        ThingListFragment frag = ThingListFragment.newQueryInstance(getAccountName(),
                subreddit, query, tfFlags);
        setThingListNavigation(frag, containerId, subreddit);
    }

    protected void setProfileThingListNavigation(int containerId, String profileUser) {
        ThingListFragment frag = ThingListFragment.newProfileInstance(getAccountName(),
                profileUser, getFilter(), tfFlags);
        setThingListNavigation(frag, containerId, null);
    }

    protected void setMessageThingListNavigation(int containerId, String messageUser) {
        ThingListFragment frag = ThingListFragment.newMessageInstance(getAccountName(),
                messageUser, getFilter(), tfFlags);
        setThingListNavigation(frag, containerId, null);
    }

    private void setThingListNavigation(ThingListFragment frag, int containerId,
            String subreddit) {
        if (isSinglePane) {
            setThingListNavigationSinglePane(frag, containerId);
        } else {
            setThingListNavigationMultiPane(frag, containerId, subreddit);
        }
    }

    private void setThingListNavigationSinglePane(ThingListFragment frag, int containerId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationSinglePane");
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(containerId, frag, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setThingListNavigationMultiPane(ThingListFragment frag, int containerId,
            String subreddit) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationMultiPane");
        }

        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();
        Fragment cf = ControlFragment.newInstance(accountName, subreddit, false, null, filter);
        Fragment sf = getSubredditListFragment();
        Fragment mf = getThingFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        if (sf != null) {
            ft.remove(sf);
        }
        ft.replace(containerId, frag, ThingListFragment.TAG);
        if (mf != null) {
            ft.remove(mf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshActionBar(subreddit, null);
        refreshThingBodyWidthMeasurement();
        refreshViews(null);
    }

    public void onInitialSubredditSelected(String subreddit, boolean error) {
        if (!isSinglePane) {
            selectInitialSubredditMultiPane(subreddit, error);
        }
    }

    private void selectInitialSubredditMultiPane(String subreddit, boolean error) {
        ControlFragment cf = getControlFragment();
        if (cf != null && cf.getSubreddit() == null) {
            cf.setSubreddit(subreddit);

            SubredditListFragment sf = getSubredditListFragment();
            sf.setSelectedSubreddit(subreddit);

            ThingListFragment tf = getThingListFragment();
            if (subreddit != null) {
                tf.setSubreddit(subreddit);
                tf.loadIfPossible();
            } else {
                tf.setEmpty(error);
            }

            refreshActionBar(subreddit, null);
        }
    }

    public void onSubredditSelected(View view, String subreddit) {
        selectSubreddit(view, subreddit, Subreddits.isRandom(subreddit), 0);
    }

    protected void selectSubreddit(View view, String subreddit, boolean isRandom, int flags) {
        if (isSinglePane) {
            selectSubredditSinglePane(view, subreddit, flags);
        } else {
            selectSubredditMultiPane(subreddit, isRandom);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void selectSubredditSinglePane(View view, String subreddit, int flags) {
        Intent intent = new Intent(this, ThingListActivity.class);
        intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(ThingListActivity.EXTRA_FLAGS, flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && view != null) {
            startActivity(intent, ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                    view.getWidth(), view.getHeight()).toBundle());
        } else {
            startActivity(intent);
        }
    }

    private void selectSubredditMultiPane(String subreddit, boolean isRandom) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        Fragment cf = ControlFragment.newInstance(accountName, subreddit, isRandom, null, filter);
        Fragment tlf = ThingListFragment.newSubredditInstance(accountName, subreddit, filter,
                tfFlags);
        Fragment tf = getThingFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        if (tf != null) {
            ft.remove(tf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshActionBar(subreddit, null);
        refreshViews(null);
    }

    protected void replaceThingListFragmentMultiPane() {
        ControlFragment cf = getControlFragment();
        selectSubredditMultiPane(cf.getSubreddit(), cf.isRandom());
    }

    public void onThingSelected(View view, Bundle thingBundle, int pageType) {
        selectThing(view, thingBundle, 0, pageType);
    }

    protected void selectThing(View view, Bundle thingBundle, int flags, int pageType) {
        if (isSinglePane) {
            selectThingSinglePane(view, thingBundle, pageType, 0);
        } else {
            selectThingMultiPane(thingBundle, pageType);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void selectThingSinglePane(View view, Bundle thingBundle, int pageType, int flags) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        intent.putExtra(ThingActivity.EXTRA_PAGE_TYPE, pageType);
        Bundle options = ThingView.makeActivityOptions(view);
        if (options != null) {
            startActivity(intent, options);
        } else {
            startActivity(intent);
        }
    }

    private void selectThingMultiPane(Bundle thingBundle, int pageType) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        String subreddit = getControlFragment().getSubreddit();
        Fragment cf = ControlFragment.newInstance(accountName, subreddit,
                Subreddits.isRandom(subreddit), thingBundle, filter);
        Fragment tf = ThingFragment.newInstance(accountName, thingBundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_container, tf, ThingFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();

        refreshThingPager(thingBundle, pageType);
    }

    public void onSubredditDiscovery(String subreddit) {
        ControlFragment cf = getControlFragment();
        if (Subreddits.isRandom(cf.getSubreddit())) {
            cf.setSubreddit(subreddit);
            cf.setIsRandom(true);
            refreshActionBar(subreddit, cf.getThingBundle());
        }
    }

    public void onThingLoaded(ThingHolder thingHolder) {
        ControlFragment cf = getControlFragment();
        Bundle thingBundle = cf.getThingBundle();
        if (Objects.equals(thingHolder.getThingId(), ThingBundle.getThingId(thingBundle))) {
            if (!ThingBundle.hasTitle(thingBundle)) {
                ThingBundle.putTitle(thingBundle, thingHolder.getThingId());
                cf.setThingBundle(thingBundle);
            }

            if (!thingHolder.isSelf() && !ThingBundle.hasLinkUrl(thingBundle)) {
                ThingBundle.putLinkUrl(thingBundle, thingHolder.getUrl());
                cf.setThingBundle(thingBundle);
            }
        }
    }

    public ViewPager getThingPager() {
        return null;
    }

    private void safePopBackStackImmediate() {
        FragmentManager fm = getSupportFragmentManager();
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
                refreshThingPager(thingBundle, -1);
                refreshActionBar(subreddit, thingBundle);
                refreshViews(thingBundle);
                refreshCheckedItems();
            }
        }
    }

    private void refreshViews(Bundle thingBundle) {
        boolean hasThing = thingBundle != null;
        int nextThingVisibility = hasThing ?
                View.VISIBLE : View.GONE;
        int nextSubredditListVisiblility = hasSubredditList() && !hasThing ?
                View.VISIBLE : View.GONE;

        if (navContainer != null) {
            int currVisibility = navContainer.getVisibility();
            int nextVisibility = !hasThing ? View.VISIBLE : View.GONE;
            if (currVisibility != nextVisibility) {
                if (nextVisibility == View.VISIBLE) {
                    startAnimation(ANIMATION_OPEN_NAV);
                } else {
                    startAnimation(ANIMATION_CLOSE_NAV);
                }
            }

            if (subredditListContainer != null) {
                subredditListContainer.setVisibility(nextSubredditListVisiblility);
            }
        } else {
            if (hasSubredditList() && subredditListContainer != null) {
                int currVisibility = subredditListContainer.getVisibility();
                if (currVisibility != nextSubredditListVisiblility) {
                    if (nextSubredditListVisiblility == View.VISIBLE) {
                        startAnimation(ANIMATION_OPEN_SUBREDDIT_LIST);
                    } else {
                        startAnimation(ANIMATION_CLOSE_SUBREDDIT_LIST);
                    }
                } else {
                    // There may be no change in visibility if we are just
                    // starting, but we should set the correct visibility, since
                    // some activities change modes.
                    thingContainer.setVisibility(nextThingVisibility);
                }
            } else {
                if (subredditListContainer != null) {
                    subredditListContainer.setVisibility(nextSubredditListVisiblility);
                }
                thingContainer.setVisibility(nextThingVisibility);
            }
            if (!hasThing) {
                // Avoid nested executePendingTransactions that would occur by
                // doing popBackStack. This is a hack to get around stale
                // adapter issues with the ViewPager after orientation changes.
                thingContainer.post(new Runnable() {
                    public void run() {
                        refreshThingPager(null, -1);
                    }
                });
            }
        }
    }

    protected abstract void refreshActionBar(String subreddit, Bundle thingBundle);

    private void refreshCheckedItems() {
        ControlFragment cf = getControlFragment();

        SubredditListFragment sf = getSubredditListFragment();
        if (sf != null) {
            sf.setSelectedSubreddit(cf.isRandom() ? Subreddits.NAME_RANDOM : cf.getSubreddit());
        }

        ThingListFragment tf = getThingListFragment();
        if (tf != null) {
            tf.setSelectedThing(ThingBundle.getThingId(cf.getThingBundle()),
                    ThingBundle.getLinkId(cf.getThingBundle()));
        }
    }

    private void refreshThingPager(Bundle thingBundle, int pageType) {
    }

    protected void refreshSubredditListVisibility() {
        // Only multi pane activities have a distinct subreddit list.
        if (!isSinglePane) {
            boolean showSubreddits = hasSubredditList();
            if (subredditListContainer != null) {
                subredditListContainer.setVisibility(showSubreddits ? View.VISIBLE : View.GONE);
            }
            refreshThingBodyWidthMeasurement();
        }
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

    public String getSubredditName() {
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

    public Bundle getThingBundle() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            return cf.getThingBundle();
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
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else if ((bar.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            finish();
        }
    }

    protected boolean hasThing() {
        return getThingFragment() != null;
    }

    private ControlFragment getControlFragment() {
        return (ControlFragment) getSupportFragmentManager()
                .findFragmentByTag(ControlFragment.TAG);
    }

    protected SubredditListFragment getSubredditListFragment() {
        return (SubredditListFragment) getSupportFragmentManager()
                .findFragmentByTag(SubredditListFragment.TAG);
    }

    protected ThingListFragment getThingListFragment() {
        return (ThingListFragment) getSupportFragmentManager()
                .findFragmentByTag(ThingListFragment.TAG);
    }

    private ThingFragment getThingFragment() {
        return (ThingFragment) getSupportFragmentManager()
                .findFragmentByTag(ThingFragment.TAG);
    }

    private void startAnimation(int type) {
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
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingContainer,
                "translationX", 0, fullNavWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(duration).play(ncTransX).with(tpTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                navContainer.setVisibility(View.VISIBLE);
                thingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingContainer.setVisibility(View.GONE);
                refreshThingPager(null, -1);
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
                thingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingContainer.setVisibility(View.GONE);
                thingContainer.setTranslationX(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                navContainer.setVisibility(View.GONE);
                thingContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingContainer.setVisibility(View.VISIBLE);
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
                thingContainer.setVisibility(View.GONE);
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
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingContainer,
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
                thingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                subredditListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                subredditListContainer.setVisibility(View.GONE);
                thingListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingListContainer.setTranslationX(0);
                thingContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingContainer.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }
}
