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
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.SubredditDetails;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.SubredditDetailsListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.widget.SearchPagerAdapter;

public class SearchActivity extends Activity implements
        ActionBar.TabListener,
        SearchView.OnQueryTextListener,
        SubredditDetailsListFragment.OnSubredditDetailsSelectedListener,
        ThingListFragment.OnThingSelectedListener,
        ViewPager.OnPageChangeListener,
        View.OnFocusChangeListener {

    public static final String TAG = "SearchActivity";

    public static final String EXTRA_QUERY = "q";

    private static final String STATE_QUERY = "q";

    private ActionBar bar;
    private ViewPager pager;
    private String query;

    private MenuItem searchItem;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.addTab(bar.newTab().setText(R.string.tab_subreddits).setTabListener(this));
        bar.addTab(bar.newTab().setText(R.string.tab_posts).setTabListener(this));

        if (savedInstanceState == null) {
            query = getIntentQuery();
        } else {
            query = savedInstanceState.getString(STATE_QUERY, "");
        }

        pager = (ViewPager) findViewById(R.id.pager);
        if (pager != null) {
            pager.setOnPageChangeListener(this);
            pager.setAdapter(new SearchPagerAdapter(getFragmentManager(), query));
        }

        bar.setTitle(query);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    private String getIntentQuery() {
        String q = getIntent().getStringExtra(EXTRA_QUERY);
        if (q == null) {
            q = "android";
        }
        return q.trim();
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        if (pager != null) {
            pager.setCurrentItem(tab.getPosition());
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

    private void submitQuery() {
        if (query != null && !query.isEmpty()) {
            bar.setTitle(query);
            if (searchItem != null) {
                searchItem.collapseActionView();
            }
            if (pager != null) {
                pager.setAdapter(new SearchPagerAdapter(getFragmentManager(), query));
                pager.setCurrentItem(bar.getSelectedNavigationIndex());
            }
        }
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void onSubredditDetailsSelected(SubredditDetails details, int position) {
        if (pager != null) {
            Subreddit s = Subreddit.newInstance(details.displayName);
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, s);
            intent.putExtra(ThingListActivity.EXTRA_SHOW_ADD_BUTTON, true);
            startActivity(intent);
        }
    }

    public void onThingSelected(Thing thing, int position) {
        if (pager != null) {
            Intent intent = new Intent(this, ThingActivity.class);
            intent.putExtra(ThingActivity.EXTRA_THING, thing);
            startActivity(intent);
        }
    }

    public int getThingBodyWidth() {
        return 0;
    }

    public void onPageSelected(int position) {
        bar.setSelectedNavigationItem(position);
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    public void onPageScrollStateChanged(int state) {
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

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome();
                return true;

            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    private void handleHome() {
        finish();
    }
}
