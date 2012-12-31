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
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.provider.Provider;

public class ThingMenuFragment extends Fragment {

    public static final String TAG = "ThingMenuFragment";

    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_THING_ID = "thingId";

    private AccountNameHolder accountNameHolder;
    private boolean saveable;
    private boolean saved;

    public static ThingMenuFragment newInstance(String subreddit, String thingId) {
        Bundle args = new Bundle(2);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putString(ARG_THING_ID, thingId);
        ThingMenuFragment frag = new ThingMenuFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof AccountNameHolder) {
            accountNameHolder = (AccountNameHolder) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void setSaved(boolean saved) {
        this.saveable = true;
        this.saved = saved;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_saved).setVisible(saveable && saved);
        menu.findItem(R.id.menu_unsaved).setVisible(saveable && !saved);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about_thing_subreddit:
                handleAboutSubreddit();
                return true;

            case R.id.menu_saved:
                handleSave(SaveActions.ACTION_UNSAVE);
                return true;

            case R.id.menu_unsaved:
                handleSave(SaveActions.ACTION_SAVE);
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

    private void handleSave(int action) {
        if (accountNameHolder != null) {
            String accountName = accountNameHolder.getAccountName();
            String thingId = getArguments().getString(ARG_THING_ID);
            Provider.saveAsync(getActivity(), accountName, thingId, action);
        }
    }
}
