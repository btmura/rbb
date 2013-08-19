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
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.HideActions;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.AbstractThingListAdapter;

class ThingTableActionModeController implements ThingActionModeController, ThingProjection {

    private final Context context;
    private final String accountName;
    private final AbstractThingListAdapter adapter;
    private ActionMode actionMode;

    ThingTableActionModeController(Context context,
            String accountName,
            AbstractThingListAdapter adapter) {
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

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu, ListView listView) {
        if (adapter.getCursor() == null) {
            listView.clearChoices();
            return false;
        }
        actionMode = mode;

        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thing_action_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        int position = ListViewUtils.getFirstCheckedPosition(listView);

        if (count == 1 && !TextUtils.isEmpty(getDomain(position))) {
            mode.setTitle(getDomain(position));
        } else {
            mode.setTitle(context.getResources().getQuantityString(R.plurals.things, count, count));
        }

        prepareAuthorActionItem(menu, listView, position);
        prepareCopyUrlActionItem(menu, listView, position);
        prepareHideActionItems(menu, listView, position);
        prepareSaveActionItems(menu, listView, position);
        prepareSubredditActionItem(menu, listView, position);
        prepareShareActionItem(menu, listView, position);
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

    private void prepareCopyUrlActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_copy_url);
        item.setVisible(isCheckedCount(listView, 1) && hasThingId(position));
    }

    private void prepareHideActionItems(Menu menu, ListView listView, int position) {
        menu.findItem(R.id.menu_hide)
                .setVisible(isCheckedCount(listView, 1) && isHidable(context, position, true));
        menu.findItem(R.id.menu_unhide)
                .setVisible(isCheckedCount(listView, 1) && isHidable(context, position, false));
    }

    private boolean isHidable(Context context, int position, boolean hide) {
        return hasAccount()
                && actionMode == null
                && isKind(position, Kinds.KIND_LINK)
                && (hide && isUnhidden(position) || !hide && isHidden(position));
    }

    private boolean isHidden(int position) {
        return !adapter.isNull(position, INDEX_HIDE_ACTION)
                && adapter.getInt(position, INDEX_HIDE_ACTION) == HideActions.ACTION_HIDE
                || adapter.getBoolean(position, INDEX_HIDDEN);
    }

    private boolean isUnhidden(int position) {
        return !adapter.isNull(position, INDEX_HIDE_ACTION)
                && adapter.getInt(position, INDEX_HIDE_ACTION) == HideActions.ACTION_UNHIDE
                || !adapter.getBoolean(position, INDEX_HIDDEN);
    }

    private void prepareSaveActionItems(Menu menu, ListView listView, int position) {
        boolean saveable = isCheckedCount(listView, 1) && hasAccount()
                && isKind(position, Kinds.KIND_LINK);
        boolean saved = isCheckedCount(listView, 1) && hasAccount()
                && isSaved(position);
        menu.findItem(R.id.menu_saved).setVisible(saveable && saved);
        menu.findItem(R.id.menu_unsaved).setVisible(saveable && !saved);
    }

    private boolean isSaved(int position) {
        // If no local save actions are pending, then rely on server info.
        if (adapter.isNull(position, INDEX_SAVE_ACTION)) {
            return adapter.getBoolean(position, INDEX_SAVED);
        }

        // We have a local pending action so use that to indicate if it's read.
        return adapter.getInt(position, INDEX_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }

    private void prepareShareActionItem(Menu menu, ListView listView, int position) {
        MenuItem item = menu.findItem(R.id.menu_share_thing);
        item.setVisible(isCheckedCount(listView, 1));
        if (item.isVisible()) {
            String title = getThingTitle(position);
            CharSequence url = getThingUrl(position);
            MenuHelper.setShareProvider(item, title, url);
        }
    }

    private String getThingTitle(int position) {
        // Link and comment posts have a title.
        String title = getTitle(position);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // CommentActions don't have titles so use the body.
        return getBody(position);
    }

    private CharSequence getThingUrl(int position) {
        // Most things and comments have the url attribute set.
        String url = getUrl(position);
        if (!TextUtils.isEmpty(url)) {
            return url;
        }

        // Comment references just provide a thing and link id.
        String thingId = getThingId(position);
        String linkId = getLinkId(position);
        return Urls.commentListing(thingId, linkId, -1, Urls.TYPE_HTML);
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
            case R.id.menu_saved:
                handleSave(listView, false);
                mode.finish();
                return true;

            case R.id.menu_unsaved:
                handleSave(listView, true);
                mode.finish();
                return true;

            case R.id.menu_hide:
                handleHide(listView, true);
                mode.finish();
                return true;

            case R.id.menu_unhide:
                handleHide(listView, false);
                mode.finish();
                return true;

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

    private void handleSave(ListView listView, boolean save) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
        if (save) {
            Provider.saveAsync(context, accountName,
                    getAuthor(position),
                    getCreatedUtc(position),
                    getDomain(position),
                    getDowns(position),
                    getLikes(position),
                    getNumComments(position),
                    isOver18(position),
                    getPermaLink(position),
                    getScore(position),
                    isSelf(position),
                    getSubreddit(position),
                    getThingId(position),
                    getThumbnailUrl(position),
                    getTitle(position),
                    getUps(position),
                    getUrl(position));
        } else {
            Provider.unsaveAsync(context, accountName, getThingId(position));
        }
    }

    private void handleHide(ListView listView, boolean hide) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
        hide(position, hide);
    }

    private void handleCopyUrl(ListView listView) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
        MenuHelper.setClipAndToast(context, getThingTitle(position), getThingUrl(position));
    }

    private void handleAuthor(ListView listView) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
        MenuHelper.startProfileActivity(context, getAuthor(position), -1);
    }

    private void handleSubreddit(ListView listView) {
        int position = ListViewUtils.getFirstCheckedPosition(listView);
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
    public boolean isHidable(int position) {
        return isHidable(context, position, true);
    }

    @Override
    public void hide(int position, boolean hide) {
        if (hide) {
            Provider.hideAsync(context, accountName,
                    getAuthor(position),
                    getCreatedUtc(position),
                    getDomain(position),
                    getDowns(position),
                    getLikes(position),
                    getNumComments(position),
                    isOver18(position),
                    getPermaLink(position),
                    getScore(position),
                    isSelf(position),
                    getSubreddit(position),
                    getThingId(position),
                    getThumbnailUrl(position),
                    getTitle(position),
                    getUps(position),
                    getUrl(position));
        } else {
            Provider.unhideAsync(context, accountName, getThingId(position));
        }
    }

    @Override
    public void vote(int position, int action) {
        Provider.voteAsync(context, accountName, action,
                getAuthor(position),
                getCreatedUtc(position),
                getDomain(position),
                getDowns(position),
                getLikes(position),
                getNumComments(position),
                isOver18(position),
                getPermaLink(position),
                getScore(position),
                isSelf(position),
                getSubreddit(position),
                getThingId(position),
                getThumbnailUrl(position),
                getTitle(position),
                getUps(position),
                getUrl(position));
    }

    // Utility methods

    private boolean hasAccount() {
        return AccountUtils.isAccount(accountName);
    }

    private boolean hasThingId(int position) {
        return !TextUtils.isEmpty(getThingId(position));
    }

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    private boolean isKind(int position, int kind) {
        return getKind(position) == kind;
    }

    // Getters for thing attributes

    private String getAuthor(int position) {
        return adapter.getString(position, INDEX_AUTHOR);
    }

    private String getBody(int position) {
        return adapter.getString(position, INDEX_BODY);
    }

    private long getCreatedUtc(int position) {
        return adapter.getLong(position, INDEX_CREATED_UTC);
    }

    private String getDomain(int position) {
        return adapter.getString(position, INDEX_DOMAIN);
    }

    private int getDowns(int position) {
        return adapter.getInt(position, INDEX_DOWNS);
    }

    private int getKind(int position) {
        return adapter.getInt(position, INDEX_KIND);
    }

    private int getLikes(int position) {
        return adapter.getInt(position, INDEX_LIKES);
    }

    private String getLinkId(int position) {
        return adapter.getString(position, INDEX_LINK_ID);
    }

    private int getNumComments(int position) {
        return adapter.getInt(position, INDEX_NUM_COMMENTS);
    }

    private String getPermaLink(int position) {
        return adapter.getString(position, INDEX_PERMA_LINK);
    }

    private int getScore(int position) {
        return adapter.getInt(position, INDEX_SCORE);
    }

    private String getSubreddit(int position) {
        return adapter.getString(position, INDEX_SUBREDDIT);
    }

    private String getThingId(int position) {
        return adapter.getString(position, INDEX_THING_ID);
    }

    private String getThumbnailUrl(int position) {
        return adapter.getString(position, INDEX_THUMBNAIL_URL);
    }

    private String getTitle(int position) {
        return adapter.getString(position, INDEX_TITLE);
    }

    private int getUps(int position) {
        return adapter.getInt(position, INDEX_UPS);
    }

    private String getUrl(int position) {
        return adapter.getString(position, INDEX_URL);
    }

    private boolean isOver18(int position) {
        return adapter.getBoolean(position, INDEX_OVER_18);
    }

    private boolean isSelf(int position) {
        return adapter.getBoolean(position, INDEX_SELF);
    }
}