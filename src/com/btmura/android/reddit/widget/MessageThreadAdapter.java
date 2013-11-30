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
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.content.MessageThreadLoader;
import com.btmura.android.reddit.text.MarkdownFormatter;

public class MessageThreadAdapter extends BaseCursorAdapter {

    private final MarkdownFormatter formatter = new MarkdownFormatter();
    private String accountName;
    private String thingId;
    private long nowTimeMs;

    public MessageThreadAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        nowTimeMs = System.currentTimeMillis();
        return super.swapCursor(newCursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(MessageThreadLoader.INDEX_AUTHOR);
        final String body = cursor.getString(MessageThreadLoader.INDEX_BODY);
        final long createdUtc = cursor.getLong(MessageThreadLoader.INDEX_CREATED_UTC);
        final String destination = null;
        final String domain = null;
        final int downs = 0;
        final boolean expanded = true;
        final boolean isNew = false;
        final int kind = cursor.getInt(MessageThreadLoader.INDEX_KIND);
        final int likes = 0;
        final String linkTitle = null;
        final int nesting = 0;
        final int numComments = 0;
        final boolean over18 = false;
        final String parentSubreddit = null;
        final int score = 0;
        final String subreddit = null;
        final int thingBodyWidth = 0;
        final int ups = 0;

        // Only show the subject on the header message.
        final String title = cursor.getPosition() == 0
                ? cursor.getString(MessageThreadLoader.INDEX_SUBJECT) : null;

        final boolean drawVotingArrows = false;
        final boolean showThumbnail = false;
        final boolean showStatusPoints = false;

        ThingView tv = (ThingView) view;
        tv.setType(ThingView.TYPE_MESSAGE_THREAD_LIST);
        tv.setData(author,
                body,
                createdUtc,
                destination,
                domain,
                downs,
                expanded,
                isNew,
                kind,
                likes,
                linkTitle,
                nesting,
                nowTimeMs,
                numComments,
                over18,
                parentSubreddit,
                score,
                subreddit,
                thingBodyWidth,
                thingId,
                title,
                ups,
                drawVotingArrows,
                showThumbnail,
                showStatusPoints,
                formatter);
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountName() {
        return accountName;
    }

    // Getters for attributes

    public String getAuthor(int position) {
        return getString(position, MessageThreadLoader.INDEX_AUTHOR);
    }

    public String getThingId(int position) {
        return getString(position, MessageThreadLoader.INDEX_THING_ID);
    }
}
