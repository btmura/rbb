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
import android.content.res.Resources;
import android.os.Bundle;
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
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.SearchSubredditAdapter;

class SubredditSearchActionModeController implements SearchSubredditListActionModeController,
        OnItemSelectedListener {

    private static final String EXTRA_ACTION_ACCOUNT_NAME = "actionAccountName";

    private final Context context;
    private final SearchSubredditAdapter adapter;
    private final AccountResultHolder accountResultHolder;

    private ActionMode actionMode;
    private TextView subredditCountText;
    private Spinner accountSpinner;

    private AccountNameAdapter accountNameAdapter;
    private String actionAccountName;

    SubredditSearchActionModeController(Context context,
            SearchSubredditAdapter adapter,
            AccountResultHolder accountResultHolder) {
        this.context = context;
        this.adapter = adapter;
        this.accountResultHolder = accountResultHolder;
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        this.actionAccountName = savedInstanceState.getString(EXTRA_ACTION_ACCOUNT_NAME);
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_ACTION_ACCOUNT_NAME, actionAccountName);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu, ListView listView) {
        if (adapter.getCursor() == null) {
            listView.clearChoices();
            return false;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.subreddit_cab, null, false);
        mode.setCustomView(view);

        actionMode = mode;
        subredditCountText = (TextView) view.findViewById(R.id.subreddit_count);
        accountSpinner = (Spinner) view.findViewById(R.id.account_spinner);

        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        boolean hasCursor = adapter.getCursor() != null;
        int count = listView.getCheckedItemCount();

        if (hasCursor) {
            Resources r = context.getResources();
            subredditCountText.setText(r.getQuantityString(R.plurals.subreddits, count, count));
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
                if (actionAccountName == null) {
                    actionAccountName = result.getLastAccount(context);
                }
                int position = accountNameAdapter.findAccountName(actionAccountName);
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
        String subreddit = adapter.getName(ListViewUtils.getFirstCheckedPosition(listView));
        subredditItem.setVisible(hasCursor && count == 1 && Subreddits.hasSidebar(subreddit));

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> av, View v, int position, long id) {
        actionAccountName = accountNameAdapter.getItem(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        actionAccountName = null;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView listView) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleAdd(listView);
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit(listView);
                mode.finish();
                return true;

            default:
                return false;
        }
    }

    private void handleAdd(ListView listView) {
        String[] subreddits = adapter.getCheckedSubreddits(listView);
        Provider.addSubredditAsync(context, actionAccountName, subreddits);
    }

    private void handleSubreddit(ListView listView) {
        String subreddit = adapter.getName(ListViewUtils.getFirstCheckedPosition(listView));
        MenuHelper.startSidebarActivity(context, subreddit);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        mode.invalidate();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        actionAccountName = null;
    }

    @Override
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }
}
