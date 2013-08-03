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

    private static final String TAG_CONTROL_FRAGMENT = "control";
    private static final String TAG_LEFT_FRAGMENT = "left";
    private static final String TAG_RIGHT_FRAGMENT = "right";
    private static final String TAG_THING_FRAGMENT = "thing";

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_EXPAND_LEFT = 2;
    private static final int ANIMATION_COLLAPSE_LEFT = 3;
    private static final int ANIMATION_EXPAND_RIGHT = 4;
    private static final int ANIMATION_COLLAPSE_RIGHT = 5;

    protected ActionBar bar;
    protected boolean isSinglePane;
    private boolean isSingleChoice;

    private View navContainer;
    private View subredditListContainer;
    private View thingListContainer;
    private View thingContainer;
    private int subredditListWidth;
    private int thingBodyWidth;

    private int fullNavWidth;
    private int durationMs;

    private AnimatorSet openNavAnimator;
    private AnimatorSet closeNavAnimator;
    private AnimatorSet expandLeftAnimator;
    private AnimatorSet collapseLeftAnimator;
    private AnimatorSet expandRightAnimator;
    private AnimatorSet collapseRightAnimator;

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
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        thingContainer = findViewById(R.id.thing_container);
        isSinglePane = thingContainer == null;
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

            Resources r = getResources();
            subredditListWidth = r.getDimensionPixelSize(R.dimen.subreddit_list_width);
            fullNavWidth = r.getDisplayMetrics().widthPixels;
        }

        measureThingBodyWidth();
    }

    // TODO: Do we need this method or can it be rolled into setupPrereqs?
    protected abstract void setupViews();

    protected abstract void setupActionBar(Bundle savedInstanceState);

    protected abstract boolean hasLeftFragment();

    // Methods used to setup the initial fragments.

    protected void setBrowserFragments() {
        setLeftFragment(R.id.subreddit_list_container,
                NavigationFragment.newInstance());
    }

    protected void setSubredditFragments(String accountName, String subreddit,
            ThingBundle thingBundle, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(R.id.thing_list_container,
                ControlFragment.newSubredditInstance(accountName, subreddit, null, filter),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice));
        if (thingBundle != null) {
            selectThing(null, subreddit, thingBundle);
        }
    }

    protected void setSearchThingsFragments(String accountName,
            String subreddit, String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(R.id.thing_list_container,
                ControlFragment.newSearchThingsInstance(accountName, subreddit, query, filter),
                SearchThingListFragment
                        .newInstance(accountName, subreddit, query, isSingleChoice));
    }

    protected void setSearchSubredditsFragments(String accountName, String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        if (isSinglePane) {
            setCenterFragment(R.id.thing_list_container,
                    ControlFragment.newSearchSubredditsInstance(accountName, query, filter),
                    SearchSubredditListFragment
                            .newInstance(accountName, query, isSingleChoice));
        } else {
            setLeftFragment(R.id.subreddit_list_container,
                    SearchSubredditListFragment
                            .newInstance(accountName, query, isSingleChoice));
        }
    }

    protected void setUserProfileFragments(String accountName, String profileUser, int filter) {
        selectAccountWithFilter(accountName, 0);
        setCenterFragment(R.id.thing_list_container,
                ControlFragment.newUserProfileInstance(accountName, profileUser, filter),
                ProfileThingListFragment
                        .newInstance(accountName, profileUser, filter, isSingleChoice));
    }

    // Callbacks triggered by calling one of the initial methods that select fragments.

    @Override
    public void onSubredditSelected(String accountName, String subreddit, int filter) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newSubredditInstance(accountName, subreddit, null, filter),
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
        selectThing(view, getControlFragment().getSubreddit(), thingBundle);
    }

    @Override
    public int onThingBodyMeasure() {
        return thingBodyWidth;
    }

    // Method to set state in this activity.

    private void selectAccountWithFilter(String accountName, int filter) {
        this.accountName = accountName;
        this.filter = filter;
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
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
        setRightFragment(R.id.thing_list_container,
                ControlFragment.newSubredditInstance(accountName, subreddit, null, filter),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice),
                false);
    }

    // Methods to select a thing

    protected void selectThing(View view, String subreddit, ThingBundle thingBundle) {
        if (isSinglePane) {
            selectThingSinglePane(view, thingBundle);
        } else {
            selectThingMultiPane(subreddit, thingBundle);
        }
    }

    private void selectThingSinglePane(View view, ThingBundle thingBundle) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        launchActivity(view, intent);
    }

    private void selectThingMultiPane(String subreddit, ThingBundle thingBundle) {
        ControlFragment controlFrag =
                ControlFragment.newSubredditInstance(accountName, subreddit, thingBundle, filter);
        ThingFragment thingFrag =
                ThingFragment.newInstance(accountName, thingBundle);

        safePopBackStackImmediate();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(controlFrag, TAG_CONTROL_FRAGMENT);
        ft.replace(R.id.thing_container, thingFrag, TAG_THING_FRAGMENT);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
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
                    .newSubredditInstance(controlFrag.getAccountName(),
                            subreddit,
                            controlFrag.getThingBundle(),
                            controlFrag.getFilter());
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(newControlFrag, TAG_CONTROL_FRAGMENT);
            ft.commitAllowingStateLoss();
            refreshActionBar(newControlFrag);
        }
    }

    @Override
    public void onThingTitleDiscovery(String title) {
    }

    // Methods for setting the fragments on the screen.

    private <F extends Fragment & ComparableFragment>
            void setLeftFragment(int containerId, F frag) {
        if (!Objects.fragmentEquals(frag, getLeftComparableFragment())) {
            safePopBackStackImmediate();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            removeFragment(ft, TAG_CONTROL_FRAGMENT);
            ft.replace(containerId, frag, TAG_LEFT_FRAGMENT);
            removeFragment(ft, TAG_RIGHT_FRAGMENT);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();
        }
    }

    private <F extends Fragment & ComparableFragment>
            void setCenterFragment(int containerId, ControlFragment controlFrag, F centerFrag) {
        setRightFragment(containerId, controlFrag, centerFrag, true);
    }

    private <F extends Fragment & ComparableFragment>
            void setRightFragment(int containerId, ControlFragment controlFrag, F rightFrag) {
        setRightFragment(containerId, controlFrag, rightFrag, false);
    }

    private <F extends Fragment & ComparableFragment>
            void setRightFragment(int containerId, ControlFragment controlFrag,
                    F rightFrag, boolean removeLeft) {
        if (!Objects.fragmentEquals(rightFrag, getRightComparableFragment())) {
            safePopBackStackImmediate();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(controlFrag, TAG_CONTROL_FRAGMENT);
            if (removeLeft) {
                removeFragment(ft, TAG_LEFT_FRAGMENT);
            }
            ft.replace(containerId, rightFrag, TAG_RIGHT_FRAGMENT);
            removeFragment(ft, TAG_THING_FRAGMENT);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();

            refreshActionBar(controlFrag);
            refreshViews(controlFrag.getThingBundle());
        }
    }

    private void safePopBackStackImmediate() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }
    }

    private void removeFragment(FragmentTransaction ft, String tag) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f != null) {
            ft.remove(f);
        }
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
                refreshViews(cf.getThingBundle());
                refreshCheckedItems();
            }
        }
    }

    protected abstract void refreshActionBar(ControlFragment controlFrag);

    private void refreshViews(ThingBundle thingBundle) {
        measureThingBodyWidth();
        if (!isSinglePane) {
            updateVisibility(thingBundle);
        }
    }

    private void measureThingBodyWidth() {
        if (isSinglePane || drawerLayout != null && navContainer != null) {
            return;
        }
        Resources r = getResources();
        DisplayMetrics dm = r.getDisplayMetrics();
        int padding = r.getDimensionPixelSize(R.dimen.element_padding);
        if (navContainer != null) {
            int newWidth = hasLeftFragment() ? subredditListWidth : 0;
            thingBodyWidth = dm.widthPixels - newWidth - padding * 2;
        } else {
            thingBodyWidth = dm.widthPixels / 5 * 2 - padding * 3;
        }
    }

    private void updateVisibility(ThingBundle thingBundle) {
        boolean hasLeftFragment = hasLeftFragment();
        if (subredditListContainer != null) {
            subredditListContainer
                    .setVisibility(getVisibility(drawerLayout != null || hasLeftFragment));
        }

        boolean hasThing = thingBundle != null;
        if (navContainer != null) {
            int cnv = navContainer.getVisibility();
            int nnv = getVisibility(!hasThing);
            if (cnv != nnv) {
                if (isVisible(nnv)) {
                    startAnimation(ANIMATION_OPEN_NAV);
                } else {
                    startAnimation(ANIMATION_CLOSE_NAV);
                }
            }
        } else {
            int ctv = thingContainer.getVisibility();
            int nnv = getVisibility(hasThing);
            if (ctv != nnv) {
                if (isVisible(nnv)) {
                    startAnimation(hasLeftFragment
                            ? ANIMATION_COLLAPSE_LEFT
                            : ANIMATION_COLLAPSE_RIGHT);
                } else {
                    startAnimation(hasLeftFragment
                            ? ANIMATION_COLLAPSE_RIGHT
                            : ANIMATION_EXPAND_RIGHT);
                }
            }
        }
    }

    private int getVisibility(boolean visible) {
        return visible ? View.VISIBLE : View.GONE;
    }

    private boolean isVisible(int visibility) {
        return visibility == View.VISIBLE;
    }

    private void refreshCheckedItems() {
        if (!isSinglePane) {
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
                .findFragmentByTag(TAG_CONTROL_FRAGMENT);
    }

    private ComparableFragment getLeftComparableFragment() {
        return (ComparableFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_LEFT_FRAGMENT);
    }

    private ComparableFragment getRightComparableFragment() {
        return (ComparableFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_RIGHT_FRAGMENT);
    }

    protected NavigationFragment getNavigationFragment() {
        return (NavigationFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_LEFT_FRAGMENT);
    }

    protected ThingListFragment<?> getThingListFragment() {
        return (ThingListFragment<?>) getSupportFragmentManager()
                .findFragmentByTag(TAG_RIGHT_FRAGMENT);
    }

    private ThingFragment getThingFragment() {
        return (ThingFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_THING_FRAGMENT);
    }

    private void startAnimation(int type) {
        getAnimator(type).start();
    }

    private AnimatorSet getAnimator(int type) {
        switch (type) {
            case ANIMATION_OPEN_NAV:
                if (openNavAnimator == null) {
                    openNavAnimator = newOpenNavAnimator();
                }
                return openNavAnimator;

            case ANIMATION_CLOSE_NAV:
                if (closeNavAnimator == null) {
                    closeNavAnimator = newCloseNavAnimator();
                }
                return closeNavAnimator;

            case ANIMATION_EXPAND_LEFT:
                if (expandLeftAnimator == null) {
                    expandLeftAnimator = newExpandLeftAnimator();
                }
                return expandLeftAnimator;

            case ANIMATION_COLLAPSE_LEFT:
                if (collapseLeftAnimator == null) {
                    collapseLeftAnimator = newCollapseLeftAnimator();
                }
                return collapseLeftAnimator;

            case ANIMATION_EXPAND_RIGHT:
                if (expandRightAnimator == null) {
                    expandRightAnimator = newExpandRightAnimator();
                }
                return expandRightAnimator;

            case ANIMATION_COLLAPSE_RIGHT:
                if (collapseRightAnimator == null) {
                    collapseRightAnimator = newCollapseRightAnimator();
                }
                return collapseRightAnimator;

            default:
                throw new IllegalArgumentException();
        }
    }

    private AnimatorSet newOpenNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer,
                "translationX", -fullNavWidth, 0);
        ObjectAnimator tpTransX = ObjectAnimator.ofFloat(thingContainer,
                "translationX", 0, fullNavWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(ncTransX).with(tpTransX);
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

    private AnimatorSet newCloseNavAnimator() {
        ObjectAnimator ncTransX = ObjectAnimator.ofFloat(navContainer,
                "translationX", 0, -subredditListWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(ncTransX);
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

    private AnimatorSet newCollapseLeftAnimator() {
        ObjectAnimator slTransX = ObjectAnimator
                .ofFloat(subredditListContainer, "translationX", 0, -subredditListWidth);
        ObjectAnimator tlTransX = ObjectAnimator
                .ofFloat(thingListContainer, "translationX", 0, -subredditListWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(slTransX).with(tlTransX);
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
                subredditListContainer.setVisibility(View.GONE);
                thingListContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingListContainer.setTranslationX(0);
                thingContainer.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }

    private AnimatorSet newExpandLeftAnimator() {
        ObjectAnimator slTransX = ObjectAnimator
                .ofFloat(subredditListContainer, "translationX", -subredditListWidth, 0);
        ObjectAnimator tlTransX = ObjectAnimator
                .ofFloat(thingListContainer, "translationX", -subredditListWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(slTransX).with(tlTransX);
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

    private AnimatorSet newCollapseRightAnimator() {
        ObjectAnimator tcTransX = ObjectAnimator
                .ofFloat(thingContainer, "translationX", subredditListWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(tcTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                thingContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });
        return as;
    }

    private AnimatorSet newExpandRightAnimator() {
        ObjectAnimator tcTransX = ObjectAnimator
                .ofFloat(thingContainer, "translationX", 0, subredditListWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(tcTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                thingContainer.setVisibility(View.GONE);
            }
        });
        return as;
    }
}
