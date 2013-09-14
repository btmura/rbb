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

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.CommentLoader;
import com.btmura.android.reddit.text.Formatter;

public class CommentAdapter extends BaseCursorAdapter {

    private final Formatter formatter = new Formatter();
    private final long nowTimeMs = System.currentTimeMillis();

    private final String accountName;
    private final OnVoteListener listener;

    public CommentAdapter(Context context, String accountName, OnVoteListener listener) {
        super(context, null, 0);
        this.accountName = accountName;
        this.listener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(CommentLoader.INDEX_AUTHOR);
        final String body = cursor.getString(CommentLoader.INDEX_BODY);
        final long createdUtc = cursor.getLong(CommentLoader.INDEX_CREATED_UTC);
        final String destination = null; // Only messages have destinations.
        final String domain = null; // Only posts have domains.
        final int downs = cursor.getInt(CommentLoader.INDEX_DOWNS);
        final boolean expanded = cursor.getInt(CommentLoader.INDEX_EXPANDED) == 1;
        final boolean isNew = false; // Only messages might be new.
        final int kind = cursor.getInt(CommentLoader.INDEX_KIND);
        final String linkTitle = null; // Only post replies have link titles.
        final int nesting = cursor.getInt(CommentLoader.INDEX_NESTING);
        final int numComments = cursor.getInt(CommentLoader.INDEX_NUM_COMMENTS);
        final boolean over18 = cursor.getInt(CommentLoader.INDEX_OVER_18) != 0;
        final String parentSubreddit = null; // Comments don't have parent subreddits.
        final String subreddit = null; // Comments don't have subreddits.
        final String title = cursor.getString(CommentLoader.INDEX_TITLE);
        final int thingBodyWidth = 0; // Use the full width all the time.
        final String thingId = cursor.getString(CommentLoader.INDEX_THING_ID);
        final int ups = cursor.getInt(CommentLoader.INDEX_UPS);

        // CommentActions don't have a score so calculate our own.
        int score = ups - downs;

        // TODO: Remove code duplication with ThingAdapter.
        // Local votes take precedence over those from reddit.
        int likes = cursor.getInt(CommentLoader.INDEX_LIKES);
        if (!cursor.isNull(CommentLoader.INDEX_VOTE_ACTION)) {
            // Local votes take precedence over those from reddit.
            likes = cursor.getInt(CommentLoader.INDEX_VOTE_ACTION);

            // Modify the score since the vote is still pending.
            score += likes;
        }

        final boolean drawVotingArrows = AccountUtils.isAccount(accountName);
        final boolean showThumbnail = false;
        final boolean showStatusPoints = !AccountUtils.isAccount(accountName)
                || cursor.getPosition() != 0;

        ThingView tv = (ThingView) view;
        tv.setType(ThingView.TYPE_COMMENT_LIST);
        tv.setData(accountName,
                author,
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
        tv.setOnVoteListener(listener);
    }
}
