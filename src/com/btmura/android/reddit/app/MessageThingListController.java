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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.ListViewUtils;
import com.btmura.android.reddit.widget.MessageListAdapter;

class MessageThingListController implements ThingListController<MessageListAdapter> {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_MESSAGE_USER = "messageUser";
    static final String EXTRA_FILTER = "filter";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";
    static final String EXTRA_CURSOR_EXTRAS = "cursorExtras";

    private final Context context;
    private final MessageListAdapter adapter;
    private final String messageUser;

    private String accountName;
    private int emptyText;
    private int filter;
    private String moreId;
    private Bundle cursorExtras;

    MessageThingListController(Context context, Bundle args) {
        this.context = context;
        this.adapter = new MessageListAdapter(context, getSingleChoiceExtra(args));
        this.accountName = getAccountNameExtra(args);
        this.messageUser = getMessageUserExtra(args);
        this.filter = getFilterExtra(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setAccountName(getAccountNameExtra(savedInstanceState));
        cursorExtras = savedInstanceState.getBundle(EXTRA_CURSOR_EXTRAS);
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_ACCOUNT_NAME, getAccountName());
        outState.putBundle(EXTRA_CURSOR_EXTRAS, cursorExtras);
    }

    // Loader related methods.

    @Override
    public boolean isLoadable() {
        return accountName != null && messageUser != null;
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new MessageThingLoader(context, accountName, filter, moreId, cursorExtras);
    }

    @Override
    public void swapCursor(Cursor cursor) {
        setMoreId(null);
        adapter.swapCursor(cursor);
        if (cursor != null && cursor.getExtras() != null) {
            cursorExtras = cursor.getExtras();
        }
    }

    @Override
    public ThingBundle getThingBundle(int position) {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToPosition(position)) {
            return adapter.getThingBundle(position);
        }
        return null;
    }

    public String getMessageUser() {
        return messageUser;
    }

    // Actions

    @Override
    public void author(int position) {
        MenuHelper.startProfileActivity(context, getAuthor(position), -1);
    }

    @Override
    public void copyUrl(int position) {
        MenuHelper.setClipAndToast(context, getMessageTitle(position), getMessageUrl(position));
    }

    // TODO(btmura): Remove this methods rather than throwing an exception.
    @Override
    public void hide(int position, boolean hide) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(int position, boolean save) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void select(int position) {
        if (adapter.isNew(position)) {
            Provider.readMessageAsync(context, accountName, getThingId(position), true);
        }
    }

    @Override
    public void subreddit(int position) {
        MenuHelper.startSidebarActivity(context, getSubreddit(position));
    }

    @Override
    public void vote(int position, int action) {
        throw new UnsupportedOperationException();
    }

    // Menu preparation methods.

    @Override
    public void onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
        int count = listView.getCheckedItemCount();
        mode.setTitle(context.getResources().getQuantityString(R.plurals.things, count, count));

        int position = ListViewUtils.getFirstCheckedPosition(listView);
        prepareAuthorActionItem(menu, listView, position);
        prepareHideActionItems(menu, listView, position);
        prepareShareActionItem(menu, listView, position);
        prepareSubredditActionItem(menu, listView, position);
    }

    private void prepareAuthorActionItem(Menu menu, ListView listView, int position) {
        String author = getAuthor(position);
        MenuItem item = menu.findItem(R.id.menu_author);
        item.setVisible(isCheckedCount(listView, 1) && MenuHelper.isUserItemVisible(author));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getUserTitle(context, author));
        }
    }

    private void prepareHideActionItems(Menu menu, ListView listView, int position) {
        menu.findItem(R.id.menu_hide).setVisible(false);
        menu.findItem(R.id.menu_unhide).setVisible(false);
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

    private void prepareSubredditActionItem(Menu menu, ListView listView, int position) {
        String subreddit = getSubreddit(position);
        MenuItem item = menu.findItem(R.id.menu_subreddit);
        item.setVisible(isCheckedCount(listView, 1) && Subreddits.hasSidebar(subreddit));
        if (item.isVisible()) {
            item.setTitle(MenuHelper.getSubredditTitle(context, subreddit));
        }
    }

    // More complex getters.

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

    @Override
    public String getNextMoreId() {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(MessageThingLoader.INDEX_KIND) == Kinds.KIND_MORE) {
                return c.getString(MessageThingLoader.INDEX_THING_ID);
            }
        }
        return null;
    }

    @Override
    public boolean hasNextMoreId() {
        return !TextUtils.isEmpty(getNextMoreId());
    }

    private boolean isCheckedCount(ListView listView, int checkedItemCount) {
        return listView.getCheckedItemCount() == checkedItemCount;
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return false;
    }

    // Getters

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public MessageListAdapter getAdapter() {
        return adapter;
    }

    @Override
    public int getEmptyText() {
        return emptyText;
    }

    @Override
    public int getFilter() {
        return filter;
    }

    @Override
    public String getMoreId() {
        return moreId;
    }

    @Override
    public String getSelectedLinkId() {
        return adapter.getSelectedLinkId();
    }

    @Override
    public String getSelectedThingId() {
        return adapter.getSelectedThingId();
    }

    @Override
    public boolean hasAccountName() {
        return !TextUtils.isEmpty(getAccountName());
    }

    @Override
    public boolean hasCursor() {
        return adapter.getCursor() != null;
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    // Simple setters.

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setEmptyText(int emptyText) {
        this.emptyText = emptyText;
    }

    @Override
    public void setFilter(int filter) {
        this.filter = filter;
    }

    @Override
    public void setMoreId(String moreId) {
        this.moreId = moreId;
    }

    @Override
    public void setSelectedPosition(int position) {
        adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedThing(String thingId, String linkId) {
        adapter.setSelectedThing(thingId, linkId);
    }

    @Override
    public void setThingBodyWidth(int thingBodyWidth) {
        adapter.setThingBodyWidth(thingBodyWidth);
    }

    // Simple adapter getters.

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

    // Getters for extras.

    private static String getAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACCOUNT_NAME);
    }

    private static String getMessageUserExtra(Bundle extras) {
        return extras.getString(EXTRA_MESSAGE_USER);
    }

    private static int getFilterExtra(Bundle extras) {
        return extras.getInt(EXTRA_FILTER);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }

    // Unfinished business...

    @Override
    public String getParentSubreddit() {
        return null;
    }

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSubreddit() {
        return null;
    }

    @Override
    public boolean hasQuery() {
        return false;
    }

    @Override
    public void setParentSubreddit(String parentSubreddit) {
    }

    @Override
    public void setSubreddit(String subreddit) {
    }

}
