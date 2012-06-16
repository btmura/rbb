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

package com.btmura.android.reddit.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.activity.SearchActivity;
import com.btmura.android.reddit.provider.Provider;

public class GlobalMenuFragment extends Fragment implements
        SearchView.OnFocusChangeListener,
        SearchView.OnQueryTextListener {

    public static final String TAG = "GlobalMenuFragment";

    private static final int REQUEST_SEARCH = 0;

    private OnQueryTextListener queryListener;
    private MenuItem searchItem;
    private SearchView searchView;

    public static GlobalMenuFragment newInstance(int flags) {
        return new GlobalMenuFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnQueryTextListener) {
            queryListener = (OnQueryTextListener) activity;
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SEARCH:
                searchItem.collapseActionView();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                handleSearch();
                return true;

            case R.id.menu_accounts:
                handleAccounts();
                return true;

            case R.id.menu_add_subreddit:
                handleAddSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void handleSearch() {
        searchItem.expandActionView();
    }

    private void handleAccounts() {
        Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
        intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {Provider.AUTHORITY});
        startActivity(intent);
    }

    private void handleAddSubreddit() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(AddSubredditFragment.newInstance(), AddSubredditFragment.TAG);
        ft.commit();
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            searchItem.collapseActionView();
        }
    }

    public boolean onQueryTextSubmit(String query) {
        if (queryListener != null) {
            searchItem.collapseActionView();
            return queryListener.onQueryTextSubmit(query);
        } else {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(SearchActivity.EXTRA_QUERY, query);
            startActivityForResult(intent, REQUEST_SEARCH);
            return true;
        }
    }

    public boolean onQueryTextChange(String newText) {
        if (queryListener != null) {
            return queryListener.onQueryTextChange(newText);
        } else {
            return false;
        }
    }
}
