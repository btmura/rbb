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

package com.btmura.android.reddit.browser;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.browser.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.search.SearchActivity;

public class BrowserActivity extends Activity implements OnBackStackChangedListener,
        OnNavigationListener, OnQueryTextListener, OnFocusChangeListener, OnPageChangeListener,
        OnSubredditSelectedListener, OnThingSelectedListener {

    public static final String EXTRA_SUBREDDIT = "subreddit";

    private static final String FRAG_CONTROL = "control";
    private static final String FRAG_SUBREDDIT_LIST = "subredditList";
    private static final String FRAG_THING_LIST = "thingList";

    private static final int NAVLAYOUT_ORIGINAL = 0;
    private static final int NAVLAYOUT_SIDENAV = 1;

    private static final int REQUEST_ADD_SUBREDDITS = 0;

    private static final String STATE_LAST_SELECTED_FILTER = "lastSelectedFilter";

    private ActionBar bar;
    private SearchView searchView;
    private FilterAdapter filterSpinner;
    private int lastSelectedFilter;

    private View singleContainer;
    private View navContainer;
    private View subredditListContainer;
    private View thingClickAbsorber;
    private ViewPager thingPager;

    private ShareActionProvider shareProvider;
    private boolean singleChoice;
    private int tlfContainerId;
    private int slfContainerId;

    private AnimatorSet showNavContainer;
    private AnimatorSet hideNavContainer;
    private AnimatorSet openSideNav;
    private AnimatorSet closeSideNav;
    private int sideNavWidth;

    private boolean insertSlfToBackStack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getFragmentManager().addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setCustomView(R.layout.search_view);

        searchView = (SearchView) bar.getCustomView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);

        filterSpinner = new FilterAdapter(this);
        bar.setListNavigationCallbacks(filterSpinner, this);

        singleContainer = findViewById(R.id.single_container);
        thingPager = (ViewPager) findViewById(R.id.thing_pager);
        thingPager.setOnPageChangeListener(this);

        singleChoice = singleContainer == null;
        if (singleContainer != null) {
            tlfContainerId = slfContainerId = R.id.single_container;
        } else {
            tlfContainerId = R.id.thing_list_container;
            slfContainerId = R.id.subreddit_list_container;
            navContainer = findViewById(R.id.nav_container);
            subredditListContainer = findViewById(R.id.subreddit_list_container);
            thingClickAbsorber = findViewById(R.id.thing_click_absorber);
            if (thingClickAbsorber != null) {
                thingClickAbsorber.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        animateSideNav(false, null);
                    }
                });
            }
        }

        if (navContainer != null) {
            sideNavWidth = getResources().getDisplayMetrics().widthPixels / 2;
            int duration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            showNavContainer = getNavContainerAnimator(true, duration);
            hideNavContainer = getNavContainerAnimator(false, duration);
            openSideNav = getSideNavAnimator(true, duration);
            closeSideNav = getSideNavAnimator(false, duration);
        }

        insertSlfToBackStack = isSubredditPreview();
        if (savedInstanceState == null) {
            initFragments(getTargetSubreddit());
        }
    }

    private boolean isSubredditPreview() {
        return getIntent().hasExtra(EXTRA_SUBREDDIT);
    }

    private Subreddit getTargetSubreddit() {
        String name = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        return name != null ? Subreddit.newInstance(name) : null;
    }

    private void initFragments(Subreddit sr) {
        refreshActionBar(sr, null, 0);
        refreshContainers(null);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ControlFragment cf = ControlFragment.newInstance(sr, null, lastSelectedFilter);
        ft.add(cf, FRAG_CONTROL);
        if (singleContainer == null || sr == null) {
            ft.replace(slfContainerId, SubredditListFragment.newInstance(singleChoice),
                    FRAG_SUBREDDIT_LIST);
        }
        if (sr != null) {
            ft.replace(tlfContainerId,
                    ThingListFragment.newInstance(sr, lastSelectedFilter, singleChoice),
                    FRAG_THING_LIST);
        }
        ft.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_LAST_SELECTED_FILTER, lastSelectedFilter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            lastSelectedFilter = savedInstanceState.getInt(STATE_LAST_SELECTED_FILTER);
            updateThingPager(getThing());
            onBackStackChanged();
        }
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        lastSelectedFilter = itemPosition;
        if (itemId != getFilter()) {
            selectSubreddit(getSubreddit(), itemPosition);
        }
        return true;
    }

    public void onSubredditSelected(Subreddit sr, int event) {
        switch (event) {
            case OnSubredditSelectedListener.FLAG_ITEM_CLICKED:
                selectSubreddit(sr, lastSelectedFilter);
                break;

            case OnSubredditSelectedListener.FLAG_LOAD_FINISHED:
                if (singleContainer == null && !isVisible(FRAG_THING_LIST)) {
                    getSubredditListFragment().setSelectedSubreddit(sr);
                    selectSubreddit(sr, lastSelectedFilter);
                }
                break;
        }
    }

    private void selectSubreddit(Subreddit sr, int filter) {
        FragmentManager fm = getFragmentManager();
        fm.removeOnBackStackChangedListener(this);
        if (singleContainer != null) {
            // Pop in case the user changed from what's hot to top.
            fm.popBackStackImmediate(FRAG_THING_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            fm.popBackStackImmediate();
        }
        fm.addOnBackStackChangedListener(this);

        refreshActionBar(sr, null, filter);
        refreshContainers(null);

        FragmentTransaction ft = fm.beginTransaction();
        ControlFragment controlFrag = ControlFragment.newInstance(sr, null, filter);
        ft.add(controlFrag, FRAG_CONTROL);
        ThingListFragment thingListFrag = ThingListFragment.newInstance(sr, filter, singleChoice);
        ft.replace(tlfContainerId, thingListFrag, FRAG_THING_LIST);
        if (singleContainer != null) {
            ft.addToBackStack(FRAG_THING_LIST);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.commit();
    }

    public void onThingSelected(final Thing thing, final int position) {
        if (navContainer != null && isSideNavShowing()) {
            animateSideNav(false, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    selectThing(thing, position);
                    animation.removeListener(this);
                }
            });
        } else {
            selectThing(thing, position);
        }
    }

    private void selectThing(Thing thing, int position) {
        FragmentManager fm = getFragmentManager();
        if (singleContainer == null) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }

        updateThingPager(thing);

        FragmentTransaction ft = fm.beginTransaction();
        ControlFragment cf = ControlFragment.newInstance(getSubreddit(), thing, getFilter());
        ft.add(cf, FRAG_CONTROL);

        if (singleContainer != null) {
            ThingListFragment tf = getThingListFragment();
            if (tf != null) {
                ft.remove(tf);
            }
        }

        ft.addToBackStack(null);
        ft.commit();
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

    private Subreddit getSubreddit() {
        return getControlFragment().getTopic();
    }

    private Thing getThing() {
        return getControlFragment().getThing();
    }

    private int getFilter() {
        return getControlFragment().getFilter();
    }

    private ControlFragment getControlFragment() {
        return (ControlFragment) getFragmentManager().findFragmentByTag(FRAG_CONTROL);
    }

    private SubredditListFragment getSubredditListFragment() {
        return (SubredditListFragment) getFragmentManager().findFragmentByTag(FRAG_SUBREDDIT_LIST);
    }

    private ThingListFragment getThingListFragment() {
        return (ThingListFragment) getFragmentManager().findFragmentByTag(FRAG_THING_LIST);
    }

    public void onBackStackChanged() {
        Subreddit sr = getSubreddit();
        Thing t = getThing();
        refreshActionBar(sr, t, getFilter());
        refreshCheckedItems();
        refreshContainers(t);
        invalidateOptionsMenu();
    }

    private void refreshActionBar(Subreddit sr, Thing t, int filter) {
        if (t != null && singleContainer != null) {
            setThingNavigationMode(t);
        } else if (sr != null) {
            setThingListNavigationMode(sr);
        } else {
            setSubredditListNavigationMode();
        }

        bar.setDisplayHomeAsUpEnabled(singleContainer != null && sr != null || t != null
                || getIntent().hasExtra(EXTRA_SUBREDDIT));
        if (bar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
            bar.setSelectedNavigationItem(filter);
        }
    }

    private void setThingNavigationMode(Thing t) {
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayShowCustomEnabled(false);
        bar.setTitle(t.assureTitle(this).title);
    }

    private void setThingListNavigationMode(Subreddit sr) {
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(false);
        filterSpinner.setSubreddit(sr.getTitle(this));
    }

    private void setSubredditListNavigationMode() {
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayShowCustomEnabled(false);
        bar.setTitle(R.string.app_name);
    }

    private void refreshCheckedItems() {
        if (isVisible(FRAG_SUBREDDIT_LIST)) {
            getSubredditListFragment().setSelectedSubreddit(getSubreddit());
        }

        if (isVisible(FRAG_THING_LIST)) {
            getThingListFragment().setChosenThing(getThing());
        }
    }

    private void refreshContainers(Thing t) {
        if (thingPager != null) {
            thingPager.setVisibility(t != null ? View.VISIBLE : View.GONE);
            if (t == null) {
                // Avoid nested executePendingTransactions that would occur by
                // doing popBackStack.
                thingPager.post(new Runnable() {
                    public void run() {
                        updateThingPager(null);
                    }
                });
            }
        }
        if (navContainer != null) {
            if (isSideNavShowing() && t == null) {
                animateNavContainer(true);
            } else {
                int newVisibility = t != null ? View.GONE : View.VISIBLE;
                if (navContainer.getVisibility() != newVisibility) {
                    animateNavContainer(t == null);
                }
            }
        }
        if (singleContainer != null) {
            singleContainer.setVisibility(t != null ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        shareProvider = (ShareActionProvider) menu.findItem(R.id.menu_share).getActionProvider();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean isThingList = isVisible(FRAG_THING_LIST);
        Thing thing = getThing();
        boolean isThing = thing != null;
        boolean isLink = isThing && !thing.isSelf;

        menu.findItem(R.id.menu_add).setVisible((isThingList || isThing) && isSubredditPreview());
        menu.findItem(R.id.menu_refresh).setVisible(isThingList && singleContainer != null);

        menu.findItem(R.id.menu_link).setVisible(isLink && !isShowingLink(thing));
        menu.findItem(R.id.menu_comments).setVisible(isLink && isShowingLink(thing));

        menu.findItem(R.id.menu_share).setVisible(isThing);
        menu.findItem(R.id.menu_copy_url).setVisible(isThing);
        menu.findItem(R.id.menu_view).setVisible(isThing);

        updateShareActionIntent(thing);
        return true;
    }

    private boolean isVisible(String tag) {
        Fragment f = getFragmentManager().findFragmentByTag(tag);
        return f != null && f.isAdded();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome();
                return true;

            case R.id.menu_add:
                handleAdd();
                return true;

            case R.id.menu_search_for_subreddits:
                handleSearchForSubreddits();
                return true;

            case R.id.menu_link:
                handleLink();
                return true;

            case R.id.menu_comments:
                handleComments();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl();
                return true;

            case R.id.menu_view:
                handleView();
                return true;
        }
        return false;
    }

    private void handleAdd() {
        Subreddit sr = getSubreddit();
        if (sr != null) {
            ContentValues values = new ContentValues(1);
            values.put(Subreddits.COLUMN_NAME, sr.name);
            Provider.addSubredditInBackground(getApplicationContext(), values);
        }
    }

    private void handleSearchForSubreddits() {
        searchView.setQuery("", false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        searchView.requestFocus();
    }

    public void onPageSelected(int position) {
        invalidateOptionsMenu();
    }

    public boolean onQueryTextSubmit(String query) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(SearchActivity.EXTRA_QUERY, query);
        startActivityForResult(intent, REQUEST_ADD_SUBREDDITS);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_SUBREDDITS:
                refreshActionBar(getSubreddit(), getThing(), getFilter());
                break;

            default:
                throw new IllegalStateException("Unexpected request code: " + requestCode);
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (v == searchView && !hasFocus) {
            refreshActionBar(getSubreddit(), getThing(), getFilter());
        }
    }

    private void handleHome() {
        FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            if (navContainer != null && !isSideNavShowing()) {
                animateSideNav(true, null);
            } else {
                fm.popBackStack();
            }
        } else if (singleContainer != null && insertSlfToBackStack) {
            insertSlfToBackStack = false;
            initFragments(null);
        } else {
            finish();
        }
    }

    private void handleLink() {
        thingPager.setCurrentItem(0);
    }

    private void handleComments() {
        thingPager.setCurrentItem(1);
    }

    private void handleCopyUrl() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String text = getLink(getThing());
        clipboard.setText(text);
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void handleView() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getLink(getThing())));
        startActivity(Intent.createChooser(intent, getString(R.string.menu_view)));
    }

    private String getLink(Thing thing) {
        return isShowingLink(thing) ? thing.url : "http://www.reddit.com" + thing.permaLink;
    }

    private void updateShareActionIntent(Thing thing) {
        if (thing != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    Formatter.formatTitle(this, thing.assureTitle(this).title));
            intent.putExtra(Intent.EXTRA_TEXT, getLink(thing));
            shareProvider.setShareIntent(intent);
        }
    }

    private boolean isShowingLink(Thing t) {
        int position = thingPager.getCurrentItem();
        return ThingPagerAdapter.getType(t, position) == ThingPagerAdapter.TYPE_LINK;
    }

    private boolean isSideNavShowing() {
        return thingClickAbsorber.isShown();
    }

    private void animateNavContainer(final boolean show) {
        changeNavContainerLayout(NAVLAYOUT_ORIGINAL);
        AnimatorSet as = show ? showNavContainer : hideNavContainer;
        navContainer.setVisibility(View.VISIBLE);
        navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        thingPager.setTranslationX(0);
        as.start();
    }

    private void animateSideNav(final boolean show, AnimatorListener listener) {
        changeNavContainerLayout(NAVLAYOUT_SIDENAV);
        AnimatorSet as = show ? openSideNav : closeSideNav;
        navContainer.setVisibility(View.VISIBLE);
        navContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        thingPager.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (listener != null) {
            as.addListener(listener);
        }
        as.start();
    }

    private void changeNavContainerLayout(int layout) {
        int subredditListVisibility;
        int clickAbsorberVisibility;
        switch (layout) {
            case NAVLAYOUT_ORIGINAL:
                subredditListVisibility = View.VISIBLE;
                clickAbsorberVisibility = View.GONE;
                break;

            case NAVLAYOUT_SIDENAV:
                subredditListVisibility = View.GONE;
                clickAbsorberVisibility = View.VISIBLE;
                break;

            default:
                throw new IllegalStateException();
        }

        subredditListContainer.setVisibility(subredditListVisibility);
        thingClickAbsorber.setVisibility(clickAbsorberVisibility);
    }

    private AnimatorSet getNavContainerAnimator(final boolean show, int duration) {
        int width = getResources().getDimensionPixelSize(R.dimen.subreddit_list_width);

        ObjectAnimator ncTransX;
        if (show) {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", -width, 0);
        } else {
            ncTransX = ObjectAnimator.ofFloat(navContainer, "translationX", 0, -width);
        }
        ncTransX.setDuration(duration);

        AnimatorSet as = new AnimatorSet();
        as.play(ncTransX);
        as.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                if (!show) {
                    navContainer.setVisibility(View.GONE);
                }
            }
        });
        return as;
    }

    private AnimatorSet getSideNavAnimator(final boolean show, int duration) {
        ObjectAnimator ncTransX;
        ObjectAnimator tpTransX;

        if (show) {
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
            public void onAnimationEnd(Animator animation) {
                navContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                thingPager.setLayerType(View.LAYER_TYPE_NONE, null);
                if (!show) {
                    navContainer.setVisibility(View.GONE);
                }
            }
        });
        return as;
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }
}
