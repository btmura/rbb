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
import android.os.Bundle;
import android.util.Log;

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

public class LoginBrowserActivity extends Activity implements
        LoaderCallbacks<BrowserResult>,
        OnSubredditSelectedListener {
    
    public static final String TAG = "LoginBrowserActivity";
    
    private AccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.browser);
        setActionBar();
        getLoaderManager().initLoader(0, null, this);
    }

    private void setActionBar() {
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setCustomView(R.layout.browser_actionbar);

        AccountSwitcher switcher = (AccountSwitcher) bar.getCustomView();
        adapter = AccountAdapter.titleBarInstance(this);
        switcher.setAdapter(adapter);
    }
    
    public Loader<BrowserResult> onCreateLoader(int id, Bundle args) {
        return new BrowserLoader(this);
    }
    
    public void onLoadFinished(Loader<BrowserResult> loader, BrowserResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onLoadFinished (id: " + loader.getId() + ")");
        }
        adapter.swapCursor(result.accounts);        
        initFragments();
    }

    private void initFragments() { 
        if (!hasFragment(GlobalMenuFragment.TAG)) {
            SubredditListFragment slf = SubredditListFragment.newInstance(null, 0);
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance(0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
            ft.commitAllowingStateLoss();
        }
    }
        
    private boolean hasFragment(String tag) {
        return getFragmentManager().findFragmentByTag(tag) != null;
    }
    
    public void onLoaderReset(Loader<BrowserResult> loader) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onLoaderReset (id: " + loader.getId() + ")");
        }
        adapter.swapCursor(null);
    }    
    
    public void onSubredditLoaded(Subreddit subreddit) {
    }
    
    public void onSubredditSelected(Subreddit subreddit) {
    }
}
