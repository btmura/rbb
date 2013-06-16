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
import android.text.TextUtils;

import com.btmura.android.reddit.content.SubredditSearchLoader;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditSearchAdapter;

class SubredditSearchController implements SubredditListController {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_SELECTED_SUBREDDIT = "selectedSubreddit";
    static final String EXTRA_QUERY = "query";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";

    private static final String EXTRA_SESSION_ID = "sessionId";
    private static final String EXTRA_ACTION_ACCOUNT_NAME = "actionAccountName";

    private final Context context;
    private final SubredditAdapter adapter;

    private String accountName;
    private String query;
    private long sessionId;
    private String actionAccountName;

    SubredditSearchController(Context context, Bundle args) {
        this.context = context;
        this.adapter = new SubredditSearchAdapter(context, getQueryExtra(args),
                getSingleChoiceExtra(args));
        this.accountName = getAccountNameExtra(args);
        this.query = getQueryExtra(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        this.accountName = getAccountNameExtra(savedInstanceState);
        setSelectedSubreddit(getSelectedSubredditExtra(savedInstanceState));
        this.query = getQueryExtra(savedInstanceState);
        this.sessionId = getSessionIdExtra(savedInstanceState);
        this.actionAccountName = getActionAccountNameExtra(savedInstanceState);
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_ACCOUNT_NAME, accountName);
        outState.putString(EXTRA_SELECTED_SUBREDDIT, getSelectedSubreddit());
        outState.putString(EXTRA_QUERY, query);
        outState.putLong(EXTRA_SESSION_ID, sessionId);
        outState.putString(EXTRA_ACTION_ACCOUNT_NAME, actionAccountName);
    }

    // Loader related methods.

    @Override
    public boolean isLoadable() {
        return accountName != null && !TextUtils.isEmpty(query);
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new SubredditSearchLoader(context, accountName, query, sessionId);
    }

    @Override
    public boolean swapCursor(Cursor cursor) {
        if (adapter.getCursor() != cursor) {
            adapter.swapCursor(cursor);
            if (cursor != null && cursor.getExtras() != null) {
                Bundle extras = cursor.getExtras();
                sessionId = extras.getLong(ThingProvider.EXTRA_SESSION_ID);
            }
            return true;
        }
        return false;
    }

    // Getters

    @Override
    public String getAccountName() {
        return accountName;
    }

    public String getActionAccountName() {
        return actionAccountName;
    }

    @Override
    public SubredditAdapter getAdapter() {
        return adapter;
    }

    @Override
    public String getSelectedSubreddit() {
        return adapter.getSelectedSubreddit();
    }

    @Override
    public boolean hasActionAccountName() {
        return getActionAccountName() != null;
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return false;
    }

    // Setters

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setActionAccountName(String actionAccountName) {
        this.actionAccountName = actionAccountName;
    }

    @Override
    public String setSelectedPosition(int position) {
        return adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedSubreddit(String selectedSubreddit) {
        adapter.setSelectedSubreddit(selectedSubreddit);
    }

    // Getters for extras.

    private static String getAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACCOUNT_NAME);
    }

    private static String getSelectedSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_SUBREDDIT);
    }

    private static String getQueryExtra(Bundle extras) {
        return extras.getString(EXTRA_QUERY);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }

    private static long getSessionIdExtra(Bundle extras) {
        return extras.getLong(EXTRA_SESSION_ID);
    }

    private static String getActionAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACTION_ACCOUNT_NAME);
    }
}
