/*
 * Copyright (C) 2013 Brian Muramatsu
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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.BrowserActivity.OnFilterSelectedListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.AccountPlaceAdapter;
import com.btmura.android.reddit.widget.AccountPlaceAdapter.OnPlaceSelectedListener;
import com.btmura.android.reddit.widget.AccountResultAdapter;
import com.btmura.android.reddit.widget.AccountResultAdapter.Item;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.MergeAdapter;

public class NavigationFragment extends ListFragment implements LoaderCallbacks<AccountResult>,
        OnPlaceSelectedListener, OnFilterSelectedListener {

    public static final String TAG = "NavigationFragment";

    private static final int ADAPTER_ACCOUNTS = 0;
    private static final int ADAPTER_PLACES = 1;
    private static final int ADAPTER_SUBREDDITS = 2;

    private static final int LOADER_ACCOUNTS = 0;
    private static final int LOADER_SUBREDDITS = 1;

    public interface OnNavigationEventListener {
        void onSubredditSelected(String accountName, String subreddit, int filter);

        void onProfileSelected(String accountName, int filter);

        void onSavedSelected(String accountName, int filter);

        void onMessagesSelected(String accountName, int filter);
    }

    private final AccountSubredditLoaderCallbacks subredditLoaderCallbacks =
            new AccountSubredditLoaderCallbacks();

    private OnNavigationEventListener listener;
    private AccountResultAdapter accountAdapter;
    private AccountPlaceAdapter placesAdapter;
    private AccountSubredditAdapter subredditAdapter;
    private MergeAdapter mergeAdapter;

    private String accountName;
    private int place;
    private String subreddit;
    private int filter;

    public static NavigationFragment newInstance() {
        return new NavigationFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnNavigationEventListener) {
            listener = (OnNavigationEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountAdapter = new AccountResultAdapter(getActivity());
        placesAdapter = new AccountPlaceAdapter(getActivity(), this);
        subredditAdapter = AccountSubredditAdapter.newAccountInstance(getActivity());
        mergeAdapter = new MergeAdapter(accountAdapter, placesAdapter, subredditAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) v.findViewById(android.R.id.list);
        listView.setVerticalScrollBarEnabled(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(mergeAdapter);
        setListShown(false);
        getLoaderManager().initLoader(LOADER_ACCOUNTS, null, this);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), true, true);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        accountAdapter.setAccountResult(result);
        setListShown(true);

        String accountName = result.getLastAccount(getActivity());
        boolean restart = !Objects.equals(this.accountName, accountName);
        selectAccount(accountName, restart);
    }

    private void selectAccount(String accountName, boolean restartLoader) {
        this.accountName = accountName;
        AccountPrefs.setLastAccount(getActivity(), accountName);
        accountAdapter.setSelectedAccountName(accountName);
        placesAdapter.setAccountPlaces(accountAdapter.getCount() > 1,
                AccountUtils.isAccount(accountName));
        refreshSubredditLoader(accountName, restartLoader);

        int place = AccountUtils.isAccount(accountName)
                ? AccountPrefs.getLastPlace(getActivity(), PLACE_SUBREDDIT)
                : PLACE_SUBREDDIT;
        selectPlaceWithDefaultFilter(place);
    }

    private void refreshSubredditLoader(String accountName, boolean restartLoader) {
        if (restartLoader) {
            getLoaderManager().restartLoader(LOADER_SUBREDDITS, null, subredditLoaderCallbacks);
        } else {
            getLoaderManager().initLoader(LOADER_SUBREDDITS, null, subredditLoaderCallbacks);
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int adapterIndex = mergeAdapter.getAdapterIndex(position);
        int adapterPosition = mergeAdapter.getAdapterPosition(position);
        switch (adapterIndex) {
            case ADAPTER_ACCOUNTS:
                handleAccountClick(adapterPosition);
                break;

            case ADAPTER_PLACES:
                throw new IllegalStateException();

            case ADAPTER_SUBREDDITS:
                handleSubredditClick(adapterPosition);
                break;
        }
    }

    private void handleAccountClick(int position) {
        Item item = accountAdapter.getItem(position);
        switch (item.getType()) {
            case Item.TYPE_ACCOUNT_NAME:
                selectAccount(item.getAccountName(), true);
                break;
        }
    }

    private void selectPlaceWithDefaultFilter(int place) {
        switch (place) {
            case PLACE_SUBREDDIT:
                String subreddit = AccountPrefs.getLastSubreddit(getActivity(), accountName);
                int filter = AccountPrefs.getLastSubredditFilter(getActivity(),
                        FilterAdapter.SUBREDDIT_HOT);
                selectPlace(place, subreddit, filter);
                break;

            case PLACE_PROFILE:
                selectPlace(place, null, FilterAdapter.PROFILE_OVERVIEW);
                break;

            case PLACE_SAVED:
                selectPlace(place, null, FilterAdapter.PROFILE_SAVED);
                break;

            case PLACE_MESSAGES:
                selectPlace(place, null, FilterAdapter.MESSAGE_INBOX);
                break;
        }
    }

    private void selectPlace(int place, String subreddit, int filter) {
        this.place = place;
        this.subreddit = subreddit;
        this.filter = filter;
        placesAdapter.setSelectedPlace(place);
        AccountPrefs.setLastPlace(getActivity(), place);
        switch (place) {
            case PLACE_SUBREDDIT:
                subredditAdapter.setSelectedSubreddit(subreddit);
                AccountPrefs.setLastSubreddit(getActivity(), accountName, subreddit);
                AccountPrefs.setLastSubredditFilter(getActivity(), filter);
                if (listener != null) {
                    listener.onSubredditSelected(accountName, subreddit, filter);
                }
                break;

            case PLACE_PROFILE:
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onProfileSelected(accountName, filter);
                }
                break;

            case PLACE_SAVED:
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onSavedSelected(accountName, filter);
                }
                break;

            case PLACE_MESSAGES:
                subredditAdapter.setSelectedSubreddit(null);
                if (listener != null) {
                    listener.onMessagesSelected(accountName, filter);
                }
                break;
        }
    }

    private void handleSubredditClick(int position) {
        String subreddit = subredditAdapter.getName(position);
        selectPlace(PLACE_SUBREDDIT, subreddit, filter);
    }

    @Override
    public void onPlaceSelected(int place) {
        selectPlaceWithDefaultFilter(place);
    }

    @Override
    public void onFilterSelected(int newFilter) {
        if (filter != newFilter) {
            selectPlace(place, subreddit, newFilter);
        }
    }

    class AccountSubredditLoaderCallbacks implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new AccountSubredditListLoader(getActivity(), accountName);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            subredditAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            subredditAdapter.swapCursor(null);
        }
    }
}
