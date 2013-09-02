/*
 * Copyright (C) 2013 Brian Muramatsu
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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;

class ThingTableMenuController implements MenuController {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final String accountName;
    private final String query;
    private final SubredditHolder subredditNameHolder;
    private final ThingBundleHolder thingBundleHolder;
    private final Refreshable refreshable;

    ThingTableMenuController(Context context,
            FragmentManager fragmentManager,
            String accountName,
            String query,
            SubredditHolder subredditNameHolder,
            ThingBundleHolder thingBundleHolder,
            Refreshable refreshable) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.accountName = accountName;
        this.query = query;
        this.subredditNameHolder = subredditNameHolder;
        this.thingBundleHolder = thingBundleHolder;
        this.refreshable = refreshable;
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        // No state to restore
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        // No state to save
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.thing_list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        String subreddit = getSubreddit();

        boolean isQuery = !TextUtils.isEmpty(query);
        boolean hasAccount = AccountUtils.isAccount(accountName);
        boolean hasSubreddit = subreddit != null;
        boolean hasThing = thingBundleHolder != null && thingBundleHolder.getThingBundle() != null;
        boolean hasSidebar = Subreddits.hasSidebar(subreddit);

        boolean showRefresh = !isQuery && hasSubreddit && !hasThing;
        boolean showAddSubreddit = showRefresh;
        boolean showNewPost = showRefresh && hasAccount;
        boolean showSubreddit = showRefresh && hasSidebar;

        menu.findItem(R.id.menu_new_post).setVisible(showNewPost);
        menu.findItem(R.id.menu_refresh).setVisible(showAddSubreddit);
        menu.findItem(R.id.menu_add_subreddit).setVisible(showAddSubreddit);

        MenuItem subredditItem = menu.findItem(R.id.menu_subreddit);
        subredditItem.setVisible(showSubreddit);
        if (showSubreddit) {
            subredditItem.setTitle(MenuHelper.getSubredditTitle(context, subreddit));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                handleRefresh();
                return true;

            case R.id.menu_add_subreddit:
                handleAddSubreddit();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit();
                return true;
        }
        return false;
    }

    private void handleRefresh() {
        refreshable.refresh();
    }

    private void handleAddSubreddit() {
        MenuHelper.showAddSubredditDialog(fragmentManager, getSubreddit());
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(context, getSubreddit());
    }

    private String getSubreddit() {
        return subredditNameHolder.getSubreddit();
    }
}
