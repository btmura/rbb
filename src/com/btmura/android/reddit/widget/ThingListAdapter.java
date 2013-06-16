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
import android.text.TextUtils;
import android.view.View;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AbstractThingLoader;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Objects;

public class ThingListAdapter extends AbstractThingListAdapter {

    public static final String TAG = "ThingListAdapter";

    private static final int[] LINK_DETAILS = {
            ThingView.DETAIL_UP_VOTES,
            ThingView.DETAIL_DOWN_VOTES,
            ThingView.DETAIL_DOMAIN,
    };

    private static final int[] COMMENT_DETAILS = {
            ThingView.DETAIL_UP_VOTES,
            ThingView.DETAIL_DOWN_VOTES,
            ThingView.DETAIL_SUBREDDIT,
    };

    private final Formatter formatter = new Formatter();
    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final OnVoteListener listener;

    private String parentSubreddit;
    private String subreddit;

    public ThingListAdapter(Context context, OnVoteListener listener, boolean singleChoice) {
        super(context, singleChoice);
        this.listener = listener;
    }

    public void setParentSubreddit(String parentSubreddit) {
        this.parentSubreddit = parentSubreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getParentSubreddit() {
        return parentSubreddit;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public boolean isSingleChoice() {
        return singleChoice;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            bindThingView(view, context, cursor);
        }
    }

    private void bindThingView(View view, Context context, Cursor cursor) {
        final String author = cursor.getString(AbstractThingLoader.INDEX_AUTHOR);
        final String body = cursor.getString(AbstractThingLoader.INDEX_BODY);
        final long createdUtc = cursor.getLong(AbstractThingLoader.INDEX_CREATED_UTC);
        final String destination = null; // Only messages have destinations.
        final String domain = cursor.getString(AbstractThingLoader.INDEX_DOMAIN);
        final int downs = cursor.getInt(AbstractThingLoader.INDEX_DOWNS);
        final boolean expanded = true; // Expanded only for comments handled by different adapter.
        final int kind = cursor.getInt(AbstractThingLoader.INDEX_KIND);
        final String linkId = cursor.getString(AbstractThingLoader.INDEX_LINK_ID);
        final String linkTitle = cursor.getString(AbstractThingLoader.INDEX_LINK_TITLE);
        final int nesting = 0; // Nesting only for comments handled by different adapter.
        final int numComments = cursor.getInt(AbstractThingLoader.INDEX_NUM_COMMENTS);
        final boolean over18 = cursor.getInt(AbstractThingLoader.INDEX_OVER_18) == 1;
        final String subreddit = cursor.getString(AbstractThingLoader.INDEX_SUBREDDIT);
        final String thingId = cursor.getString(AbstractThingLoader.INDEX_THING_ID);
        final String thumbnailUrl = cursor.getString(AbstractThingLoader.INDEX_THUMBNAIL_URL);
        final String title = cursor.getString(AbstractThingLoader.INDEX_TITLE);
        final int ups = cursor.getInt(AbstractThingLoader.INDEX_UPS);

        // Comments don't have a score so calculate our own.
        int score = cursor.getInt(AbstractThingLoader.INDEX_SCORE);
        if (kind == Kinds.KIND_COMMENT) {
            score = ups - downs;
        }

        // Reconcile local and remote votes.
        int likes = cursor.getInt(AbstractThingLoader.INDEX_LIKES);
        if (!cursor.isNull(AbstractThingLoader.THING_VOTE_ACTION)) {
            // Local votes take precedence over those from reddit.
            likes = cursor.getInt(AbstractThingLoader.THING_VOTE_ACTION);

            // Modify the score since the vote is still pending and don't go
            // below 0 since reddit doesn't seem to do that.
            score = Math.max(0, score + likes);
        }

        final boolean drawVotingArrows = AccountUtils.isAccount(accountName)
                && kind != Kinds.KIND_MESSAGE;
        final boolean showThumbnail = !TextUtils.isEmpty(thumbnailUrl);
        final boolean showStatusPoints = !AccountUtils.isAccount(accountName)
                || kind == Kinds.KIND_COMMENT;

        ThingView tv = (ThingView) view;
        tv.setBody(body, false, formatter);
        tv.setData(accountName,
                author,
                createdUtc,
                destination,
                domain,
                downs,
                expanded,
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
                showStatusPoints);
        tv.setChosen(singleChoice
                && Objects.equals(selectedThingId, thingId)
                && Objects.equals(selectedLinkId, linkId));
        tv.setOnVoteListener(listener);
        setThingDetails(tv, kind);
        thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
    }

    private void setThingDetails(ThingView tv, int kind) {
        switch (kind) {
            case Kinds.KIND_LINK:
                tv.setDetails(LINK_DETAILS);
                break;

            case Kinds.KIND_COMMENT:
                tv.setDetails(COMMENT_DETAILS);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    int getAuthorIndex() {
        return AbstractThingLoader.INDEX_AUTHOR;
    }

    @Override
    int getKindIndex() {
        return AbstractThingLoader.INDEX_KIND;
    }

    @Override
    String getLinkId(int position) {
        return getString(position, AbstractThingLoader.INDEX_LINK_ID);
    }

    @Override
    String getThingId(int position) {
        return getString(position, AbstractThingLoader.INDEX_THING_ID);
    }
}
