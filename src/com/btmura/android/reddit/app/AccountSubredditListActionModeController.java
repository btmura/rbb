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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;

public class AccountSubredditListActionModeController implements ActionModeController {

    private final Context context;
    private final String accountName;
    private final AccountSubredditAdapter adapter;
    private ActionMode actionMode;

    public AccountSubredditListActionModeController(Context context,
            String accountName,
            AccountSubredditAdapter adapter) {
        this.context = context;
        this.accountName = accountName;
        this.adapter = adapter;
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
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }

    // MultiChoiceMode-like listener methods

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu, ListView listView) {
        if (adapter.getCursor() == null) {
            listView.clearChoices();
            return false;
        }

        actionMode = mode;
        MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.subreddit_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        boolean hasCursor = adapter.getCursor() != null;

        if (hasCursor) {
            Resources resources = context.getResources();
            mode.setTitle(resources.getQuantityString(R.plurals.subreddits, count, count));
        } else {
            mode.setTitle("");
        }

        menu.findItem(R.id.menu_add).setVisible(false);
        menu.findItem(R.id.menu_delete).setVisible(hasCursor);

        MenuItem subredditItem = menu.findItem(R.id.menu_subreddit);
        String subreddit = adapter.getName(ListViewUtils.getFirstCheckedPosition(listView));
        subredditItem.setVisible(hasCursor && count == 1 && Subreddits.hasSidebar(subreddit));

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView listView) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                handleDelete(listView);
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

    private void handleDelete(ListView listView) {
        String[] subreddits = adapter.getCheckedSubreddits(listView);
        Provider.removeSubredditAsync(context, accountName, subreddits);
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
    }
}
