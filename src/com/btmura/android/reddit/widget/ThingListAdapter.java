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
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.util.Objects;

public class ThingListAdapter extends AbstractThingListAdapter implements ThingProjection {

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
        final String author = cursor.getString(ThingProjection.INDEX_AUTHOR);
        final String body = cursor.getString(ThingProjection.INDEX_BODY);
        final long createdUtc = cursor.getLong(ThingProjection.INDEX_CREATED_UTC);
        final String destination = null; // Only messages have destinations.
        final String domain = cursor.getString(ThingProjection.INDEX_DOMAIN);
        final int downs = cursor.getInt(ThingProjection.INDEX_DOWNS);
        final boolean expanded = true; // Expanded only for comments handled by different adapter.
        final int kind = cursor.getInt(ThingProjection.INDEX_KIND);
        final String linkId = cursor.getString(ThingProjection.INDEX_LINK_ID);
        final String linkTitle = cursor.getString(ThingProjection.INDEX_LINK_TITLE);
        final int nesting = 0; // Nesting only for comments handled by different adapter.
        final int numComments = cursor.getInt(ThingProjection.INDEX_NUM_COMMENTS);
        final boolean over18 = cursor.getInt(ThingProjection.INDEX_OVER_18) == 1;
        final String subreddit = cursor.getString(ThingProjection.INDEX_SUBREDDIT);
        final String thingId = cursor.getString(ThingProjection.INDEX_THING_ID);
        final String thumbnailUrl = cursor.getString(ThingProjection.INDEX_THUMBNAIL_URL);
        final String title = cursor.getString(ThingProjection.INDEX_TITLE);
        final int ups = cursor.getInt(ThingProjection.INDEX_UPS);

        // Comments don't have a score so calculate our own.
        int score = cursor.getInt(ThingProjection.INDEX_SCORE);
        if (kind == Kinds.KIND_COMMENT) {
            score = ups - downs;
        }

        // Reconcile local and remote votes.
        int likes = cursor.getInt(ThingProjection.INDEX_LIKES);
        if (!cursor.isNull(ThingProjection.INDEX_VOTE_ACTION)) {
            // Local votes take precedence over those from reddit.
            likes = cursor.getInt(ThingProjection.INDEX_VOTE_ACTION);

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

    public ThingBundle getThingBundle(int position) {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            if (cursor.isNull(INDEX_LINK_ID)) {
                return ThingBundle.newLinkInstance(cursor.getString(INDEX_AUTHOR),
                        cursor.getLong(INDEX_CREATED_UTC),
                        cursor.getString(INDEX_DOMAIN),
                        cursor.getInt(INDEX_DOWNS),
                        cursor.getInt(INDEX_LIKES),
                        cursor.getInt(INDEX_KIND),
                        cursor.getInt(INDEX_NUM_COMMENTS),
                        cursor.getInt(INDEX_OVER_18) == 1,
                        cursor.getString(INDEX_PERMA_LINK),
                        cursor.getInt(INDEX_SAVED) == 1,
                        cursor.getInt(INDEX_SCORE),
                        cursor.getInt(INDEX_SELF) == 1,
                        cursor.getString(INDEX_SUBREDDIT),
                        cursor.getString(INDEX_THING_ID),
                        cursor.getString(INDEX_THUMBNAIL_URL),
                        cursor.getString(INDEX_TITLE),
                        cursor.getInt(INDEX_UPS),
                        cursor.getString(INDEX_URL));
            } else {
                return ThingBundle.newCommentInstance(cursor.getString(INDEX_AUTHOR),
                        cursor.getLong(INDEX_CREATED_UTC),
                        cursor.getString(INDEX_DOMAIN),
                        cursor.getInt(INDEX_DOWNS),
                        cursor.getInt(INDEX_LIKES),
                        cursor.getInt(INDEX_KIND),
                        cursor.getString(INDEX_LINK_ID),
                        cursor.getString(INDEX_LINK_TITLE),
                        cursor.getInt(INDEX_NUM_COMMENTS),
                        cursor.getInt(INDEX_OVER_18) == 1,
                        cursor.getString(INDEX_PERMA_LINK),
                        cursor.getInt(INDEX_SAVED) == 1,
                        cursor.getInt(INDEX_SCORE),
                        cursor.getInt(INDEX_SELF) == 1,
                        cursor.getString(INDEX_SUBREDDIT),
                        cursor.getString(INDEX_THING_ID),
                        cursor.getString(INDEX_THUMBNAIL_URL),
                        cursor.getString(INDEX_TITLE),
                        cursor.getInt(INDEX_UPS),
                        cursor.getString(INDEX_URL));
            }
        }
        return null;
    }

    @Override
    int getKindIndex() {
        return ThingProjection.INDEX_KIND;
    }

    @Override
    String getLinkId(int position) {
        return getString(position, ThingProjection.INDEX_LINK_ID);
    }

    @Override
    String getThingId(int position) {
        return getString(position, ThingProjection.INDEX_THING_ID);
    }
}
