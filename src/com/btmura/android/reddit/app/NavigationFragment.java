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
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.btmura.android.reddit.app.BrowserActivity.OnFilterSelectedListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;
import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.MergeAdapter;
import com.btmura.android.reddit.widget.NavigationAdapter;
import com.btmura.android.reddit.widget.NavigationAdapter.Item;

public class NavigationFragment extends ListFragment
        implements LoaderCallbacks<AccountResult>, OnFilterSelectedListener {

    public static final String TAG = "NavigationFragment";

    private static final int[] ATTRIBUTES = {
            android.R.attr.windowBackground,
    };

    private static final int ADAPTER_ACCOUNTS = 0;
    private static final int ADAPTER_PLACES = 1;
    private static final int ADAPTER_SUBREDDITS = 2;

    private static final String LOADER_ARG_ACCOUNT_NAME = "accountName";

    private static final int LOADER_ACCOUNTS = 0;
    private static final int LOADER_SUBREDDITS = 1;

    private static final boolean LOADER_INIT = false;
    private static final boolean LOADER_RESTART = true;

    public interface OnNavigationEventListener {
        void onSubredditSelected(String accountName, String subreddit, int filter);

        void onProfileSelected(String accountName, int filter);

        void onSavedSelected(String accountName, int filter);

        void onMessagesSelected(String accountName, int filter);
    }

    private final AccountSubredditLoaderCallbacks subredditLoaderCallbacks =
            new AccountSubredditLoaderCallbacks();

    private OnNavigationEventListener listener;
    private NavigationAdapter accountAdapter;
    private NavigationAdapter placesAdapter;
    private AccountSubredditAdapter subredditAdapter;
    private MergeAdapter mergeAdapter;

    private String accountName;
    private int place;
    private String subreddit;
    private int filter;

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
        accountAdapter = new NavigationAdapter(getActivity());
        placesAdapter = new NavigationAdapter(getActivity());
        subredditAdapter = AccountSubredditAdapter.newAccountInstance(getActivity());
        mergeAdapter = new MergeAdapter(accountAdapter, placesAdapter, subredditAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundResource(getBackgroundResource());
        return view;
    }

    // TODO(btmura): Replace this with a proper resource instead of window background.
    private int getBackgroundResource() {
        TypedArray array = getActivity().getTheme().obtainStyledAttributes(ATTRIBUTES);
        int backgroundResId = array.getResourceId(0, 0);
        array.recycle();
        return backgroundResId;
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
        selectAccount(accountName, LOADER_INIT);
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
                handlePlaceClick(adapterPosition);
                break;

            case ADAPTER_SUBREDDITS:
                handleSubredditClick(adapterPosition);
                break;
        }
    }

    private void handleAccountClick(int position) {
        Item item = accountAdapter.getItem(position);
        switch (item.getType()) {
            case Item.TYPE_ACCOUNT_NAME:
                selectAccount(item.getAccountName(), LOADER_RESTART);
                break;
        }
    }

    private void handlePlaceClick(int position) {
        Item item = placesAdapter.getItem(position);
        switch (item.getType()) {
            case Item.TYPE_PLACE:
                selectPlace(item.getPlace());
                break;
        }
    }

    private void handleSubredditClick(int position) {
        String subreddit = subredditAdapter.getName(position);
        selectSubreddit(subreddit, filter);
    }

    private void selectAccount(String accountName, boolean restartLoader) {
        this.accountName = accountName;
        AccountPrefs.setLastAccount(getActivity(), accountName);
        placesAdapter.setAccountPlaces(accountName);
        refreshSubredditLoader(accountName, restartLoader);

        String subreddit = AccountPrefs.getLastSubreddit(getActivity(), accountName);
        int filter = AccountPrefs.getLastSubredditFilter(getActivity(),
                FilterAdapter.SUBREDDIT_HOT);
        selectSubreddit(subreddit, filter);
    }

    private void refreshSubredditLoader(String accountName, boolean restartLoader) {
        Bundle args = new Bundle(1);
        args.putString(LOADER_ARG_ACCOUNT_NAME, accountName);
        if (restartLoader) {
            getLoaderManager().restartLoader(LOADER_SUBREDDITS, args, subredditLoaderCallbacks);
        } else {
            getLoaderManager().initLoader(LOADER_SUBREDDITS, args, subredditLoaderCallbacks);
        }
    }

    private void selectPlace(int place) {
        this.place = place;
        if (listener != null) {
            switch (place) {
                case Item.PLACE_PROFILE:
                    listener.onProfileSelected(accountName, FilterAdapter.PROFILE_OVERVIEW);
                    break;

                case Item.PLACE_SAVED:
                    listener.onSavedSelected(accountName, FilterAdapter.PROFILE_SAVED);
                    break;

                case Item.PLACE_MESSAGES:
                    listener.onMessagesSelected(accountName, FilterAdapter.MESSAGE_INBOX);
                    break;
            }
        }
    }

    private void selectSubreddit(String subreddit, int filter) {
        this.place = Item.PLACE_SUBREDDIT;

        this.subreddit = subreddit;
        subredditAdapter.setSelectedSubreddit(subreddit);
        AccountPrefs.setLastSubreddit(getActivity(), accountName, subreddit);

        this.filter = filter;
        AccountPrefs.setLastSubredditFilter(getActivity(), filter);

        if (listener != null) {
            listener.onSubredditSelected(accountName, subreddit, filter);
        }
    }

    @Override
    public void onFilterSelected(int newFilter) {
        if (place == Item.PLACE_SUBREDDIT && filter != newFilter) {
            selectSubreddit(subreddit, newFilter);
        }
    }

    class AccountSubredditLoaderCallbacks implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String accountName = args.getString(LOADER_ARG_ACCOUNT_NAME);
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
