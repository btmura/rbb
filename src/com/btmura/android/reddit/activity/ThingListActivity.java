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
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.FilterAdapter;
import com.btmura.android.reddit.browser.Subreddit;
import com.btmura.android.reddit.browser.Thing;
import com.btmura.android.reddit.browser.ThingListFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;

public class ThingListActivity extends GlobalMenuActivity implements
        ActionBar.OnNavigationListener,
        ThingListFragment.OnThingSelectedListener {

    public static final String EXTRA_SUBREDDIT = "s";
    public static final String EXTRA_INSERT_HOME_ACTIVITY = "i";
    public static final String EXTRA_SHOW_ADD_BUTTON = "a";

    private static final String STATE_FILTER = "f";

    private ActionBar bar;
    private Subreddit subreddit;
    private boolean insertHomeActivity;
    private boolean showAddButton;
    private boolean restoringState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list_activity);

        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);
        insertHomeActivity = getIntent().getBooleanExtra(EXTRA_INSERT_HOME_ACTIVITY, false);
        showAddButton = getIntent().getBooleanExtra(EXTRA_SHOW_ADD_BUTTON, false);

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
        if (insertHomeActivity) {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(BrowserActivity.EXTRA_HOME_UP_ENABLED, true);
            startActivity(intent);
        }
    }

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (restoringState) {
            restoringState = false;
            return true;
        }

        GlobalMenuFragment gmf = GlobalMenuFragment.newInstance();
        Fragment tlf = ThingListFragment.newInstance(subreddit, (int) itemId, showAddButton, false);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(gmf, GlobalMenuFragment.TAG);
        ft.replace(R.id.single_container, tlf, ThingListFragment.TAG);
        ft.commit();
        return false;
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
}
