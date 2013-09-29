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
import android.app.Activity;
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
import com.btmura.android.reddit.app.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingView;

abstract class AbstractBrowserActivity extends GlobalMenuActivity implements
        OnNavigationEventListener,
        OnSubredditSelectedListener,
        OnThingSelectedListener,
        OnThingEventListener,
        OnBackStackChangedListener,
        AccountNameHolder,
        SubredditHolder,
        ThingHolder {

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

    interface LeftFragment extends ComparableFragment {
    }

    interface RightFragment extends ComparableFragment {
        void setSelectedThing(String thingId, String linkId);
    }

    private final Class<? extends Activity> thingActivityClass;

    protected ActionBar bar;
    protected boolean isSinglePane;
    private boolean isSingleChoice;

    private View navContainer;
    private View leftContainer;
    private View rightContainer;
    private View thingContainer;
    private int leftWidth;
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

    AbstractBrowserActivity(Class<? extends Activity> thingActivityClass) {
        this.thingActivityClass = thingActivityClass;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView();
        setupPrereqs();
        if (!skipSetup(savedInstanceState)) {
            setupCommonFragments(savedInstanceState);
            setupCommonViews();
            doSetup(savedInstanceState);
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

    protected abstract boolean skipSetup(Bundle savedInstanceState);

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
            leftContainer = findViewById(R.id.left_container);
            rightContainer = findViewById(R.id.right_container);

            Resources r = getResources();
            leftWidth = r.getDimensionPixelSize(R.dimen.left_width);
            fullNavWidth = r.getDisplayMetrics().widthPixels;
        }

        measureThingBodyWidth();
    }

    protected abstract void doSetup(Bundle savedInstanceState);

    protected abstract boolean hasLeftFragment();

    // Methods used to setup the initial fragments.

    protected void setBrowserFragments(String requestedSubreddit,
            ThingBundle requestedThingBundle) {
        int containerId = drawerLayout != null ? R.id.drawer_container : R.id.left_container;
        setLeftFragment(containerId,
                ControlFragment.newDrawerInstance(),
                NavigationFragment.newInstance(requestedSubreddit, requestedThingBundle));
    }

    protected void setSearchThingsFragments(String accountName,
            String subreddit, String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(R.id.right_container,
                ControlFragment.newSearchThingsInstance(accountName, subreddit, query, filter),
                SearchThingListFragment
                        .newInstance(accountName, subreddit, query, isSingleChoice),
                false);
    }

    protected void setUserProfileFragments(String accountName, String profileUser, int filter) {
        selectAccountWithFilter(accountName, filter);
        setCenterFragment(R.id.right_container,
                ControlFragment.newUserProfileInstance(accountName, profileUser, filter),
                ProfileThingListFragment
                        .newInstance(accountName, profileUser, filter, isSingleChoice),
                false);
    }

    protected void setSidebarFragments(String accountName, String subreddit) {
        selectAccountWithFilter(accountName, 0);
        setCenterFragment(R.id.right_container,
                ControlFragment.newSidebarInstance(accountName, subreddit),
                SidebarFragment.newInstance(subreddit),
                false);
    }

    protected void setSearchSubredditsFragments(String accountName, String query, int filter) {
        selectAccountWithFilter(accountName, filter);
        if (isSinglePane) {
            setCenterFragment(R.id.right_container,
                    ControlFragment.newSearchSubredditsInstance(accountName, query, filter),
                    SearchSubredditListFragment
                            .newInstance(accountName, query, isSingleChoice),
                    false);
        } else {
            setLeftFragment(R.id.left_container,
                    ControlFragment.newSearchSubredditsInstance(accountName, query, filter),
                    SearchSubredditListFragment
                            .newInstance(accountName, query, isSingleChoice));
        }
    }

    protected void setRelatedSubredditsFragments(String accountName, String subreddit) {
        selectAccountWithFilter(accountName, 0);
        if (isSinglePane) {
            setCenterFragment(R.id.right_container,
                    ControlFragment.newRelatedSubredditsInstance(accountName, subreddit),
                    RelatedSubredditListFragment.newInstance(subreddit, isSingleChoice),
                    false);
        } else {
            setLeftFragment(R.id.left_container,
                    ControlFragment.newRelatedSubredditsInstance(accountName, subreddit),
                    RelatedSubredditListFragment.newInstance(subreddit, isSingleChoice));
        }
    }

    // Callbacks triggered by calling one of the initial methods that select fragments.

    @Override
    public void onNavigationSubredditSelected(String accountName,
            String subreddit,
            boolean isRandom,
            int filter,
            ThingBundle thingBundle,
            boolean force) {
        selectAccountWithFilter(accountName, filter);
        ControlFragment controlFrag =
                ControlFragment.newSubredditInstance(accountName, subreddit, isRandom, filter);
        setRightFragment(R.id.right_container,
                controlFrag,
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice),
                force);
        if (thingBundle != null) {
            setThingFragment(controlFrag.withThingBundle(thingBundle));
        }
    }

    @Override
    public void onNavigationProfileSelected(String accountName, int filter, boolean force) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.right_container,
                ControlFragment.newProfileInstance(accountName, filter),
                ProfileThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice),
                force);
    }

    @Override
    public void onNavigationSavedSelected(String accountName, int filter, boolean force) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.right_container,
                ControlFragment.newSavedInstance(accountName, filter),
                ProfileThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice),
                force);
    }

    @Override
    public void onNavigationMessagesSelected(String accountName, int filter, boolean force) {
        selectAccountWithFilter(accountName, filter);
        setRightFragment(R.id.right_container,
                ControlFragment.newMessagesInstance(accountName, filter),
                MessageThingListFragment
                        .newInstance(accountName, accountName, filter, isSingleChoice),
                force);
    }

    @Override
    public void onSubredditSelected(View view, String subreddit, boolean onLoad) {
        if (!onLoad || !isSinglePane) {
            selectSubreddit(view, subreddit, !onLoad);
        }
    }

    @Override
    public void onThingSelected(View view, ThingBundle thingBundle, int pageType) {
        selectThing(view, thingBundle);
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

    private void selectSubreddit(View view, String subreddit, boolean force) {
        if (isSinglePane) {
            selectSubredditSinglePane(view, subreddit);
        } else {
            selectSubredditMultiPane(subreddit, force);
        }
    }

    private void selectSubredditSinglePane(View view, String subreddit) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_SUBREDDIT, subreddit);
        launchActivity(view, intent);
    }

    private void selectSubredditMultiPane(String subreddit, boolean force) {
        setRightFragment(R.id.right_container,
                getControlFragment().withSubreddit(subreddit),
                SubredditThingListFragment
                        .newInstance(accountName, subreddit, filter, isSingleChoice),
                force);
    }

    // Methods to select a thing

    private void selectThing(View view, ThingBundle thingBundle) {
        if (isSinglePane) {
            selectThingSinglePane(view, thingBundle);
        } else {
            selectThingMultiPane(thingBundle);
        }
    }

    protected void selectThingSinglePane(View view, ThingBundle thingBundle) {
        Intent intent = new Intent(this, thingActivityClass);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        launchActivity(view, intent);
    }

    private void selectThingMultiPane(ThingBundle thingBundle) {
        setThingFragment(getControlFragment().withThingBundle(thingBundle));
    }

    private void setThingFragment(ControlFragment controlFrag) {
        safePopBackStackImmediate();

        ThingFragment frag = ThingFragment.newInstance(accountName, controlFrag.getThingBundle());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(controlFrag, TAG_CONTROL_FRAGMENT);
        ft.replace(R.id.thing_container, frag, TAG_THING_FRAGMENT);
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
    public void onThingTitleDiscovery(String title) {
    }

    // Methods for setting the fragments on the screen.

    private <F extends Fragment & LeftFragment>
            void setLeftFragment(int containerId, ControlFragment controlFrag, F frag) {
        if (!Objects.equals(frag, getLeftFragment())) {
            safePopBackStackImmediate();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(controlFrag, TAG_CONTROL_FRAGMENT);
            ft.replace(containerId, frag, TAG_LEFT_FRAGMENT);
            removeFragment(ft, TAG_RIGHT_FRAGMENT);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                    | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commitAllowingStateLoss();

            refreshActionBar(null);
            refreshViews(null);
        }
    }

    private <F extends Fragment & RightFragment>
            void setCenterFragment(int containerId, ControlFragment controlFrag, F centerFrag,
                    boolean force) {
        setRightFragmentRemoveLeft(containerId, controlFrag, centerFrag, force, true);
    }

    private <F extends Fragment & RightFragment>
            void setRightFragment(int containerId, ControlFragment controlFrag, F rightFrag,
                    boolean force) {
        setRightFragmentRemoveLeft(containerId, controlFrag, rightFrag, force, false);
    }

    private <F extends Fragment & RightFragment>
            void setRightFragmentRemoveLeft(int containerId, ControlFragment controlFrag,
                    F rightFrag, boolean force, boolean removeLeft) {
        if (force || !Objects.equals(rightFrag, getRightFragment())) {
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
            int newWidth = hasLeftFragment() ? leftWidth : 0;
            thingBodyWidth = dm.widthPixels - newWidth - padding * 2;
        } else {
            thingBodyWidth = dm.widthPixels / 5 * 2 - padding * 3;
        }
    }

    private void updateVisibility(ThingBundle thingBundle) {
        boolean hasLeftFragment = hasLeftFragment();
        boolean hasThing = thingBundle != null;
        if (navContainer != null) {
            // Refresh the subreddit list container which is inside the navigation container.
            if (leftContainer != null) {
                leftContainer.setVisibility(getVisibility(hasLeftFragment));
            }
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
            int ntv = getVisibility(hasThing);
            if (ctv != ntv) {
                if (isVisible(ntv)) {
                    startAnimation(hasLeftFragment
                            ? ANIMATION_COLLAPSE_LEFT
                            : ANIMATION_COLLAPSE_RIGHT);
                } else {
                    startAnimation(hasLeftFragment
                            ? ANIMATION_EXPAND_LEFT
                            : ANIMATION_EXPAND_RIGHT);
                }
            } else if (leftContainer != null) {
                // Refresh the subreddit list container if the animations don't take care of it.
                leftContainer.setVisibility(getVisibility(hasLeftFragment && !hasThing));
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
            RightFragment rf = getRightFragment();
            if (rf != null) {
                ControlFragment cf = getControlFragment();
                ThingBundle thingBundle = cf.getThingBundle();
                if (thingBundle != null) {
                    rf.setSelectedThing(thingBundle.getThingId(), thingBundle.getLinkId());
                } else {
                    rf.setSelectedThing(null, null);
                }
            }
        }
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public String getSubreddit() {
        ControlFragment cf = getControlFragment();
        if (cf != null) {
            ThingBundle thingBundle = cf.getThingBundle();
            if (thingBundle != null) {
                String subreddit = thingBundle.getSubreddit();
                if (subreddit != null) {
                    return subreddit;
                }
            }

            String subreddit = cf.getSubreddit();
            if (subreddit != null) {
                return subreddit;
            }
        }
        return null;
    }

    @Override
    public boolean isShowingThing() {
        ControlFragment cf = getControlFragment();
        return cf != null && cf.getThingBundle() != null;
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

    private LeftFragment getLeftFragment() {
        return (LeftFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_LEFT_FRAGMENT);
    }

    private RightFragment getRightFragment() {
        return (RightFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_RIGHT_FRAGMENT);
    }

    protected NavigationFragment getNavigationFragment() {
        return (NavigationFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_LEFT_FRAGMENT);
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
        ObjectAnimator ncTransX = ObjectAnimator
                .ofFloat(navContainer, "translationX", -fullNavWidth, 0);
        ObjectAnimator tpTransX = ObjectAnimator
                .ofFloat(thingContainer, "translationX", 0, fullNavWidth);

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
        ObjectAnimator ncTransX = ObjectAnimator
                .ofFloat(navContainer, "translationX", 0, -leftWidth);

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
                .ofFloat(leftContainer, "translationX", 0, -leftWidth);
        ObjectAnimator tlTransX = ObjectAnimator
                .ofFloat(rightContainer, "translationX", 0, -leftWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(slTransX).with(tlTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                leftContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                leftContainer.setVisibility(View.VISIBLE);
                rightContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                rightContainer.setVisibility(View.VISIBLE);
                thingContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                leftContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                leftContainer.setVisibility(View.GONE);
                rightContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                rightContainer.setTranslationX(0);
                thingContainer.setVisibility(View.VISIBLE);
            }
        });
        return as;
    }

    private AnimatorSet newExpandLeftAnimator() {
        ObjectAnimator slTransX = ObjectAnimator
                .ofFloat(leftContainer, "translationX", -leftWidth, 0);
        ObjectAnimator tlTransX = ObjectAnimator
                .ofFloat(rightContainer, "translationX", -leftWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(slTransX).with(tlTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                leftContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                leftContainer.setVisibility(View.VISIBLE);
                rightContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                rightContainer.setVisibility(View.VISIBLE);
                thingContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                leftContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                rightContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        return as;
    }

    private AnimatorSet newCollapseRightAnimator() {
        ObjectAnimator tcTransX = ObjectAnimator
                .ofFloat(thingContainer, "translationX", leftWidth, 0);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(tcTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (leftContainer != null) {
                    leftContainer.setVisibility(View.GONE);
                }
                rightContainer.setVisibility(View.VISIBLE);
                thingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                thingContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        return as;
    }

    private AnimatorSet newExpandRightAnimator() {
        ObjectAnimator tcTransX = ObjectAnimator
                .ofFloat(thingContainer, "translationX", 0, leftWidth);

        AnimatorSet as = new AnimatorSet();
        as.setDuration(durationMs).play(tcTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (leftContainer != null) {
                    leftContainer.setVisibility(View.GONE);
                }
                rightContainer.setVisibility(View.VISIBLE);
                thingContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                thingContainer.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                thingContainer.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        return as;
    }
}
