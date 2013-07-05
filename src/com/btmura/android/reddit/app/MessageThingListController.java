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
import android.widget.ListView;

import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.widget.AbstractThingListAdapter;
import com.btmura.android.reddit.widget.MessageListAdapter;

class MessageThingListController implements ThingListController {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_MESSAGE_USER = "messageUser";
    static final String EXTRA_FILTER = "filter";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";

    private static final String EXTRA_SESSION_ID = "sessionId";

    private final Context context;
    private final MessageListAdapter adapter;
    private final String messageUser;

    private String accountName;
    private int emptyText;
    private int filter;
    private String moreId;
    private long sessionId;

    MessageThingListController(Context context, Bundle args) {
        this.context = context.getApplicationContext();
        this.adapter = new MessageListAdapter(context, getSingleChoiceExtra(args));
        this.accountName = getAccountNameExtra(args);
        this.messageUser = getMessageUserExtra(args);
        this.filter = getFilterExtra(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setAccountName(getAccountNameExtra(savedInstanceState));
        setSessionId(getSessionIdExtra(savedInstanceState));
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_ACCOUNT_NAME, getAccountName());
        outState.putLong(EXTRA_SESSION_ID, getSessionId());
    }

    // Loader related methods.

    @Override
    public boolean isLoadable() {
        return accountName != null && messageUser != null;
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new MessageThingLoader(context, accountName, filter, moreId, sessionId);
    }

    @Override
    public void swapCursor(Cursor cursor) {
        setMoreId(null);
        adapter.swapCursor(cursor);
        if (cursor != null && cursor.getExtras() != null) {
            Bundle extras = cursor.getExtras();
            setSessionId(extras.getLong(ThingProvider.EXTRA_SESSION_ID));
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

    // Actions

    @Override
    public void author(int position) {

    }

    @Override
    public void copyUrl(int position) {
        MenuHelper.setClipAndToast(context, getMessageTitle(position), getMessageUrl(position));
    }

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
        if (isNew(position)) {
            Provider.readMessageAsync(context, accountName, getThingId(position), true);
        }
    }

    @Override
    public void subreddit(int position) {
    }

    @Override
    public void vote(int position, int action) {
        throw new UnsupportedOperationException();
    }

    // Menu preparation methods.

    @Override
    public void onPrepareActionMode(ActionMode mode, Menu menu, ListView listView) {
    }

    // More complex getters.

    private String getMessageTitle(int position) {
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

    private boolean isNew(int position) {
        // If no local read actions are pending, then rely on what reddit thinks.
        if (adapter.isNull(position, MessageThingLoader.INDEX_ACTION)) {
            return adapter.getBoolean(position, MessageThingLoader.INDEX_NEW);
        }

        // We have a local pending action so use that to indicate if it's new.
        return adapter.getInt(position, MessageThingLoader.INDEX_ACTION) == ReadActions.ACTION_UNREAD;
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
    public AbstractThingListAdapter getAdapter() {
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
    public long getSessionId() {
        return sessionId;
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
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void setThingBodyWidth(int thingBodyWidth) {
        adapter.setThingBodyWidth(thingBodyWidth);
    }

    // Simple adapter getters.

    private String getContext(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_CONTEXT);
    }

    private String getSubject(int position) {
        return adapter.getString(position, MessageThingLoader.INDEX_SUBJECT);
    }

    private String getThingId(int position) {
        return adapter.getString(MessageThingLoader.INDEX_THING_ID, position);
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

    private static long getSessionIdExtra(Bundle extras) {
        return extras.getLong(EXTRA_SESSION_ID);
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
