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
import android.view.Menu;
import android.widget.ListView;

import com.btmura.android.reddit.content.MessageThingLoader;
import com.btmura.android.reddit.database.ReadActions;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.widget.AbstractThingListAdapter;
import com.btmura.android.reddit.widget.ThingBundle;

class MessageThingListController implements ThingListController {

    private final Context context;
    private final String accountName;
    private final String messageUser;
    private final int filter;
    private final String more;
    private final AbstractThingListAdapter adapter;

    MessageThingListController(Context context, String accountName, String messageUser, int filter,
            String more, AbstractThingListAdapter adapter) {
        this.context = context.getApplicationContext();
        this.accountName = accountName;
        this.messageUser = messageUser;
        this.filter = filter;
        this.more = more;
        this.adapter = adapter;
    }

    @Override
    public boolean isLoadable() {
        return accountName != null && messageUser != null;
    }

    @Override
    public void saveState(Bundle outState) {
    }

    @Override
    public void restoreState(Bundle savedInstanceState) {
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new MessageThingLoader(context, accountName, filter, more);
    }

    @Override
    public Bundle getThingBundle(int position) {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeMessageThingBundle(c);
        }
        return null;
    }

    private Bundle makeMessageThingBundle(Cursor c) {
        Bundle b = new Bundle(7);
        ThingBundle.putAuthor(b, c.getString(MessageThingLoader.MESSAGE_AUTHOR));
        ThingBundle.putSubreddit(b, c.getString(MessageThingLoader.MESSAGE_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(MessageThingLoader.MESSAGE_KIND));

        // Messages don't have titles so use the body for both.
        String body = c.getString(MessageThingLoader.MESSAGE_BODY);
        ThingBundle.putTitle(b, body);

        ThingBundle.putThingId(b, c.getString(MessageThingLoader.MESSAGE_THING_ID));

        String contextUrl = c.getString(MessageThingLoader.MESSAGE_CONTEXT);
        if (!TextUtils.isEmpty(contextUrl)) {
            // If there is a context url, then we have to parse that url to grab
            // the link id embedded inside of it like:
            //
            // /r/rbb/comments/13ejyf/testing_from_laptop/c738opg?context=3
            String[] parts = contextUrl.split("/");
            if (parts != null && parts.length >= 5) {
                ThingBundle.putLinkId(b, parts[4]);
            }
        }

        // If this message isn't a comment, then it's simply a message with no
        // comments or links.
        ThingBundle.putNoComments(b, c.getInt(MessageThingLoader.MESSAGE_WAS_COMMENT) == 0);

        return b;
    }

    // Actions.

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
    public void prepareActionMenu(Menu menu, ListView listView, int position) {
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
        return null;
    }

    @Override
    public boolean hasNextMoreId() {
        return false;
    }

    private boolean isNew(int position) {
        // If no local read actions are pending, then rely on what reddit thinks.
        if (adapter.isNull(position, MessageThingLoader.MESSAGE_ACTION)) {
            return adapter.getBoolean(position, MessageThingLoader.MESSAGE_NEW);
        }

        // We have a local pending action so use that to indicate if it's new.
        return adapter.getInt(position, MessageThingLoader.MESSAGE_ACTION) == ReadActions.ACTION_UNREAD;
    }

    @Override
    public boolean isSwipeDismissable(int position) {
        return false;
    }

    // Simple getters.

    @Override
    public String getAccountName() {
        return null;
    }

    @Override
    public String getMoreId() {
        return null;
    }

    @Override
    public long getSessionId() {
        return 0;
    }

    @Override
    public AbstractThingListAdapter getAdapter() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getEmptyText() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFilter() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getParentSubreddit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQuery() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSelectedLinkId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSelectedThingId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSubreddit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasAccountName() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasCursor() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasQuery() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSingleChoice() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setEmptyText(int emptyText) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setParentSubreddit(String parentSubreddit) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSelectedPosition(int position) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSelectedThing(String thingId, String linkId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setSubreddit(String subreddit) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setThingBodyWidth(int thingBodyWidth) {
        // TODO Auto-generated method stub
    }

    @Override
    public void swapCursor(Cursor cursor) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setFilter(int filter) {
        // TODO Auto-generated method stub
    }

    // Simple setters.

    @Override
    public void setAccountName(String accountName) {
    }

    @Override
    public void setMoreId(String moreId) {
    }

    @Override
    public void setSessionId(long sessionId) {
    }

    // Simple adapter getters.

    private String getContext(int position) {
        return adapter.getString(position, MessageThingLoader.MESSAGE_CONTEXT);
    }

    private String getSubject(int position) {
        return adapter.getString(position, MessageThingLoader.MESSAGE_SUBJECT);
    }

    private String getThingId(int position) {
        return adapter.getString(MessageThingLoader.MESSAGE_THING_ID, position);
    }
}
