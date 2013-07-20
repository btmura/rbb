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
import android.support.v4.app.FragmentManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.SearchSubredditAdapter;

class SearchSubredditActionModeController implements ActionModeController {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final SearchSubredditAdapter adapter;

    private ActionMode actionMode;

    SearchSubredditActionModeController(Context context, FragmentManager fragmentManager,
            SearchSubredditAdapter adapter) {
        this.context = context;
        this.fragmentManager = fragmentManager;
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

        prepareModeCustomView(count);
        prepareAboutItem(menu, listView, aboutItemVisible);
        prepareDeleteItem(menu);
        prepareShareItems(menu, listView, shareItemsVisible);
        return true;
    }

    private void prepareModeCustomView(int checkedCount) {
        actionMode.setTitle(context.getResources().getQuantityString(R.plurals.subreddits,
                checkedCount, checkedCount));
    }

    private void prepareAboutItem(Menu menu, ListView listView, boolean visible) {
        MenuItem aboutItem = menu.findItem(R.id.menu_subreddit);
        aboutItem.setVisible(visible);
        if (visible) {
            aboutItem.setTitle(context.getString(R.string.menu_subreddit,
                    getFirstCheckedSubreddit(listView)));
        }
    }

    private String getFirstCheckedSubreddit(ListView listView) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
        return adapter.getName(position);
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
        return getFirstCheckedSubreddit(listView);
    }

    private CharSequence getClipText(ListView listView) {
        return Urls.subreddit(getFirstCheckedSubreddit(listView), -1, null, Urls.TYPE_HTML);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        mode.invalidate();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView listView) {
        switch (item.getItemId()) {
            case R.id.menu_add_subreddit:
                handleAddSubreddit(listView);
                mode.finish();
                return true;

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

    private void handleAddSubreddit(ListView listView) {
        MenuHelper.showAddSubredditDialog(fragmentManager, getCheckedSubreddits(listView));
    }

    private String[] getCheckedSubreddits(ListView listView) {
        int checkedCount = listView.getCheckedItemCount();
        String[] subreddits = new String[checkedCount];

        SparseBooleanArray checked = listView.getCheckedItemPositions();
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

    private void handleSubreddit(ListView listView) {
        MenuHelper.startSidebarActivity(context, getFirstCheckedSubreddit(listView));
    }

    private void handleCopyUrl(ListView listView) {
        MenuHelper.setClipAndToast(context, getClipLabel(listView), getClipText(listView));
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }

    @Override
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }
}
