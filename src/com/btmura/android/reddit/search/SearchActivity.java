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

package com.btmura.android.reddit.search;

import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.search.SubredditInfoListFragment.OnSelectedListener;
import com.btmura.android.reddit.sidebar.SidebarFragment;

public class SearchActivity extends Activity implements OnQueryTextListener, OnSelectedListener,
        OnBackStackChangedListener, TabListener {

    public static final String EXTRA_QUERY = "q";

    private static final String FRAG_SUBREDDITS = "s";
    private static final String FRAG_DETAILS = "d";

    private static final String STATE_QUERY = "q";

    private boolean singleChoice;
    private int slfContainerId;

    private String query;
    private boolean restoringState;

    private MenuItem searchItem;
    private SearchView searchView;
    private View singleContainer;

    private ActionBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        singleContainer = findViewById(R.id.single_container);
        singleChoice = singleContainer == null;
        if (singleContainer != null) {
            slfContainerId = R.id.single_container;
        } else {
            slfContainerId = R.id.subreddit_list_container;
        }

        restoringState = savedInstanceState != null;
        if (savedInstanceState == null) {
            query = getIntentQuery();
        } else {
            query = savedInstanceState.getString(STATE_QUERY, "");
        }

        FragmentManager manager = getFragmentManager();
        manager.addOnBackStackChangedListener(this);

        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.addTab(bar.newTab().setText(R.string.tab_subreddits).setTabListener(this));

        refreshTitle();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    }

    private String getIntentQuery() {
        String q = getIntent().getStringExtra(EXTRA_QUERY);
        if (q == null) {
            q = "";
        }
        return q.trim();
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.v("S", "onTabSelected");
        if (restoringState) {
            restoringState = false;
        } else {
            submitQuery();
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        if (singleContainer != null) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }
    }

    public boolean onQueryTextSubmit(String query) {
        this.query = query;
        submitQuery();
        return true;
    }

    private void refreshTitle() {
        bar.setTitle(getString(R.string.search_title, query));
    }

    private void submitQuery() {
        refreshTitle();
        if (searchItem != null) {
            searchItem.collapseActionView();
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(slfContainerId, SubredditInfoListFragment.newInstance(query, singleChoice),
                FRAG_SUBREDDITS);

        SidebarFragment df = getDetailsFragment();
        if (df != null) {
            ft.remove(df);
        }

        ft.commit();
    }

    public void onSelected(List<SubredditInfo> infos, int position, int event) {
        switch (event) {
            case OnSelectedListener.EVENT_LIST_ITEM_CLICKED:
                handleListItemClicked(infos, position);
                break;

            case OnSelectedListener.EVENT_ACTION_ITEM_CLICKED:
                handleActionItemClicked(infos);
                break;

            default:
                throw new IllegalArgumentException("Unexpected event: " + event);
        }
    }

    private void handleListItemClicked(List<SubredditInfo> infos, int position) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        int containerId = singleContainer != null ? R.id.single_container : R.id.details_container;
        SidebarFragment df = SidebarFragment.newInstance(infos.get(0).displayName, position);
        ft.replace(containerId, df, FRAG_DETAILS);
        if (singleContainer != null) {
            ft.addToBackStack(null);
        } else {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        ft.commit();
    }

    private void handleActionItemClicked(List<SubredditInfo> infos) {
        int size = infos.size();
        ContentValues[] values = new ContentValues[size];
        for (int i = 0; i < size; i++) {
            values[i] = new ContentValues(1);
            values[i].put(Subreddits.COLUMN_NAME, infos.get(i).displayName);
        }
        Provider.addMultipleSubredditsInBackground(getApplicationContext(), values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search, menu);
        searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.sr_search));
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        SidebarFragment f = getDetailsFragment();
        boolean showDetails = f != null;
        menu.findItem(R.id.menu_add).setVisible(showDetails);
        menu.findItem(R.id.menu_view).setVisible(showDetails);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                handleHome();
                return true;

            case R.id.menu_add_front_page:
                handleAddFrontPage();
                return true;

            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    private void handleHome() {
        if (singleContainer != null) {
            if (getDetailsFragment() != null) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void handleAddFrontPage() {
        ContentValues values = new ContentValues(1);
        values.put(Subreddits.COLUMN_NAME, "");
        Provider.addSubredditInBackground(getApplicationContext(), values);
    }

    public void onBackStackChanged() {
        SidebarFragment f = getDetailsFragment();
        refreshPosition(f);
        refreshActionBar(f);
    }

    private void refreshPosition(SidebarFragment detailsFrag) {
        if (singleContainer == null) {
            int position = detailsFrag != null ? detailsFrag.getPosition() : -1;
            getSubredditListFragment().setChosenPosition(position);
        }
    }

    private void refreshActionBar(SidebarFragment detailsFrag) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUERY, query);
    }

    private SubredditInfoListFragment getSubredditListFragment() {
        return (SubredditInfoListFragment) getFragmentManager().findFragmentByTag(FRAG_SUBREDDITS);
    }

    private SidebarFragment getDetailsFragment() {
        return (SidebarFragment) getFragmentManager().findFragmentByTag(FRAG_DETAILS);
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }
}
