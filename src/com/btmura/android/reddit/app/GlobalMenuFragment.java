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
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Array;

public class GlobalMenuFragment extends Fragment implements
        SearchView.OnFocusChangeListener,
        SearchView.OnQueryTextListener,
        LoaderCallbacks<AccountResult> {

    public static final String TAG = "GlobalMenuFragment";

    private static final int REQUEST_SEARCH = 0;

    public interface OnSearchQuerySubmittedListener {
        boolean onSearchQuerySubmitted(String query);
    }

    private SubredditNameHolder subredditNameHolder;
    private OnSearchQuerySubmittedListener listener;
    private MenuItem searchItem;
    private SearchView searchView;
    private boolean hasAccounts;

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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        hasAccounts = !Array.isEmpty(result.accountNames);
        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        hasAccounts = false;
        getActivity().invalidateOptionsMenu();
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
        menu.findItem(R.id.menu_submit_link).setVisible(hasAccounts);
        menu.findItem(R.id.menu_profile).setVisible(hasAccounts);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_subreddit:
                return handleAddSubreddit();

            case R.id.menu_submit_link:
                return handleSubmitLink();

            case R.id.menu_search:
                return handleSearch();

            case R.id.menu_profile:
                return handleProfile();

            case R.id.menu_settings:
                return handleSettings();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleAddSubreddit() {
        AddSubredditFragment.newInstance().show(getFragmentManager(), AddSubredditFragment.TAG);
        return true;
    }

    private boolean handleSubmitLink() {
        Intent intent = new Intent(getActivity(), SubmitLinkActivity.class);
        if (subredditNameHolder != null) {
            intent.putExtra(SubmitLinkActivity.EXTRA_SUBREDDIT, subredditNameHolder.getSubredditName());
        }
        startActivity(intent);
        return true;
    }

    public boolean handleSearch() {
        searchItem.expandActionView();
        return true;
    }

    private boolean handleProfile() {
        Intent intent = new Intent(getActivity(), UserProfileActivity.class);
        startActivity(intent);
        return true;
    }

    private boolean handleSettings() {
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
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
}
