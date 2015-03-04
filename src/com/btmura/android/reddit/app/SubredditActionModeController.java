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
import android.support.v4.app.FragmentManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.Views;
import com.btmura.android.reddit.widget.SubredditAdapter;

class SubredditActionModeController implements ActionModeController {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final SubredditAdapter adapter;
    private final AccountResultHolder accountResultHolder;
    private ActionMode actionMode;

    SubredditActionModeController(Context context, FragmentManager fragmentManager,
            SubredditAdapter adapter, AccountResultHolder accountResultHolder) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.adapter = adapter;
        this.accountResultHolder = accountResultHolder;
    }

    @Override
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }

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

        prepareMode(count);
        prepareAddItem(menu);
        prepareAboutItem(menu, listView, aboutItemVisible);
        prepareDeleteItem(menu);
        prepareShareItems(menu, shareItemsVisible);
        return true;
    }

    private void prepareMode(int checked) {
        actionMode.setTitle(context.getResources().getQuantityString(R.plurals.subreddits,
                checked, checked));
    }

    private void prepareAddItem(Menu menu) {
        AccountResult result = accountResultHolder.getAccountResult();
        MenuItem item = menu.findItem(R.id.menu_add_subreddit);
        item.setVisible(result != null && result.accountNames.length > 1);
    }

    private void prepareAboutItem(Menu menu, ListView lv, boolean visible) {
        MenuItem item = menu.findItem(R.id.menu_subreddit);
        item.setVisible(visible);
        if (visible) {
            item.setTitle(context.getString(R.string.menu_subreddit, getFirstCheckedSubreddit(lv)));
        }
    }

    private void prepareDeleteItem(Menu menu) {
        menu.findItem(R.id.menu_delete).setVisible(false);
    }

    private void prepareShareItems(Menu menu, boolean visible) {
        menu.findItem(R.id.menu_share).setVisible(visible);
        menu.findItem(R.id.menu_copy_url).setVisible(visible);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView lv) {
        switch (item.getItemId()) {
            case R.id.menu_add_subreddit:
                handleAddSubreddit(lv);
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit(lv);
                mode.finish();
                return true;

            case R.id.menu_share:
                handleShare(lv);
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl(lv);
                mode.finish();
                return true;
        }
        return false;
    }

    private void handleAddSubreddit(ListView lv) {
        MenuHelper.showAddSubredditDialog(fragmentManager, getCheckedSubreddits(lv));
    }

    private void handleSubreddit(ListView lv) {
        MenuHelper.startSidebarActivity(context, getFirstCheckedSubreddit(lv));
    }

    private void handleShare(ListView lv) {
        MenuHelper.share(context, getClipText(lv));
    }

    private void handleCopyUrl(ListView lv) {
        MenuHelper.copyUrl(context, getClipLabel(lv), getClipText(lv));
    }

    private String getFirstCheckedSubreddit(ListView lv) {
        return adapter.getName(Views.getCheckedPosition(lv));
    }

    private String[] getCheckedSubreddits(ListView lv) {
        SparseBooleanArray checked = lv.getCheckedItemPositions();
        String[] subreddits = new String[lv.getCheckedItemCount()];

        int size = checked.size();
        int j = 0;
        for (int i = 0; i < size; i++) {
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                subreddits[j++] = adapter.getName(position);
            }
        }

        return subreddits;
    }

    private String getClipLabel(ListView lv) {
        return getFirstCheckedSubreddit(lv);
    }

    private CharSequence getClipText(ListView lv) {
        return Urls.subreddit(getFirstCheckedSubreddit(lv), -1, null, Urls.TYPE_HTML);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }
}
