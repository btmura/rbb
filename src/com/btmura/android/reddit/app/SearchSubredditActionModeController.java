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
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.SearchSubredditAdapter;

class SearchSubredditActionModeController implements ActionModeController, OnClickListener {

    // TODO: Use this in NavigationFragment to remove code duplication for action mode.
    public interface CheckedSubredditProvider {
        String getFirstCheckedSubreddit();

        String[] getCheckedSubreddits();
    }

    private final Context context;
    private final SearchSubredditAdapter adapter;
    private final AccountResultHolder accountResultHolder;
    private final CheckedSubredditProvider checkedProvider;

    private final AccountNameAdapter accountNameAdapter;
    private ActionMode actionMode;
    private TextView subredditCountText;
    private Spinner accountSpinner;
    private ImageButton addSubredditButton;

    SearchSubredditActionModeController(Context context,
            SearchSubredditAdapter adapter,
            AccountResultHolder accountResultHolder,
            CheckedSubredditProvider subredditProvider) {
        this.context = context;
        this.adapter = adapter;
        this.accountResultHolder = accountResultHolder;
        this.checkedProvider = subredditProvider;
        this.accountNameAdapter = new AccountNameAdapter(context, R.layout.account_name_row);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        // No state to restore
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        // No state to save
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
        accountSpinner.setAdapter(accountNameAdapter);
        addSubredditButton = (ImageButton) view.findViewById(R.id.add_subreddit_button);
        addSubredditButton.setOnClickListener(this);

        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        boolean aboutItemVisible = count == 1;
        boolean shareItemsVisible = count == 1;

        SparseBooleanArray checked = listView.getCheckedItemPositions();
        int size = checked.size();
        for (int i = 0; i < size; i++) {
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                String subreddit = adapter.getName(position);
                boolean hasSidebar = Subreddits.hasSidebar(subreddit);
                aboutItemVisible &= hasSidebar;
                shareItemsVisible &= hasSidebar;
            }
        }

        prepareModeCustomView(count);
        prepareAboutItem(menu, listView, aboutItemVisible);
        prepareDeleteItem(menu);
        prepareShareItems(menu, listView, shareItemsVisible);
        return true;
    }

    private void prepareModeCustomView(int checkedCount) {
        subredditCountText.setText(context.getResources().getQuantityString(R.plurals.subreddits,
                checkedCount, checkedCount));

        accountNameAdapter.clear();
        AccountResult result = accountResultHolder.getAccountResult();
        if (result != null) {
            accountNameAdapter.addAll(result.accountNames);
        }

        if (accountNameAdapter.getCount() > 1) {
            String accountName = result.getLastAccount(context);
            int position = accountNameAdapter.findAccountName(accountName);
            accountSpinner.setSelection(position);
            accountSpinner.setVisibility(View.VISIBLE);
            addSubredditButton.setVisibility(View.VISIBLE);
        } else {
            accountSpinner.setVisibility(View.GONE);
            addSubredditButton.setVisibility(View.GONE);
        }
    }

    private void prepareAboutItem(Menu menu, ListView listView, boolean visible) {
        MenuItem aboutItem = menu.findItem(R.id.menu_subreddit);
        aboutItem.setVisible(visible);
        if (visible) {
            aboutItem.setTitle(context.getString(R.string.menu_subreddit,
                    checkedProvider.getFirstCheckedSubreddit()));
        }
    }

    private void prepareDeleteItem(Menu menu) {
        menu.findItem(R.id.menu_delete).setVisible(false);
    }

    private void prepareShareItems(Menu menu, ListView listView, boolean visible) {
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        shareItem.setVisible(visible);
        if (visible) {
            MenuHelper.setShareProvider(shareItem, getClipLabel(listView), getClipText(listView));
        }

        MenuItem copyUrlItem = menu.findItem(R.id.menu_copy_url);
        copyUrlItem.setVisible(visible);
    }

    private String getClipLabel(ListView listView) {
        return checkedProvider.getFirstCheckedSubreddit();
    }

    private CharSequence getClipText(ListView listView) {
        return Urls.subreddit(checkedProvider.getFirstCheckedSubreddit(), -1, null,
                Urls.TYPE_HTML);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        mode.invalidate();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView listView) {
        switch (item.getItemId()) {
            case R.id.menu_subreddit:
                handleSubreddit(listView);
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl(listView);
                mode.finish();
                return true;
        }
        return false;
    }

    private void handleSubreddit(ListView listView) {
        MenuHelper.startSidebarActivity(context, checkedProvider.getFirstCheckedSubreddit());
    }

    private void handleCopyUrl(ListView listView) {
        MenuHelper.setClipAndToast(context, getClipLabel(listView), getClipText(listView));
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
        subredditCountText = null;
        accountSpinner = null;
        addSubredditButton = null;
    }

    @Override
    public void onClick(View v) {
        String accountName = accountNameAdapter.getItem(accountSpinner.getSelectedItemPosition());
        Provider.addSubredditAsync(context, accountName, checkedProvider.getCheckedSubreddits());
        actionMode.finish();
    }

    @Override
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }
}
