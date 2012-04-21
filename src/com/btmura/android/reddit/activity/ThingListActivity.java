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
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.FilterAdapter;
import com.btmura.android.reddit.browser.Subreddit;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.browser.ThingListFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;

public class ThingListActivity extends Activity implements
        ActionBar.OnNavigationListener,
        ThingListFragment.OnThingSelectedListener {

    public static final String EXTRA_SUBREDDIT = "s";

    private static final String STATE_FILTER = "f";

    private ActionBar bar;
    private Subreddit subreddit;
    private boolean restoringState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list_activity);

        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);

        FilterAdapter adapter = new FilterAdapter(this);
        adapter.setTitle(subreddit.getTitle(this));
        bar.setListNavigationCallbacks(adapter, this);

        if (savedInstanceState != null) {
            restoringState = true;
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_FILTER));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_FILTER, bar.getSelectedNavigationIndex());
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH:
                getGlobalMenuFragment().handleSearch();
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (restoringState) {
            restoringState = false;
            return true;
        }

        GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
        ThingListFragment tlf = ThingListFragment.newInstance(subreddit, (int) itemId, false);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.single_container, tlf, ThingListFragment.TAG);
        ft.add(gmf, GlobalMenuFragment.TAG);
        ft.commit();
        return false;
    }

    public void onThingSelected(Thing thing, int position) {
        Intent intent = new Intent(this, ThingActivity.class);
        intent.putExtra(ThingActivity.EXTRA_THING, thing);
        startActivity(intent);
    }

    public int getThingBodyWidth() {
        return 0;
    }

    private GlobalMenuFragment getGlobalMenuFragment() {
        return (GlobalMenuFragment) getFragmentManager().findFragmentByTag(GlobalMenuFragment.TAG);
    }
}
