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
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.Views;
import com.btmura.android.reddit.widget.MessageListAdapter;

public class MessageThingActionModeController implements ThingActionModeController {

    private final Context context;
    private final MessageListAdapter adapter;
    private ActionMode actionMode;

    MessageThingActionModeController(Context context, MessageListAdapter adapter) {
        this.context = context;
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu, ListView listView) {
        if (adapter.getCursor() == null) {
            listView.clearChoices();
            return false;
        }
        actionMode = mode;

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.message_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        mode.setTitle(context.getResources().getQuantityString(R.plurals.things, count, count));

        int position = Views.getCheckedPosition(listView);
        prepareAuthorActionItem(menu, listView, position);
        prepareShareActionItem(menu, listView, position);
        prepareSubredditActionItem(menu, listView, position);
        return true;
    }

    private void prepareAuthorActionItem(Menu menu, ListView listView, int position) {
        String author = getAuthor(position);
        MenuItem item = menu.findItem(R.id.menu_author);
        item.setVisible(isCheckedCount(listView, 1) && MenuHelper.isUserItemVisible(author));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getUserTitle(context, author));
        }
    }

    private void prepareShareActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_share_thing);
        item.setVisible(isCheckedCount(listView, 1));
        if (item.isVisible()) {
            String title = getMessageTitle(position);
            CharSequence url = getMessageUrl(position);
            MenuHelper.setShareProvider(item, title, url);
        }
    }

    private String getMessageTitle(int position) {
        // Comment reply messages just have "comment reply" as subject so try to use the link title.
        String linkTitle = getLinkTitle(position);
        if (!TextUtils.isEmpty(linkTitle)) {
            return linkTitle;
        }

        // Assume this is a message with a subject.
        return getSubject(position);
    }

    private CharSequence getMessageUrl(int position) {
        // Comment reply messages have a context url we can use.
        String context = getContext(position);
        if (!TextUtils.isEmpty(context)) {
            return Urls.perma(context, null);
        }

        // Assume this is a raw message.
        return Urls.messageThread(getThingId(position), Urls.TYPE_HTML);
    }

    private void prepareSubredditActionItem(Menu menu, ListView listView, int position) {
        String subreddit = getSubreddit(position);
        MenuItem item = menu.findItem(R.id.menu_subreddit);
        item.setVisible(isCheckedCount(listView, 1) && Subreddits.hasSidebar(subreddit));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getSubredditTitle(context, subreddit));
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView listView) {
        switch (item.getItemId()) {
            case R.id.menu_copy_url:
                handleCopyUrl(listView);
                mode.finish();
                return true;

            case R.id.menu_author:
                handleAuthor(listView);
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit(listView);
                mode.finish();
                return true;
        }
        return false;
    }

    private void handleCopyUrl(ListView listView) {
        int position = Views.getCheckedPosition(listView);
        MenuHelper.setClipAndToast(context, getMessageTitle(position), getMessageUrl(position));
    }

    private void handleAuthor(ListView listView) {
        int position = Views.getCheckedPosition(listView);
        MenuHelper.startProfileActivity(context, getAuthor(position), -1);
    }

    private void handleSubreddit(ListView listView) {
        int position = Views.getCheckedPosition(listView);
        MenuHelper.startSidebarActivity(context, getSubreddit(position));
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

    @Override
    public boolean isSwipeable(int position) {
        return false;
    }

    @Override
    public void clickStatus(int position) {
    }

    @Override
    public void swipe(int position) {
    }

    @Override
    public void vote(int position, int action) {
    }

    // Utility methods

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    // Getters for message attributes

    private String getAuthor(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_AUTHOR);
    }

    private String getContext(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_CONTEXT);
    }

    private String getLinkTitle(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_LINK_TITLE);
    }

    private String getSubject(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_SUBJECT);
    }

    private String getSubreddit(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_SUBREDDIT);
    }

    private String getThingId(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_THING_ID);
    }
}
