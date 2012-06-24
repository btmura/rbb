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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.LoaderIds;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.AccountNameHolder;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditNameHolder;
import com.btmura.android.reddit.fragment.ThingListFragment;
import com.btmura.android.reddit.fragment.ThingListFragment.OnThingSelectedListener;
import com.btmura.android.reddit.widget.AccountSpinnerAdapter;

public class ThingListActivity extends GlobalMenuActivity implements
        LoaderCallbacks<AccountResult>,
        OnNavigationListener,
        OnThingSelectedListener,
        AccountNameHolder,
        SubredditNameHolder {

    public static final String TAG = "ThingListActivity";

    public static final String EXTRA_SUBREDDIT = "s";
    public static final String EXTRA_FLAGS = "f";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_FILTER = "f";

    private ActionBar bar;
    private AccountSpinnerAdapter adapter;
    private SharedPreferences prefs;

    private Subreddit subreddit;
    private boolean insertHome;
    private int tlfFlags;
    private boolean restoringState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list);
        setInitialFragments(savedInstanceState);
        setActionBar();
        getLoaderManager().initLoader(LoaderIds.ACCOUNTS, null, this);

        subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);

        int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
        insertHome = Flag.isEnabled(flags, FLAG_INSERT_HOME);

//        FilterAdapter adapter = new FilterAdapter(this);
//        adapter.setTitle(subreddit.getTitle(this));
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar() {
        bar = getActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(false);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        adapter = new AccountSpinnerAdapter(this);
        bar.setListNavigationCallbacks(adapter, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(this);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        adapter.setAccountNames(result.accountNames);
        prefs = result.prefs;
        int index = AccountLoader.getLastAccountIndex(prefs, result.accountNames);
        bar.setSelectedNavigationItem(index);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (restoringState) {
            restoringState = false;
            return true;
        }
        replaceFragments((int) itemId);
        return true;
    }

    private void replaceFragments(int filter) {
        Fragment tlf = ThingListFragment.newInstance(subreddit, filter, tlfFlags);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.single_container, tlf, ThingListFragment.TAG);
        ft.commit();
    }

    public void onThingSelected(Thing thing, int position) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra(ThingActivity.EXTRA_THING, thing);
        startActivity(intent);
    }

    public int getThingBodyWidth() {
        return 0;
    }

    public String getAccountName() {
        return adapter.getAccountName(bar.getSelectedNavigationIndex());
    }

    public String getSubredditName() {
        return subreddit.name;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_FILTER, bar.getSelectedNavigationIndex());
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
        finish();
        if (insertHome) {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_FLAGS, BrowserActivity.FLAG_HOME_UP_ENABLED);
            startActivity(intent);
        }
    }
}
