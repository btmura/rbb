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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.NavigationFragment.OnNavigationEventListener;
import com.btmura.android.reddit.app.SearchSubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.app.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingView;

abstract class AbstractBrowserActivity extends GlobalMenuActivity implements
        OnNavigationEventListener,
        OnSubredditEventListener,
        OnSubredditSelectedListener,
        OnThingSelectedListener,
        OnThingEventListener,
        OnBackStackChangedListener,
        AccountNameHolder,
        SubredditNameHolder,
        ThingBundleHolder {

    public static final String TAG = "AbstractBrowserActivity";

    private static final String CONTROL_FRAGMENT_TAG = "control";
    private static final String LEFT_FRAGMENT_TAG = "left";
    private static final String RIGHT_FRAGMENT_TAG = "right";
    private static final String THING_FRAGMENT_TAG = "thing";

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SUBREDDIT_LIST = 2;
    private static final int ANIMATION_CLOSE_SUBREDDIT_LIST = 3;

    protected ActionBar bar;

    protected boolean isSinglePane;
    protected boolean isSingleChoice;

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

    private String accountName;
    private int filter;
    protected DrawerLayout drawerLayout;

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
        }
    }

    protected abstract void setContentView();

    private void setupPrereqs() {
        bar = getActionBar();
        isSinglePane = findViewById(R.id.thing_container) == null;
        isSingleChoice = !isSinglePane;
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

    protected abstract boolean hasLeftFragment();

    // Methods that set the left fragments

    protected void setBrowserFragments(int containerId) {
        setLeftFragment(containerId, NavigationFragment.newInstance());
    }

    protected void setSearchSubredditsFragments(int containerId, String accountName,
            String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        setLeftFragment(containerId, SearchSubredditListFragment
                .newInstance(accountName, query, isSingleChoice));
    }

    // Callbacks that set the right fragments in response to the left fragments
    // Methods for setting the content of the right hand thing list pane.

    @Override
    public void onSubredditSelected(String accountName, String subreddit, int filter) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newSubredditInstance(accountName, subreddit, filter),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice));
    }

    @Override
    public void onProfileSelected(String accountName, int filter) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newProfileInstance(accountName, filter),
                ProfileThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice));
    }

    @Override
    public void onSavedSelected(String accountName, int filter) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newSavedInstance(accountName, filter),
                ProfileThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice));
    }

    @Override
    public void onMessagesSelected(String accountName, int filter) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newMessagesInstance(accountName, filter),
                MessageThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice));
    }

    @Override
    public void onSubredditSelected(View view, String subreddit, boolean onLoad) {
        if (!onLoad || !isSinglePane) {
            selectSubreddit(view, subreddit);
        }
    }

    @Override
    public void onThingSelected(View view, ThingBundle thingBundle, int pageType) {
        selectThing(view, thingBundle);
    }

    // Methods that set the center fragments

    protected void setSubredditFragments(int containerId, String accountName, String subreddit,
            ThingBundle thingBundle, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(containerId,
                ControlFragment.newSubredditInstance(accountName, subreddit, filter),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice));
    }

    protected void setSearchThingsFragments(int containerId, String accountName,
            String subreddit, String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(containerId,
                ControlFragment.newSearchThingsInstance(accountName, subreddit, query, filter),
                SearchThingListFragment
                        .newInstance(accountName, subreddit, query, isSingleChoice));
    }

    protected void setUserProfileFragments(int containerId, String accountName,
            String profileUser, int filter) {
        selectAccountWithFilter(accountName, 0);
        setCenterFragment(containerId,
                ControlFragment.newUserProfileInstance(accountName, profileUser, filter),
                ProfileThingListFragment
                        .newInstance(accountName, profileUser, filter, isSingleChoice));
    }

    // Methods for setting the fragments on the screen.

    private void selectAccountWithFilter(String accountName, int filter) {
        this.accountName = accountName;
        this.filter = filter;
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    private <F extends Fragment & ComparableFragment>
            void setLeftFragment(int containerId, F frag) {
        if (!Objects.fragmentEquals(frag, getLeftComparableFragment())) {
            safePopBackStackImmediate();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            removeFragment(ft, CONTROL_FRAGMENT_TAG);
            ft.replace(containerId, frag, LEFT_FRAGMENT_TAG);
            removeFragment(ft, RIGHT_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();
        }
    }

    private void setCenterFragment(int containerId,
            ControlFragment controlFrag, ThingListFragment<?> rightFrag) {
        if (isSinglePane) {
            setRightFragmentSinglePane(containerId, controlFrag, rightFrag, true);
        } else {
            setRightFragmentMultiPane(containerId, controlFrag, rightFrag, true);
        }
    }

    private <F extends Fragment & ComparableFragment>
            void setRightFragment(int containerId, ControlFragment controlFrag, F frag) {
        if (isSinglePane) {
            setRightFragmentSinglePane(containerId, controlFrag, frag, false);
        } else {
            setRightFragmentMultiPane(containerId, controlFrag, frag, false);
        }
    }

    private <F extends Fragment & ComparableFragment>
            void setRightFragmentSinglePane(int containerId, ControlFragment controlFrag, F frag,
                    boolean removeLeft) {
        if (!Objects.fragmentEquals(frag, getRightComparableFragment())) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(controlFrag, CONTROL_FRAGMENT_TAG);
            if (removeLeft) {
                removeFragment(ft, LEFT_FRAGMENT_TAG);
            }
            ft.replace(containerId, frag, RIGHT_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();
            refreshActionBar(controlFrag);
        }
    }

    private <F extends Fragment & ComparableFragment>
            void setRightFragmentMultiPane(int containerId, ControlFragment controlFrag, F frag,
                    boolean removeLeft) {
        if (!Objects.fragmentEquals(frag, getRightComparableFragment())) {
            safePopBackStackImmediate();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(controlFrag, CONTROL_FRAGMENT_TAG);
            if (removeLeft) {
                removeFragment(ft, LEFT_FRAGMENT_TAG);
            }
            ft.replace(containerId, frag, RIGHT_FRAGMENT_TAG);
            removeFragment(ft, THING_FRAGMENT_TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();

            refreshActionBar(controlFrag);
            refreshThingBodyWidthMeasurement();
            refreshViews(null);
        }
    }

    private void removeFragment(FragmentTransaction ft, String tag) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f != null) {
            ft.remove(f);
        }
    }

    // Methods to select a subreddit

    private void selectSubreddit(View view, String subreddit) {
        if (isSinglePane) {
            selectSubredditSinglePane(view, subreddit);
        } else {
            selectSubredditMultiPane(subreddit);
        }
    }

    private void selectSubredditSinglePane(View view, String subreddit) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_SUBREDDIT, subreddit);
        launchActivity(view, intent);
    }

    private void selectSubredditMultiPane(String subreddit) {
        setRightFragmentMultiPane(R.id.thing_list_container,
                ControlFragment.newSubredditInstance(accountName, subreddit, filter),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice),
                false);
    }

    // Methods to select a thing

    protected void launchThingActivity(ThingBundle thingBundle) {
        selectThingSinglePane(null, thingBundle);
    }

    private void selectThing(View view, ThingBundle thingBundle) {
        if (isSinglePane) {
            selectThingSinglePane(view, thingBundle);
        } else {
            selectThingMultiPane(thingBundle);
        }
    }

    private void selectThingSinglePane(View view, ThingBundle thingBundle) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        launchActivity(view, intent);
    }

    private void selectThingMultiPane(ThingBundle thingBundle) {
        safePopBackStackImmediate();

        String subreddit = getControlFragment().getSubreddit();
        Fragment cf = ControlFragment.newInstance(accountName, subreddit,
                Subreddits.isRandom(subreddit), thingBundle, filter);
        Fragment tf = ThingFragment.newInstance(accountName, thingBundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(cf, CONTROL_FRAGMENT_TAG);
        ft.replace(R.id.thing_container, tf, THING_FRAGMENT_TAG);
        ft.addToBackStack(null);
        ft.commit();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void launchActivity(View view, Intent intent) {
        Bundle options = ThingView.makeActivityOptions(view);
        if (options != null) {
            startActivity(intent, options);
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onSubredditDiscovery(String subreddit) {
        ControlFragment controlFrag = getControlFragment();
        if (Subreddits.isRandom(controlFrag.getSubreddit())) {
            ControlFragment newControlFrag = ControlFragment
                    .newSubredditInstance(controlFrag.getAccountName(), subreddit,
                            controlFrag.getFilter());
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(newControlFrag, CONTROL_FRAGMENT_TAG);
            ft.commitAllowingStateLoss();
            refreshActionBar(newControlFrag);
        }
    }

    @Override
    public void onThingTitleDiscovery(String title) {
    }

    private void safePopBackStackImmediate() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }
    }

    @Override
    public int onMeasureThingBody() {
        return thingBodyWidth;
    }

    @Override
    public void onBackStackChanged() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            refreshActionBar(cf);
            refreshViews(cf.getThingBundle());
            refreshCheckedItems();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            ControlFragment cf = getControlFragment();
            if (cf != null) {
                refreshActionBar(cf);
                if (!isSinglePane) {
                    refreshViews(cf.getThingBundle());
                    refreshCheckedItems();
                }
            }
        }
    }

    private void refreshViews(ThingBundle thingBundle) {
        boolean hasThing = thingBundle != null;
        int nextThingVisibility = hasThing ?
                View.VISIBLE : View.GONE;
        int nextSubredditListVisiblility = hasLeftFragment() && !hasThing ?
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
            if (hasLeftFragment() && subredditListContainer != null) {
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
        }
    }

    protected abstract void refreshActionBar(ControlFragment controlFrag);

    private void refreshCheckedItems() {
        ThingListFragment<?> tf = getThingListFragment();
        if (tf != null) {
            ControlFragment cf = getControlFragment();
            ThingBundle thingBundle = cf.getThingBundle();
            if (thingBundle != null) {
                tf.setSelectedThing(thingBundle.getThingId(), thingBundle.getLinkId());
            } else {
                tf.setSelectedThing(null, null);
            }
        }
    }

    protected void refreshSubredditListVisibility() {
        // Only multi pane activities have a distinct subreddit list.
        if (!isSinglePane) {
            boolean showSubreddits = hasLeftFragment();
            if (subredditListContainer != null) {
                subredditListContainer.setVisibility(showSubreddits ? View.VISIBLE : View.GONE);
            }
            refreshThingBodyWidthMeasurement();
        }
    }

    private void refreshThingBodyWidthMeasurement() {
        int newWidth = hasLeftFragment() ? subredditListWidth : 0;
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int padding = r.getDimensionPixelSize(R.dimen.element_padding);
        if (navContainer != null) {
            thingBodyWidth = dm.widthPixels - newWidth - padding * 2;
        } else {
            thingBodyWidth = dm.widthPixels / 5 * 2 - padding * 3;
        }
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getSubredditName() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            String subreddit = cf.getSubreddit();
            if (subreddit != null) {
                return subreddit;
            }
            return cf.getThingBundle().getSubreddit();
        }
        return null;
    }

    @Override
    public ThingBundle getThingBundle() {
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
                handleHome(item);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleHome(MenuItem item) {
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

    protected ControlFragment getControlFragment() {
        return (ControlFragment) getSupportFragmentManager()
                .findFragmentByTag(CONTROL_FRAGMENT_TAG);
    }

    private ComparableFragment getLeftComparableFragment() {
        return (ComparableFragment) getSupportFragmentManager()
                .findFragmentByTag(LEFT_FRAGMENT_TAG);
    }

    private ComparableFragment getRightComparableFragment() {
        return (ComparableFragment) getSupportFragmentManager()
                .findFragmentByTag(RIGHT_FRAGMENT_TAG);
    }

    protected NavigationFragment getNavigationFragment() {
        return (NavigationFragment) getSupportFragmentManager()
                .findFragmentByTag(LEFT_FRAGMENT_TAG);
    }

    protected ThingListFragment<?> getThingListFragment() {
        return (ThingListFragment<?>) getSupportFragmentManager()
                .findFragmentByTag(RIGHT_FRAGMENT_TAG);
    }

    private ThingFragment getThingFragment() {
        return (ThingFragment) getSupportFragmentManager()
                .findFragmentByTag(THING_FRAGMENT_TAG);
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
