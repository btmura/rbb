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

package com.btmura.android.reddit.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_BUNDLE = "thingBundle";

    private static final String STATE_THING_BUNDLE = ARG_THING_BUNDLE;

    interface ThingMenuListenerHolder {
        void addThingMenuListener(ThingMenuListener listener);

        void removeThingMenuListener(ThingMenuListener listener);
    }

    /**
     * Interface that activities should implement to be aware of when the user selects menu items
     * that ThingMenuFragment cannot handle on its own.
     */
    interface ThingMenuListener {

        void onCreateThingOptionsMenu(Menu menu);

        void onPrepareThingOptionsMenu(Menu menu, int pageType);

        void onThingOptionsItemSelected(MenuItem item, int pageType);
    }

    private ThingMenuListener listener;
    private ThingPagerHolder thingPagerHolder;

    private Bundle thingBundle;
    private MenuItem userItem;
    private MenuItem subredditItem;

    public static ThingMenuFragment newInstance(String accountName, Bundle thingBundle) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);
        ThingMenuFragment frag = new ThingMenuFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ThingMenuListener) {
            listener = (ThingMenuListener) activity;
        }
        if (activity instanceof ThingPagerHolder) {
            thingPagerHolder = (ThingPagerHolder) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            thingBundle = getArguments().getBundle(ARG_THING_BUNDLE);
        } else {
            thingBundle = savedInstanceState.getBundle(STATE_THING_BUNDLE);
        }
        setHasOptionsMenu(true);
    }

    public void setThingBundle(Bundle thingBundle) {
        this.thingBundle = thingBundle;
        refreshMenuItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu_menu, menu);
        userItem = menu.findItem(R.id.menu_user);
        subredditItem = menu.findItem(R.id.menu_thing_subreddit);

        if (listener != null) {
            listener.onCreateThingOptionsMenu(menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (listener != null) {
            int pageType = getCurrentPageType();
            if (pageType != -1) {
                listener.onPrepareThingOptionsMenu(menu, getCurrentPageType());
            }
        }
        refreshMenuItems();
    }

    private void refreshMenuItems() {
        refreshUserItems();
        refreshSubredditItem();
    }

    private void refreshUserItems() {
        if (userItem != null) {
            userItem.setVisible(MenuHelper.isUserItemVisible(getUser()));
            if (userItem.isVisible()) {
                userItem.setTitle(MenuHelper.getUserTitle(getActivity(), getUser()));
            }
        }
    }

    private void refreshSubredditItem() {
        if (subredditItem != null) {
            boolean visible = hasSubreddit();
            subredditItem.setVisible(visible);
            if (visible) {
                subredditItem.setTitle(MenuHelper.getSubredditTitle(getActivity(), getSubreddit()));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_link:
            case R.id.menu_comments:
            case R.id.menu_saved:
            case R.id.menu_unsaved:
            case R.id.menu_new_comment:
            case R.id.menu_open:
            case R.id.menu_copy_url:
                listener.onThingOptionsItemSelected(item, getCurrentPageType());
                return true;

            case R.id.menu_user:
                handleUser();
                return true;

            case R.id.menu_thing_subreddit:
                handleSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleUser() {
        MenuHelper.startProfileActivity(getActivity(), getUser(), -1);
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), getSubreddit());
    }

    private boolean hasSubreddit() {
        return Subreddits.hasSidebar(getSubreddit());
    }

    private String getSubreddit() {
        return ThingBundle.getSubreddit(thingBundle);
    }

    private String getUser() {
        return ThingBundle.getAuthor(thingBundle);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_THING_BUNDLE, thingBundle);
    }

    private int getCurrentPageType() {
        if (thingPagerHolder != null) {
            ViewPager pager = thingPagerHolder.getThingPager();
            ThingPagerAdapter adapter = (ThingPagerAdapter) pager.getAdapter();
            if (adapter != null) {
                return adapter.getPageType(pager.getCurrentItem());
            }
        }
        return -1;
    }
}
