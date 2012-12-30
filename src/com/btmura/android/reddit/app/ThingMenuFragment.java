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

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_SAVED = "saved";

    public static ThingMenuFragment newInstance(String subreddit, boolean saved) {
        Bundle args = new Bundle(1);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putBoolean(ARG_SAVED, saved);
        ThingMenuFragment frag = new ThingMenuFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        boolean saved = getArguments().getBoolean(ARG_SAVED);
        menu.findItem(R.id.menu_save).setVisible(!saved);
        menu.findItem(R.id.menu_unsave).setVisible(saved);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about_thing_subreddit:
                handleAboutSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAboutSubreddit() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, getArguments().getString(ARG_SUBREDDIT));
        startActivity(intent);
    }
}
