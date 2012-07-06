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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.activity.SearchActivity;
import com.btmura.android.reddit.activity.SubmitLinkActivity;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.provider.SubredditProvider;

public class GlobalMenuFragment extends Fragment implements
        SearchView.OnFocusChangeListener,
        SearchView.OnQueryTextListener {

    public static final String TAG = "GlobalMenuFragment";

    public static final int FLAG_SHOW_MANAGE_ACCOUNTS = 0x1;

    private static final String ARG_FLAGS = "f";

    private static final int REQUEST_SEARCH = 0;

    public interface OnSearchQuerySubmittedListener {
        boolean onSearchQuerySubmitted(String query);
    }

    private OnSearchQuerySubmittedListener listener;
    private MenuItem searchItem;
    private SearchView searchView;

    public static GlobalMenuFragment newInstance(int flags) {
        Bundle args = new Bundle(1);
        args.putInt(ARG_FLAGS, flags);
        GlobalMenuFragment f = new GlobalMenuFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_manage_accounts).setVisible(showManageAccounts());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                handleSearch();
                return true;

            case R.id.menu_manage_accounts:
                handleAccounts();
                return true;

            case R.id.menu_submit_link:
                handleSubmitLink();
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
        intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {SubredditProvider.AUTHORITY});
        startActivity(intent);
    }

    private void handleSubmitLink() {
        Intent intent = new Intent(getActivity(), SubmitLinkActivity.class);
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
            intent.putExtra(SearchActivity.EXTRA_QUERY, query);
            startActivityForResult(intent, REQUEST_SEARCH);
        }
        return true;
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

    private boolean showManageAccounts() {
        return Flag.isEnabled(getFlags(), FLAG_SHOW_MANAGE_ACCOUNTS);
    }

    private int getFlags() {
        return getArguments().getInt(ARG_FLAGS);
    }
}
