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
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.fragment.AddSubredditFragment;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.ThingListFragment;

public class ThingListActivity extends GlobalMenuActivity implements
        ActionBar.OnNavigationListener,
        ThingListFragment.OnThingSelectedListener,
        AddSubredditFragment.SubredditNameHolder {

    public static final String TAG = "ThingListActivity";

    public static final String EXTRA_SUBREDDIT = "es";
    public static final String EXTRA_FLAGS = "ef";

    public static final int FLAG_INSERT_HOME = 0x1;

    private static final String STATE_FILTER = "f";

    private ActionBar bar;
    private Subreddit subreddit;
    private boolean insertHome;
    private int tlfFlags;
    private boolean restoringState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thing_list);

        bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        subreddit = getIntent().getParcelableExtra(EXTRA_SUBREDDIT);

        int flags = getIntent().getIntExtra(EXTRA_FLAGS, 0);
        insertHome = Flag.isEnabled(flags, FLAG_INSERT_HOME);

        FilterAdapter adapter = new FilterAdapter(this);
        adapter.setTitle(subreddit.getTitle(this));
        bar.setListNavigationCallbacks(adapter, this);
        if (savedInstanceState != null) {
            restoringState = true;
            bar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_FILTER));
        }

        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.commit();
        }
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
