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

import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.widget.MessageListAdapter;

class MessageThingListController implements ThingListController<MessageListAdapter> {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_MESSAGE_USER = "messageUser";
    static final String EXTRA_FILTER = "filter";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";
    static final String EXTRA_CURSOR_EXTRAS = "cursorExtras";

    private final Context context;
    private final String accountName;
    private final String messageUser;
    private final int filter;
    private final MessageListAdapter adapter;

    private String moreId;
    private Bundle cursorExtras;

    MessageThingListController(Context context, Bundle args) {
        this.context = context;
        this.accountName = getAccountNameExtra(args);
        this.messageUser = getMessageUserExtra(args);
        this.filter = getFilterExtra(args);
        this.adapter = new MessageListAdapter(context, accountName, getSingleChoiceExtra(args));
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        cursorExtras = savedInstanceState.getBundle(EXTRA_CURSOR_EXTRAS);
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putBundle(EXTRA_CURSOR_EXTRAS, cursorExtras);
    }

    // Loader related methods.

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
    public void onThingSelected(int position) {
        if (adapter.isNew(position)) {
            Provider.readMessageAsync(context, accountName, getThingId(position), true);
        }
    }

    // More complex getters.

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
