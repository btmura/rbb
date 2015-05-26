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
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.net.Urls2;
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
    public void invalidateActionMode() {
        if (actionMode != null) {
            actionMode.invalidate();
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu, ListView lv) {
        if (adapter.getCursor() == null) {
            lv.clearChoices();
            return false;
        }
        actionMode = mode;
        mode.getMenuInflater().inflate(R.menu.message_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView lv) {
        int count = lv.getCheckedItemCount();
        mode.setTitle(context.getResources().getQuantityString(R.plurals.things, count, count));

        int pos = Views.getCheckedPosition(lv);
        prepareShare(menu, lv);
        prepareCopyUrl(menu, lv);
        prepareAuthor(menu, lv, pos);
        prepareSubreddit(menu, lv, pos);
        return true;
    }

    private void prepareAuthor(Menu menu, ListView lv, int pos) {
        String author = getAuthor(pos);
        MenuItem item = menu.findItem(R.id.menu_author);
        item.setVisible(isCheckedCount(lv, 1) && MenuHelper.isUserItemVisible(author));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getUserTitle(context, author));
        }
    }

    private void prepareCopyUrl(Menu menu, ListView lv) {
        menu.findItem(R.id.menu_copy_url).setVisible(isCheckedCount(lv, 1));
    }

    private void prepareShare(Menu menu, ListView lv) {
        menu.findItem(R.id.menu_share_thing).setVisible(isCheckedCount(lv, 1));
    }

    private void prepareSubreddit(Menu menu, ListView lv, int pos) {
        String subreddit = getSubreddit(pos);
        MenuItem item = menu.findItem(R.id.menu_subreddit);
        item.setVisible(isCheckedCount(lv, 1) && Subreddits.hasSidebar(subreddit));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getSubredditTitle(context, subreddit));
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item, ListView lv) {
        switch (item.getItemId()) {
            case R.id.menu_share_thing:
                handleShare(lv);
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                handleCopyUrl(lv);
                mode.finish();
                return true;

            case R.id.menu_author:
                handleAuthor(lv);
                mode.finish();
                return true;

            case R.id.menu_subreddit:
                handleSubreddit(lv);
                mode.finish();
                return true;
        }
        return false;
    }

    private void handleShare(ListView lv) {
        int pos = Views.getCheckedPosition(lv);
        MenuHelper.share(context, getMessageTitle(pos), getMessageUrl(pos));
    }

    private void handleCopyUrl(ListView lv) {
        int pos = Views.getCheckedPosition(lv);
        MenuHelper.copyUrl(context, getMessageTitle(pos), getMessageUrl(pos));
    }

    private void handleAuthor(ListView lv) {
        int pos = Views.getCheckedPosition(lv);
        MenuHelper.startProfileActivity(context, getAuthor(pos), -1);
    }

    private void handleSubreddit(ListView lv) {
        int pos = Views.getCheckedPosition(lv);
        MenuHelper.startSidebarActivity(context, getSubreddit(pos));
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int pos, long id, boolean checked) {
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
    public void swipe(int pos) {
    }

    @Override
    public void vote(int pos, int action) {
    }

    // Utility methods

    private String getMessageTitle(int position) {
        // Comment reply messages just have "comment reply" as subject so try to use the link title.
        String title = getLinkTitle(position);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // Assume this is a message with a subject.
        return getSubject(position);
    }

    private CharSequence getMessageUrl(int pos) {
        // Comment reply messages have a context url we can use.
        String context = getContext(pos);
        if (!TextUtils.isEmpty(context)) {
            return Urls.perma(context, null);
        }

        // Assume this is a raw message.
        return Urls2.messageThread("", getThingId(pos), Urls2.TYPE_HTML);
    }

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    // Getters for message attributes

    private String getAuthor(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_AUTHOR);
    }

    private String getContext(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_CONTEXT);
    }

    private String getLinkTitle(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_LINK_TITLE);
    }

    private String getSubject(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_SUBJECT);
    }

    private String getSubreddit(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_SUBREDDIT);
    }

    private String getThingId(int pos) {
        return adapter.getString(pos, MessageThingLoader.INDEX_THING_ID);
    }
}
