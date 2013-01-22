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
import android.app.Fragment;
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
import com.btmura.android.reddit.app.ThingMenuFragment.OnThingMenuEventListener;
import com.btmura.android.reddit.app.ThingMenuFragment.ThingMenuEventListenerHolder;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingBundle;

abstract class AbstractBrowserActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnSubredditSelectedListener,
        OnSubredditEventListener,
        OnThingSelectedListener,
        OnThingEventListener,
        OnThingMenuEventListener,
        OnBackStackChangedListener,
        AccountNameHolder,
        SubredditNameHolder,
        ThingMenuEventListenerHolder {

    public static final String TAG = "AbstractBrowserActivity";

    private static final int ANIMATION_OPEN_NAV = 0;
    private static final int ANIMATION_CLOSE_NAV = 1;
    private static final int ANIMATION_OPEN_SUBREDDIT_LIST = 2;
    private static final int ANIMATION_CLOSE_SUBREDDIT_LIST = 3;

    protected ActionBar bar;

    protected boolean isSinglePane;
    private ViewPager thingPager;
    private int sfFlags;
    private int tfFlags;

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

    private OnThingMenuEventListener thingMenuEventListener;

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

    protected abstract void setupCommonFragments(Bundle savedInstanceState);

    private void setupCommonViews() {
        if (!isSinglePane) {
            getFragmentManager().addOnBackStackChangedListener(this);

            sfFlags |= SubredditListFragment.FLAG_SINGLE_CHOICE;
            tfFlags |= ThingListFragment.FLAG_SINGLE_CHOICE;

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

    public abstract Loader<AccountResult> onCreateLoader(int id, Bundle args);

    public abstract void onLoadFinished(Loader<AccountResult> loader, AccountResult result);

    public abstract void onLoaderReset(Loader<AccountResult> loader);

    public abstract String getAccountName();

    protected abstract int getFilter();

    protected abstract boolean hasSubredditList();

    protected void setSubredditListNavigation(String subreddit, boolean isRandom, String query,
            Bundle thingBundle) {
        if (isSinglePane) {
            setSubredditListNavigationSinglePane(query);
        } else {
            setSubredditListNavigationMultiPane(subreddit, isRandom, query, thingBundle);
        }
    }

    private void setSubredditListNavigationSinglePane(String query) {
        Fragment sf = SubredditListFragment.newInstance(getAccountName(), null, query, sfFlags);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.subreddit_list_container, sf, SubredditListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setSubredditListNavigationMultiPane(String subreddit, boolean isRandom,
            String query, Bundle thingBundle) {
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
        Fragment sf = SubredditListFragment.newInstance(accountName, subreddit, query, sfFlags);
        Fragment tf = ThingListFragment.newSubredditInstance(accountName, subreddit, filter,
                tfFlags);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, sf, SubredditListFragment.TAG);
        ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);

        // If a thing was specified by the thingBundle argument, then add the
        // ThingMenuFragment. Otherwise, make sure to remove the prior
        // ThingMenuFragment for some other thing.
        if (thingBundle != null) {
            Fragment mf = ThingMenuFragment.newInstance(accountName, thingBundle);
            ft.add(mf, ThingMenuFragment.TAG);
        } else {
            Fragment mf = getThingMenuFragment();
            if (mf != null) {
                ft.remove(mf);
            }
        }

        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);

        // Use commitAllowingStateLoss to allow changing accounts when the
        // account list activity is a dialog on large devices and we remove an
        // account causing new fragment transactions to occur.
        ft.commitAllowingStateLoss();

        refreshSubredditListVisibility();
        refreshActionBar(subreddit, thingBundle);
        refreshViews(thingBundle);
        refreshThingPager(thingBundle);
    }

    protected void setQueryThingListNavigation(String subreddit, String query) {
        setThingListNavigation(subreddit, query, null, null);
    }

    protected void setProfileThingListNavigation(String profileUser) {
        setThingListNavigation(null, null, profileUser, null);
    }

    protected void setMessageThingListNavigation(String messageUser) {
        setThingListNavigation(null, null, null, messageUser);
    }

    private void setThingListNavigation(String subreddit, String query, String profileUser,
            String messageUser) {
        if (isSinglePane) {
            setThingListNavigationSinglePane(subreddit, query, profileUser, messageUser);
        } else {
            setThingListNavigationMultiPane(subreddit, query, profileUser, messageUser);
        }
    }

    private void setThingListNavigationSinglePane(String subreddit, String query,
            String profileUser, String messageUser) {
        String accountName = getAccountName();
        int filter = getFilter();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationSinglePane accountName: " + accountName
                    + " subreddit: " + subreddit
                    + " query: " + query
                    + " profileUser: " + profileUser
                    + " messageUser: " + messageUser
                    + " filter: " + filter);
        }

        Fragment tf = ThingListFragment.newInstance(accountName, subreddit, query, profileUser,
                messageUser, filter, tfFlags);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();
    }

    private void setThingListNavigationMultiPane(String subreddit, String query,
            String profileUser, String messageUser) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setThingListNavigationMultiPane accountName: " + accountName
                    + " subreddit: " + subreddit
                    + " query: " + query
                    + " profileUser: " + profileUser
                    + " messageUser: " + messageUser
                    + " filter: " + filter);
        }

        Fragment cf = ControlFragment.newInstance(accountName, subreddit, false, null, filter);
        Fragment sf = getSubredditListFragment();
        Fragment tf = ThingListFragment.newInstance(accountName, subreddit, query, profileUser,
                messageUser, filter, tfFlags);
        Fragment mf = getThingMenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        if (sf != null) {
            ft.remove(sf);
        }
        ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);
        if (mf != null) {
            ft.remove(mf);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                | FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        refreshSubredditListVisibility();
        refreshActionBar(subreddit, null);
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

    public void onSubredditSelected(String subreddit) {
        selectSubreddit(subreddit, Subreddits.isRandom(subreddit), 0);
    }

    protected void selectSubreddit(String subreddit, boolean isRandom, int flags) {
        if (isSinglePane) {
            selectSubredditSinglePane(subreddit, flags);
        } else {
            selectSubredditMultiPane(subreddit, isRandom);
        }
    }

    private void selectSubredditSinglePane(String subreddit, int flags) {
        Intent intent = new Intent(this, ThingListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, subreddit);
        intent.putExtra(ThingListActivity.EXTRA_FLAGS, flags);
        startActivity(intent);
    }

    private void selectSubredditMultiPane(String subreddit, boolean isRandom) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        Fragment cf = ControlFragment.newInstance(accountName, subreddit, isRandom, null, filter);
        Fragment tf = ThingListFragment.newSubredditInstance(accountName, subreddit, filter,
                tfFlags);
        Fragment mf = getThingMenuFragment();

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.thing_list_container, tf, ThingListFragment.TAG);
        if (mf != null) {
            ft.remove(mf);
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

    public void onThingSelected(Bundle thingBundle) {
        selectThing(thingBundle, 0);
    }

    protected void selectThing(Bundle thingBundle, int flags) {
        if (isSinglePane) {
            selectThingSinglePane(thingBundle, 0);
        } else {
            selectThingMultiPane(thingBundle);
        }
    }

    private void selectThingSinglePane(Bundle thingBundle, int flags) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingActivity.EXTRA_THING_BUNDLE, thingBundle);
        intent.putExtra(ThingActivity.EXTRA_FLAGS, flags);
        startActivity(intent);
    }

    private void selectThingMultiPane(Bundle thingBundle) {
        safePopBackStackImmediate();

        String accountName = getAccountName();
        int filter = getFilter();

        String subreddit = ThingBundle.getSubreddit(thingBundle);
        Fragment cf = ControlFragment.newInstance(accountName, subreddit,
                Subreddits.isRandom(subreddit), thingBundle, filter);
        Fragment mf = ThingMenuFragment.newInstance(accountName, thingBundle);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.add(mf, ThingMenuFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();

        refreshThingPager(thingBundle);
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

                ThingPagerAdapter adapter = (ThingPagerAdapter) thingPager.getAdapter();
                adapter.addPage(0, ThingPagerAdapter.TYPE_LINK);
                thingPager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
            }

            ThingMenuFragment mf = getThingMenuFragment();
            mf.setNewCommentItemEnabled(thingHolder.isReplyable());
            mf.setSaved(thingHolder.isSaved());
        }
    }

    public void onLinkMenuItemClick() {
        thingPager.setCurrentItem(ThingPagerAdapter.PAGE_LINK);
    }

    public void onCommentMenuItemClick() {
        thingPager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
    }

    public void setOnThingMenuEventListener(OnThingMenuEventListener listener) {
        this.thingMenuEventListener = listener;
    }

    public void onSavedItemSelected() {
        if (thingMenuEventListener != null) {
            thingMenuEventListener.onSavedItemSelected();
        }
    }

    public void onUnsavedItemSelected() {
        if (thingMenuEventListener != null) {
            thingMenuEventListener.onUnsavedItemSelected();
        }
    }

    public void onNewItemSelected() {
        if (thingMenuEventListener != null) {
            thingMenuEventListener.onNewItemSelected();
        }
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
                    startAnimation(ANIMATION_OPEN_NAV);
                } else {
                    startAnimation(ANIMATION_CLOSE_NAV);
                }
            }
        } else {
            if (hasSubredditList() && subredditListContainer != null) {
                int currVisibility = subredditListContainer.getVisibility();
                int nextVisibility = hasThing ? View.GONE : View.VISIBLE;
                if (currVisibility != nextVisibility) {
                    if (nextVisibility == View.VISIBLE) {
                        startAnimation(ANIMATION_OPEN_SUBREDDIT_LIST);
                    } else {
                        startAnimation(ANIMATION_CLOSE_SUBREDDIT_LIST);
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

    protected boolean hasThing() {
        return getThingMenuFragment() != null;
    }

    private ControlFragment getControlFragment() {
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

    private ThingMenuFragment getThingMenuFragment() {
        return (ThingMenuFragment) getFragmentManager()
                .findFragmentByTag(ThingMenuFragment.TAG);
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
