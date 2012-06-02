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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
    
    private static final int MESSAGE_INIT_FRAGMENTS = 0;
    
    private AccountAdapter adapter;
    
    private SharedPreferences prefs;

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
        Log.v(TAG, "onLoadFinished: " + result.accounts.getCount());
        adapter.swapCursor(result.accounts);
        prefs = result.prefs;        
        handler.obtainMessage(MESSAGE_INIT_FRAGMENTS).sendToTarget();
    }
    
    public void onLoaderReset(Loader<BrowserResult> loader) {
        adapter.swapCursor(null);
    }
    
    final Handler handler = new Handler() {        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT_FRAGMENTS:
                    initFragments();
                    break;
                    
                default:
                    throw new IllegalStateException();
            }
        }
    };
    
    void initFragments() { 
        if (!hasFragment(GlobalMenuFragment.TAG)) {
            SubredditListFragment slf = SubredditListFragment.newInstance(null, 0);
            GlobalMenuFragment gmf = GlobalMenuFragment.newInstance(0);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(gmf, GlobalMenuFragment.TAG);
            ft.replace(R.id.single_container, slf, SubredditListFragment.TAG);
            ft.commit();
        }
    }
    
    public void onSubredditLoaded(Subreddit subreddit) {
    }
    
    public void onSubredditSelected(Subreddit subreddit) {
    }
    
    private boolean hasFragment(String tag) {
        return getFragmentManager().findFragmentByTag(tag) != null;
    }
}
