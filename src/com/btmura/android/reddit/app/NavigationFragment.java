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

import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;
import com.btmura.android.reddit.widget.MergeAdapter;
import com.btmura.android.reddit.widget.NavigationAdapter;
import com.btmura.android.reddit.widget.NavigationAdapter.Item;

public class NavigationFragment extends ListFragment
        implements LoaderCallbacks<AccountResult> {

    public interface OnNavigationEventListener {

        void onSubredditSelected(String accountName, String subreddit);
    }

    private static final int[] ATTRIBUTES = {
            android.R.attr.windowBackground,
    };

    private static final String LOADER_ARG_ACCOUNT_NAME = "accountName";

    private final AccountSubredditLoaderCallbacks subredditLoaderCallbacks =
            new AccountSubredditLoaderCallbacks();

    private OnNavigationEventListener listener;
    private NavigationAdapter accountAdapter;
    private AccountSubredditAdapter subredditAdapter;
    private MergeAdapter mergeAdapter;

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
        subredditAdapter = AccountSubredditAdapter.newAccountInstance(getActivity());

        mergeAdapter = new MergeAdapter(2);
        mergeAdapter.add(accountAdapter);
        mergeAdapter.add(subredditAdapter);
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
        getLoaderManager().initLoader(0, null, this);
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
        Bundle args = new Bundle(1);
        args.putString(LOADER_ARG_ACCOUNT_NAME, accountName);
        getLoaderManager().initLoader(1, args, subredditLoaderCallbacks);
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int adapterIndex = mergeAdapter.getAdapterIndex(position);
        int adapterPosition = mergeAdapter.getAdapterPosition(position);
        switch (adapterIndex) {
            case 0:
                handleAccountAdapterClick(adapterPosition);
                break;
        }
    }

    private void handleAccountAdapterClick(int position) {
        Item item = accountAdapter.getItem(position);
        switch (item.getType()) {
            case Item.TYPE_ACCOUNT_NAME:
                Bundle args = new Bundle(1);
                args.putString(LOADER_ARG_ACCOUNT_NAME, item.getAccountName());
                getLoaderManager().restartLoader(1, args, subredditLoaderCallbacks);
                break;
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
