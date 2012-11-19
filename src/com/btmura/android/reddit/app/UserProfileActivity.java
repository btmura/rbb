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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;

/**
 * {@link Activity} for viewing a user's profile.
 */
public class UserProfileActivity extends Activity {

    /** Required string extra that is the user's name. */
    public static final String EXTRA_USER = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);
        setupActionBar();
        setupFragments(savedInstanceState);
    }

    private void setupActionBar() {
        ActionBar bar = getActionBar();
        bar.setTitle(getUserName());
        bar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment frag = ThingListFragment.newInstance(Subreddits.ACCOUNT_NONE, null, 0, null,
                    getUserName(), 0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.thing_list_container, frag, ThingListFragment.TAG);
            ft.commit();
        }
    }

    private String getUserName() {
        String user = getIntent().getStringExtra(EXTRA_USER);
        // TODO: Remove this fallback once things become more stable.
        if (user == null) {
            user = "rbbtest1";
        }
        return user;
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
}
