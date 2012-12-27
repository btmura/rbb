/*
 * Copyright (C) 2012 Brian Muramatsu
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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Objects;

public class MessageThreadLoaderAdapter extends BaseLoaderAdapter {

    private static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CONTEXT,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_KIND,
            Messages.COLUMN_SUBREDDIT,
            Messages.COLUMN_THING_ID,
            Messages.COLUMN_WAS_COMMENT,
    };

    private static final int INDEX_AUTHOR = 1;
    private static final int INDEX_BODY = 2;
    private static final int INDEX_CONTEXT = 3;
    private static final int INDEX_CREATED_UTC = 4;
    private static final int INDEX_KIND = 5;
    private static final int INDEX_SUBREDDIT = 6;
    private static final int INDEX_THING_ID = 7;
    private static final int INDEX_WAS_COMMENT = 8;

    public MessageThreadLoaderAdapter(Context context) {
        super(context, null);
    }

    @Override
    public boolean isLoadable() {
        return accountName != null;
    }

    @Override
    protected Uri getLoaderUri() {
        switch (filter) {
            case FilterAdapter.MESSAGE_INBOX:
                return ThingProvider.messageInboxUri(sessionId, accountName);

            case FilterAdapter.MESSAGE_SENT:
                return ThingProvider.messageSentUri(sessionId, accountName);

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected String[] getProjection() {
        return PROJECTION;
    }

    @Override
    public String getAuthor(int position) {
        return getString(position, INDEX_AUTHOR);
    }

    @Override
    public String getLinkId(int position) {
        return null; // Messages don't have link ids.
    }

    @Override
    public Bundle getReplyExtras(int position) {
        return null;
    }

    @Override
    public boolean isSaved(int position) {
        return false;
    }

    @Override
    public String getThingId(int position) {
        return getString(position, INDEX_THING_ID);
    }

    @Override
    public String getTitle(int position) {
        return getString(position, INDEX_BODY);
    }

    @Override
    public CharSequence getUrl(int position) {
        // Comment reply messages have a context url we can use.
        String context = getString(position, INDEX_CONTEXT);
        if (!TextUtils.isEmpty(context)) {
            return Urls.perma(context, null);
        }

        // Assume this is a raw message.
        return Urls.messageThread(getThingId(position), Urls.TYPE_HTML);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        switch (getInt(position, INDEX_KIND)) {
            case Kinds.KIND_MORE:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        switch (getInt(cursor.getPosition(), INDEX_KIND)) {
            case Kinds.KIND_MORE:
                LayoutInflater inflater = LayoutInflater.from(context);
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                return new ThingView(context);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            String author = cursor.getString(INDEX_AUTHOR);
            String body = cursor.getString(INDEX_BODY);
            long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
            int kind = cursor.getInt(INDEX_KIND);
            String subreddit = cursor.getString(INDEX_SUBREDDIT);
            String thingId = cursor.getString(INDEX_THING_ID);

            ThingView tv = (ThingView) view;
            tv.setData(accountName, author, body, createdUtc, null, 0, true, kind, 0,
                    null, 0, System.currentTimeMillis(), 0, false, parentSubreddit, 0, subreddit,
                    thingBodyWidth, thingId, null, null, 0);
            tv.setChosen(singleChoice && Objects.equals(selectedThingId, thingId));
            tv.setOnVoteListener(listener);
        }
    }

    @Override
    protected Bundle makeThingBundle(Context context, Cursor c) {
        Bundle b = new Bundle(5);
        ThingBundle.putSubreddit(b, c.getString(INDEX_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(INDEX_KIND));

        // Messages don't have titles so use the body for both.
        String body = c.getString(INDEX_BODY);
        ThingBundle.putTitle(b, body);

        ThingBundle.putThingId(b, c.getString(INDEX_THING_ID));

        String contextUrl = c.getString(INDEX_CONTEXT);
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
        ThingBundle.putNoComments(b, c.getInt(INDEX_WAS_COMMENT) == 0);

        return b;
    }
}
