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
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.content.SubredditSearchLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.ViewUtils;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditSearchAdapter;

class SubredditSearchController extends AbstractSubredditListController implements
        OnItemSelectedListener {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_SELECTED_SUBREDDIT = "selectedSubreddit";
    static final String EXTRA_QUERY = "query";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";

    private static final String EXTRA_SESSION_ID = "sessionId";
    private static final String EXTRA_ACTION_ACCOUNT_NAME = "actionAccountName";

    private final AccountResultHolder accountResultHolder;
    private final SubredditAdapter adapter;
    private AccountNameAdapter accountNameAdapter;

    private String accountName;
    private String query;
    private long sessionId;
    private String actionAccountName;

    private ActionMode actionMode;
    private TextView subredditCountText;
    private Spinner accountSpinner;

    SubredditSearchController(Context context, Bundle args,
            AccountResultHolder accountResultHolder) {
        super(context);
        this.accountResultHolder = accountResultHolder;
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

    // Menu creation and preparation methods

    @Override
    public boolean createActionMode(ActionMode mode, Menu menu, ListView listView) {
        if (adapter.getCursor() == null) {
            listView.clearChoices();
            return false;
        }

        View v = LayoutInflater.from(context).inflate(R.layout.subreddit_cab, null, false);
        mode.setCustomView(v);

        actionMode = mode;
        subredditCountText = (TextView) v.findViewById(R.id.subreddit_count);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);

        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    @Override
    public boolean prepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        boolean hasCursor = adapter.getCursor() != null;

        if (hasCursor) {
            String text = context.getResources().getQuantityString(R.plurals.subreddits, count,
                    count);
            subredditCountText.setText(text);
            subredditCountText.setVisibility(View.VISIBLE);
        } else {
            subredditCountText.setVisibility(View.GONE);
        }

        if (accountResultHolder != null && accountResultHolder.getAccountResult() != null) {
            if (accountNameAdapter == null) {
                accountNameAdapter = new AccountNameAdapter(context, R.layout.account_name_row);
            } else {
                accountNameAdapter.clear();
            }

            AccountResult result = accountResultHolder.getAccountResult();
            accountNameAdapter.addAll(result.accountNames);
            accountSpinner.setAdapter(accountNameAdapter);
            accountSpinner.setOnItemSelectedListener(this);

            if (result.accountNames.length > 0) {
                if (!hasActionAccountName()) {
                    setActionAccountName(result.getLastAccount(context));
                }
                int position = accountNameAdapter.findAccountName(getActionAccountName());
                accountSpinner.setSelection(position);
                accountSpinner.setVisibility(View.VISIBLE);
            } else {
                accountSpinner.setSelection(0);
                accountSpinner.setVisibility(View.GONE);
            }
        }

        menu.findItem(R.id.menu_add).setVisible(hasCursor);
        menu.findItem(R.id.menu_delete).setVisible(false);

        MenuItem subredditItem = menu.findItem(R.id.menu_subreddit);
        subredditItem.setVisible(hasCursor && count == 1
                && Subreddits.hasSidebar(getSubreddit(ViewUtils.getFirstCheckedPosition(listView))));

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> av, View v, int position, long id) {
        setActionAccountName(accountNameAdapter.getItem(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        setActionAccountName(null);
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

    // Getters for subreddit attributes.

    @Override
    protected String getSelectedAccountName() {
        return accountNameAdapter.getItem(accountSpinner.getSelectedItemPosition());
    }

    @Override
    protected int getCount() {
        return adapter.getCount();
    }

    @Override
    protected String getSubreddit(int position) {
        return adapter.getName(position);
    }

    @Override
    protected boolean isDeletable(int position) {
        return adapter.isDeletable(position);
    }
}
