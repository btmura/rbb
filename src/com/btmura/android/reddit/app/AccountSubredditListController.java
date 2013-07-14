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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.AccountSubredditListLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;

class AccountSubredditListController
        implements SubredditListController<AccountSubredditAdapter> {

    static final String EXTRA_ACCOUNT_RESULT = "accountResult";
    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_SELECTED_SUBREDDIT = "selectedSubreddit";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";

    private static final String EXTRA_SESSION_ID = "sessionId";

    private final Context context;
    private final AccountSubredditAdapter adapter;

    private String accountName;
    private long sessionId;

    AccountSubredditListController(Context context, Bundle args) {
        this.context = context;
        this.adapter = AccountSubredditAdapter.newAccountInstance(context,
                getAccountResultExtra(args),
                getSingleChoiceExtra(args));
        restoreInstanceState(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setAccountName(getAccountNameExtra(savedInstanceState));
        setSelectedSubreddit(getSelectedSubredditExtra(savedInstanceState));
        this.sessionId = getSessionIdExtra(savedInstanceState);
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_ACCOUNT_NAME, accountName);
        outState.putString(EXTRA_SELECTED_SUBREDDIT, getSelectedSubreddit());
        outState.putLong(EXTRA_SESSION_ID, sessionId);
    }

    // Loader related methods.

    @Override
    public boolean isLoadable() {
        return accountName != null;
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new AccountSubredditListLoader(context, accountName);
    }

    @Override
    public void swapCursor(Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    // Getters

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public AccountSubredditAdapter getAdapter() {
        return adapter;
    }

    @Override
    public String getSelectedSubreddit() {
        return adapter.getSelectedSubreddit();
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return Subreddits.hasSidebar(adapter.getName(position));
    }

    // Setters

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String setSelectedPosition(int position) {
        return adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedSubreddit(String subreddit) {
        adapter.setSelectedSubreddit(subreddit);
    }

    // Getters for extras.

    private static AccountResult getAccountResultExtra(Bundle extras) {
        return extras.getParcelable(EXTRA_ACCOUNT_RESULT);
    }

    private static String getAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACCOUNT_NAME);
    }

    private static String getSelectedSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_SUBREDDIT);
    }

    private static long getSessionIdExtra(Bundle extras) {
        return extras.getLong(EXTRA_SESSION_ID);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }
}
