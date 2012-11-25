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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.ThingBundle;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_THING_BUNDLE = "thingBundle";

    private Bundle thingBundle;

    public static ThingMenuFragment newInstance(Bundle thingBundle) {
        Bundle args = new Bundle(1);
        args.putBundle(ARG_THING_BUNDLE, thingBundle);
        ThingMenuFragment f = new ThingMenuFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thingBundle = getArguments().getBundle(ARG_THING_BUNDLE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        hideExtraViewSidebarItems(menu);
    }

    private void hideExtraViewSidebarItems(Menu menu) {
        MenuItem thingSidebarItem = menu.findItem(R.id.menu_view_thing_sidebar);
        MenuItem subredditSidebarItem = menu.findItem(R.id.menu_view_subreddit_sidebar);
        if (thingSidebarItem != null && subredditSidebarItem != null) {
            thingSidebarItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_thing_sidebar:
                handleViewSidebar();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleViewSidebar() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, ThingBundle.getSubreddit(thingBundle));
        startActivity(intent);
    }
}
