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
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;

public class GlobalMenuFragment extends Fragment implements OnFocusChangeListener,
        OnQueryTextListener {

    public static final String TAG = "GlobalMenuFragment";

    private static final int REQUEST_SEARCH = 0;

    public interface OnSearchQuerySubmittedListener {
        boolean onSearchQuerySubmitted(String query);
    }

    private SubredditNameHolder subredditNameHolder;
    private OnSearchQuerySubmittedListener listener;
    private SearchView searchView;
    private MenuItem searchItem;

    public static GlobalMenuFragment newInstance() {
        return new GlobalMenuFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SubredditNameHolder) {
            subredditNameHolder = (SubredditNameHolder) activity;
        }
        if (activity instanceof OnSearchQuerySubmittedListener) {
            listener = (OnSearchQuerySubmittedListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.global_menu, menu);
        searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextFocusChangeListener(this);
        searchView.setOnQueryTextListener(this);
        menu.findItem(R.id.menu_debug).setVisible(BuildConfig.DEBUG);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // GlobalMenuFragment handles more menu items than it presents for
        // convenience. Some other items have complicated visibility logic, so
        // they aren't inflated in this fragment.
        switch (item.getItemId()) {
            case R.id.menu_browser_new_post:
            case R.id.menu_new_post:
                handleNewPost();
                return true;

            case R.id.menu_browser_add_subreddit:
            case R.id.menu_add_subreddit:
                handleAddSubreddit();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit();
                return true;

            case R.id.menu_search:
                handleSearch();
                return true;

            case R.id.menu_debug:
                handleDebug();
                return true;

            case R.id.menu_help:
                handleHelp();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleNewPost() {
        MenuHelper.startComposeActivity(getActivity(), ComposeActivity.DEFAULT_TYPE_SET,
                subredditNameHolder.getSubredditName(), null, null, null, null, false);
    }

    private void handleAddSubreddit() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(),
                subredditNameHolder.getSubredditName());
    }

    private void handleSubreddit() {
        MenuHelper.startSidebarActivity(getActivity(), subredditNameHolder.getSubredditName());
    }

    public void handleSearch() {
        searchItem.expandActionView();
    }

    private void handleDebug() {
        MenuHelper.startContentBrowserActivity(getActivity());
    }

    private void handleHelp() {
        MenuHelper.openUrl(getActivity(), "http://btmura.github.com/rbb");
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            searchItem.collapseActionView();
        }
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public boolean onQueryTextSubmit(String query) {
        if (listener != null && listener.onSearchQuerySubmitted(query)) {
            searchItem.collapseActionView();
        } else {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(SearchActivity.EXTRA_SUBREDDIT,
                    subredditNameHolder.getSubredditName());
            intent.putExtra(SearchActivity.EXTRA_QUERY, query);
            startActivityForResult(intent, REQUEST_SEARCH);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SEARCH:
                if (searchItem != null) {
                    searchItem.collapseActionView();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
