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
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.widget.AccountAdapter;

public class LoginBrowserActivity extends Activity implements
        LoaderCallbacks<Cursor>,
        OnNavigationListener,
        OnSubredditSelectedListener {

    public static final String TAG = "LoginBrowserActivity";

    private AccountAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        adapter = new AccountAdapter(this, true);
        
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setListNavigationCallbacks(adapter, this);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        getLoaderManager().initLoader(0, null, this);

    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "onCreateLoader");
        return AccountAdapter.createLoader(getApplicationContext());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "onLoadFinished: " + data.getCount());
        adapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
    
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.v(TAG, "onNavigationItemSelected: " + itemPosition);        
        String cookie = adapter.getCookie(this, itemPosition);        
        SubredditListFragment frag = SubredditListFragment.newAccountInstance(cookie, 0);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.single_container, frag);
        ft.commit();        
        return false;
    }
    
    public void onSubredditLoaded(Subreddit subreddit) {
    }
    
    public void onSubredditSelected(Subreddit subreddit) {
    }
}
