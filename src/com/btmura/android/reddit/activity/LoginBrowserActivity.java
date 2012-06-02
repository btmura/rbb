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
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.BrowserLoader;
import com.btmura.android.reddit.content.BrowserLoader.BrowserResult;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.fragment.GlobalMenuFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment;
import com.btmura.android.reddit.fragment.SubredditListFragment.OnSubredditSelectedListener;
import com.btmura.android.reddit.widget.AccountAdapter;
import com.btmura.android.reddit.widget.AccountSwitcher;

public class LoginBrowserActivity extends Activity implements Debug,
        LoaderCallbacks<BrowserResult>,
        OnItemSelectedListener,
        OnSubredditSelectedListener {

    public static final String TAG = "LoginBrowserActivity";

    private AccountAdapter adapter;
    
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.enableDefaults();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        setInitialFragments(savedInstanceState);
        setActionBar();
        getLoaderManager().initLoader(0, null, this);
    }

    private void setInitialFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(GlobalMenuFragment.newInstance(0), GlobalMenuFragment.TAG);
            ft.commit();
        }
    }

    private void setActionBar() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(R.layout.browser_actionbar);

        AccountSwitcher switcher = (AccountSwitcher) bar.getCustomView();
        switcher.setOnItemSelectedListener(this);
        
        adapter = AccountAdapter.titleBarInstance(this);        
        switcher.setAdapter(adapter);
    }

    public Loader<BrowserResult> onCreateLoader(int id, Bundle args) {
        return new BrowserLoader(this);
    }

    public void onLoadFinished(Loader<BrowserResult> loader, BrowserResult result) {
        if (DEBUG_LOADERS) {
            Log.d(TAG, "onLoadFinished (id " + loader.getId() + ")");
        }
        adapter.swapCursor(result.accounts);
        prefs = result.prefs;
    }

    public void onLoaderReset(Loader<BrowserResult> loader) {
        if (DEBUG_LOADERS) {
            Log.d(TAG, "onLoaderReset (id " + loader.getId() + ")");
        }
        adapter.swapCursor(null);
    }
    
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (DEBUG_ACTIVITY) {
            Log.d(TAG, "onItemSelected (position " + position + ")");
        }        
        
        String cookie = adapter.getCookie(position);        
        SubredditListFragment slf = SubredditListFragment.newInstance(null, cookie, 0);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
        ft.commit();
        
        String lastLogin = adapter.getLogin(position);
        saveLastLoginPreference(lastLogin);        
    }
    
    private void saveLastLoginPreference(String lastLogin) {
        Editor editor = prefs.edit();
        editor.putString("lastLogin", lastLogin);
        editor.apply();
    }
    
    public void onNothingSelected(AdapterView<?> adapterView) {
        if (DEBUG_ACTIVITY) {
            Log.d(TAG, "onNothingSelected");
        }
    }
    
    public void onSubredditLoaded(Subreddit subreddit) {
    }

    public void onSubredditSelected(Subreddit subreddit) {
    }
}
