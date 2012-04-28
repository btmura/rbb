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
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.ControlFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.widget.SearchPagerAdapter;

public class SearchActivity extends AbstractBrowserActivity implements
        ActionBar.TabListener,
        SearchView.OnQueryTextListener,
        ViewPager.OnPageChangeListener,
        View.OnFocusChangeListener {

    public static final String TAG = "SearchActivity";

    public static final String EXTRA_QUERY = "q";

    private static final String STATE_QUERY = "q";
    private static final String STATE_TAB_POSITION = "t";

    private static final int TAB_POSTS = 0;
    private static final int TAB_SUBREDDITS = 1;

    private ActionBar bar;
    private ViewPager pager;
    private String query;

    private MenuItem searchItem;
    private SearchView searchView;

    private boolean tabListenerDisabled;

    public SearchActivity() {
        super(R.layout.search);
    }

    @Override
    protected void initPrereqs(Bundle savedInstanceState) {
        setThingListActivityFlags(ThingListActivity.FLAG_SHOW_ADD_ACTION);
        setThingListFragmentFlags(ThingListFragment.FLAG_SHOW_ADD_ACTION);

        if (savedInstanceState == null) {
            query = getIntentQuery();
        } else {
            query = savedInstanceState.getString(STATE_QUERY, "");
        }

        pager = (ViewPager) findViewById(R.id.pager);

        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(query);

        tabListenerDisabled = true;
        bar.addTab(bar.newTab().setText(R.string.tab_posts).setTabListener(this));
        bar.addTab(bar.newTab().setText(R.string.tab_subreddits).setTabListener(this));
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_TAB_POSITION));
        }
        tabListenerDisabled = false;
    }

    private String getIntentQuery() {
        String q = getIntent().getStringExtra(EXTRA_QUERY);
        if (q == null) {
            q = "android";
        }
        return q.trim();
    }

    @Override
    protected boolean isSinglePane() {
        return pager != null;
    }

    @Override
    protected boolean hasSubredditList() {
        return bar.getSelectedNavigationIndex() == TAB_SUBREDDITS;
    }

    @Override
    protected void initSinglePaneLayout(Bundle savedInstanceState) {
        pager.setOnPageChangeListener(this);
        pager.setAdapter(new SearchPagerAdapter(getFragmentManager(), query));
    }

    @Override
    protected void initMultiPaneLayout(Bundle savedInstanceState, int filter) {
        if (savedInstanceState == null) {
            submitQuery();
        }
    }

    private void submitQuery() {
        if (query != null && !query.isEmpty()) {
            bar.setTitle(query);
            if (searchItem != null) {
                searchItem.collapseActionView();
            }
            if (pager != null) {
                pager.setAdapter(new SearchPagerAdapter(getFragmentManager(), query));
                pager.setCurrentItem(bar.getSelectedNavigationIndex());
            } else {
                updateFragments();
            }
        }
    }

    private void updateFragments() {
        refreshSubredditListVisibility();
        switch (bar.getSelectedNavigationIndex()) {
            case TAB_POSTS:
                updatePostResultsFragments();
                break;

            case TAB_SUBREDDITS:
                updateSubredditResultsFragments();
                break;
        }
    }

    private void updatePostResultsFragments() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }

        refreshContainers(null);

        Fragment cf = ControlFragment.newInstance(null, null, -1, 0);
        Fragment slf = getFragmentManager().findFragmentByTag(SubredditListFragment.TAG);
        Fragment tlf = ThingListFragment.newSearchInstance(query,
                ThingListFragment.FLAG_SINGLE_CHOICE);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        if (slf != null) {
            ft.remove(slf);
        }
        ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        ft.commit();
    }

    private void updateSubredditResultsFragments() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.removeOnBackStackChangedListener(this);
            fm.popBackStackImmediate();
            fm.addOnBackStackChangedListener(this);
        }

        refreshContainers(null);

        Fragment cf = ControlFragment.newInstance(null, null, -1, 0);
        Fragment slf = SubredditListFragment.newSearchInstance(query,
                SubredditListFragment.FLAG_SINGLE_CHOICE);
        Fragment tlf = getFragmentManager().findFragmentByTag(ThingListFragment.TAG);

        FragmentTransaction ft = fm.beginTransaction();
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);
        if (tlf != null) {
            ft.remove(tlf);
        }
        ft.commit();
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        if (tabListenerDisabled) {
            return;
        }
        if (pager != null) {
            pager.setCurrentItem(tab.getPosition());
        } else {
            updateFragments();
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    public boolean onQueryTextSubmit(String query) {
        this.query = query;
        submitQuery();
        return true;
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    protected void refreshActionBar(Subreddit subreddit, Thing thing, int filter) {
        bar.setTitle(query);
    }

    @Override
    public void onPageSelected(int position) {
        if (pager != null) {
            bar.setSelectedNavigationItem(position);
        } else {
            super.onPageSelected(position);
        }
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            searchItem.collapseActionView();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, query);
        outState.putInt(STATE_TAB_POSITION, bar.getSelectedNavigationIndex());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                searchItem.expandActionView();
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search_menu, menu);
        searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
        return true;
    }
}
