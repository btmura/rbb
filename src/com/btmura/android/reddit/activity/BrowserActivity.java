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
import android.app.ActionBar.OnNavigationListener;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.ViewSwitcher;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.ControlFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.widget.AccountAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;

public class BrowserActivity extends AbstractBrowserActivity implements
        OnNavigationListener,
        LoaderCallbacks<Cursor> {

    public static final String TAG = "BrowserActivity";

    public static final String EXTRA_SUBREDDIT_NAME = "es";
    public static final String EXTRA_FLAGS = "ef";

    public static final int FLAG_HOME_UP_ENABLED = 0x1;
    public static final int FLAG_SUGGEST_SUBREDDIT = 0x2;

    private static final String STATE_NAVIGATION_INDEX = "i";

    private ActionBar bar;
    private ViewSwitcher switcher;
    private FilterAdapter filterAdapter;
    private View singleContainer;
    
    private AccountAdapter adapter;
    
    private boolean homeUpEnabled;
    private boolean navigationListenerDisabled;

    public BrowserActivity() {
        super(R.layout.browser);
    }

    @Override
    protected void initPrereqs(Bundle savedInstanceState) {
        int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
        homeUpEnabled = Flag.isEnabled(flags, FLAG_HOME_UP_ENABLED);
        setThingListFragmentFlags(ThingListFragment.FLAG_SINGLE_CHOICE);
        singleContainer = findViewById(R.id.single_container);
    }

    @Override
    protected boolean isSinglePane() {
        return singleContainer != null;
    }

    @Override
    protected boolean hasSubredditList() {
        return true;
    }

    @Override
    protected void initSinglePaneLayout(Bundle savedInstanceState) {
        ActionBar bar = getActionBar();        
        bar.setDisplayHomeAsUpEnabled(homeUpEnabled);
        
        
        if (getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            finish();
            Subreddit s = Subreddit.newInstance(getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME));
            Intent intent = new Intent(this, ThingListActivity.class);
            intent.putExtra(ThingListActivity.EXTRA_SUBREDDIT, s);
            intent.putExtra(ThingListActivity.EXTRA_FLAGS, ThingListActivity.FLAG_INSERT_HOME);
            startActivity(intent);
        } else if (savedInstanceState == null) {            
            bar.setDisplayShowTitleEnabled(false); 
            
            switcher = (ViewSwitcher) bar.getCustomView();
            adapter = new AccountAdapter(this);
            
            Spinner spinner = (Spinner) switcher.findViewById(R.id.spinner);        
            spinner.setAdapter(adapter);
            
            SubredditListFragment slf = SubredditListFragment.newInstance(null, -1, 0);
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance(0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
            ft.commit();
            
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    protected void initMultiPaneLayout(Bundle savedInstanceState) {
        filterAdapter = new FilterAdapter(this);

        navigationListenerDisabled = true;
        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
        bar.setListNavigationCallbacks(filterAdapter, this);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_NAVIGATION_INDEX));
        }
        navigationListenerDisabled = false;

        if (savedInstanceState == null) {
            initMultiPaneFragments();
        }
    }

    private void initMultiPaneFragments() {
        Subreddit s = null;
        if (getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
            s = Subreddit.newInstance(getIntent().getStringExtra(EXTRA_SUBREDDIT_NAME));
        }

        int filter = FilterAdapter.FILTER_HOT;

        ControlFragment cf = ControlFragment.newInstance(s, null, -1, filter);
        GlobalMenuFragment gmf = GlobalMenuFragment.newInstance(0);
        SubredditListFragment slf = SubredditListFragment.newInstance(s, -1,
                SubredditListFragment.FLAG_SINGLE_CHOICE);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(gmf, GlobalMenuFragment.TAG);
        ft.add(cf, ControlFragment.TAG);
        ft.replace(R.id.subreddit_list_container, slf, SubredditListFragment.TAG);

        if (s != null) {
            refreshActionBar(s, null, filter);
            Fragment tlf = ThingListFragment.newInstance(s, filter, getThingListFragmentFlags());
            ft.replace(R.id.thing_list_container, tlf, ThingListFragment.TAG);
        }

        ft.commit();
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (navigationListenerDisabled) {
            return true;
        }
        setFilter((int) itemId);
        return true;
    }

    @Override
    protected void refreshActionBar(Subreddit subreddit, Thing thing, int filter) {
        if (thing != null) {
            filterAdapter.setTitle(thing.assureTitle(this).title);
        } else if (subreddit != null) {
            filterAdapter.setTitle(subreddit.getTitle(this));
        } else {
            filterAdapter.setTitle(getString(R.string.app_name));
        }

        navigationListenerDisabled = true;
        bar.setSelectedNavigationItem(filter);
        navigationListenerDisabled = false;

        bar.setDisplayHomeAsUpEnabled(homeUpEnabled
                || getFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!isSinglePane()) {
            outState.putInt(STATE_NAVIGATION_INDEX, bar.getSelectedNavigationIndex());
        }
    }
    
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return AccountAdapter.createLoader(getApplicationContext());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        if (data.getCount() > 0) {
            switcher.showNext();
        }
    }
 
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}
