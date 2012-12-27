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
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Objects;

public class ThingLoaderAdapter extends BaseLoaderAdapter {

    private static final String[] PROJECTION = {
            Things._ID,
            Things.COLUMN_AUTHOR,
            Things.COLUMN_BODY,
            Things.COLUMN_CREATED_UTC,
            Things.COLUMN_DOMAIN,
            Things.COLUMN_DOWNS,
            Things.COLUMN_KIND,
            Things.COLUMN_LIKES,
            Things.COLUMN_LINK_ID,
            Things.COLUMN_LINK_TITLE,
            Things.COLUMN_NUM_COMMENTS,
            Things.COLUMN_OVER_18,
            Things.COLUMN_PERMA_LINK,
            Things.COLUMN_SAVED,
            Things.COLUMN_SCORE,
            Things.COLUMN_SELF,
            Things.COLUMN_SUBREDDIT,
            Things.COLUMN_TITLE,
            Things.COLUMN_THING_ID,
            Things.COLUMN_THUMBNAIL_URL,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,

            // Following 2 columns are from joined tables at the end.
            SharedColumns.COLUMN_SAVE,
            SharedColumns.COLUMN_VOTE,
    };

    private static final int INDEX_AUTHOR = 1;
    private static final int INDEX_BODY = 2;
    private static final int INDEX_CREATED_UTC = 3;
    private static final int INDEX_DOMAIN = 4;
    private static final int INDEX_DOWNS = 5;
    private static final int INDEX_KIND = 6;
    private static final int INDEX_LIKES = 7;
    private static final int INDEX_LINK_ID = 8;
    private static final int INDEX_LINK_TITLE = 9;
    private static final int INDEX_NUM_COMMENTS = 10;
    private static final int INDEX_OVER_18 = 11;
    private static final int INDEX_PERMA_LINK = 12;
    private static final int INDEX_SAVED = 13;
    private static final int INDEX_SCORE = 14;
    private static final int INDEX_SELF = 15;
    private static final int INDEX_SUBREDDIT = 16;
    private static final int INDEX_TITLE = 17;
    private static final int INDEX_THING_ID = 18;
    private static final int INDEX_THUMBNAIL_URL = 19;
    private static final int INDEX_UPS = 20;
    private static final int INDEX_URL = 21;

    // Following 2 columns are from joined tables at the end.
    private static final int INDEX_SAVE_ACTION = 22;
    private static final int INDEX_VOTE = 23;

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final String profileUser;

    public ThingLoaderAdapter(Context context, String subreddit, String query, String profileUser) {
        super(context, query);
        this.subreddit = subreddit;
        this.profileUser = profileUser;
    }

    @Override
    public boolean isLoadable() {
        return accountName != null && (subreddit != null || query != null || profileUser != null);
    }

    @Override
    protected Uri getLoaderUri() {
        // Empty but non-null subreddit means front page.
        if (subreddit != null) {
            return ThingProvider.subredditUri(sessionId, accountName, subreddit, filter, more);
        } else if (!TextUtils.isEmpty(profileUser)) {
            return ThingProvider.profileUri(sessionId, accountName, profileUser, filter, more);
        } else if (!TextUtils.isEmpty(query)) {
            return ThingProvider.searchUri(sessionId, accountName, query);
        } else {
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
    public Bundle getReplyExtras(int position) {
        return null;
    }

    @Override
    public String getThingId(int position) {
        return getString(position, INDEX_THING_ID);
    }

    @Override
    public String getLinkId(int position) {
        return getString(position, INDEX_LINK_ID);
    }

    @Override
    public boolean isSaved(int position) {
        return getBoolean(position, INDEX_SAVED)
                || getInt(position, INDEX_SAVE_ACTION) == SaveActions.ACTION_SAVE;
    }

    @Override
    public String getTitle(int position) {
        // Link and comment posts have a title.
        String title = getString(position, INDEX_TITLE);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // CommentActions don't have titles so use the body.
        return getString(position, INDEX_BODY);
    }

    @Override
    public CharSequence getUrl(int position) {
        // Most things and comments have the url attribute set.
        String url = getString(position, INDEX_URL);
        if (!TextUtils.isEmpty(url)) {
            return url;
        }

        // Comment references just provide a thing and link id.
        String thingId = getThingId(position);
        String linkId = getLinkId(position);
        return Urls.commentListing(thingId, linkId, Urls.TYPE_HTML);
    }

    @Override
    public String getMore() {
        Cursor c = getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Kinds.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
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
            String domain = cursor.getString(INDEX_DOMAIN);
            int downs = cursor.getInt(INDEX_DOWNS);
            int kind = cursor.getInt(INDEX_KIND);
            String linkId = cursor.getString(INDEX_LINK_ID);
            String linkTitle = cursor.getString(INDEX_LINK_TITLE);
            int numComments = cursor.getInt(INDEX_NUM_COMMENTS);
            boolean over18 = cursor.getInt(INDEX_OVER_18) == 1;
            int score = cursor.getInt(INDEX_SCORE);
            String subreddit = cursor.getString(INDEX_SUBREDDIT);
            String thingId = cursor.getString(INDEX_THING_ID);
            String thumbnailUrl = cursor.getString(INDEX_THUMBNAIL_URL);
            String title = cursor.getString(INDEX_TITLE);
            int ups = cursor.getInt(INDEX_UPS);

            // CommentActions don't have a score so calculate our own.
            if (kind == Kinds.KIND_COMMENT) {
                score = ups - downs;
            }

            // Reconcile local and remote votes.
            int likes = cursor.getInt(INDEX_LIKES);
            if (!cursor.isNull(INDEX_VOTE)) {
                // Local votes take precedence over those from reddit.
                likes = cursor.getInt(INDEX_VOTE);

                // Modify the score since the vote is still pending and don't go
                // below 0 since reddit doesn't seem to do that.
                score = Math.max(0, score + likes);
            }

            ThingView tv = (ThingView) view;
            tv.setData(accountName, author, body, createdUtc, domain, downs, true, kind,
                    likes, linkTitle, 0, System.currentTimeMillis(), numComments, over18,
                    parentSubreddit, score, subreddit, thingBodyWidth, thingId,
                    thumbnailUrl, title, ups);
            tv.setChosen(singleChoice
                    && Objects.equals(selectedThingId, thingId)
                    && Objects.equals(selectedLinkId, linkId));
            tv.setOnVoteListener(listener);
            thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
        }
    }

    @Override
    protected Bundle makeThingBundle(Context context, Cursor c) {
        Bundle b = new Bundle(6);
        ThingBundle.putSubreddit(b, c.getString(INDEX_SUBREDDIT));
        ThingBundle.putKind(b, c.getInt(INDEX_KIND));

        String title = c.getString(INDEX_TITLE);
        String body = c.getString(INDEX_BODY);
        ThingBundle.putTitle(b, !TextUtils.isEmpty(title) ? title : body);

        String thingId = c.getString(INDEX_THING_ID);
        ThingBundle.putThingId(b, thingId);

        String linkId = c.getString(INDEX_LINK_ID);
        ThingBundle.putLinkId(b, linkId);

        boolean isSelf = c.getInt(INDEX_SELF) == 1;
        if (!isSelf) {
            ThingBundle.putLinkUrl(b, c.getString(INDEX_URL));
        }

        String permaLink = c.getString(INDEX_PERMA_LINK);
        if (!TextUtils.isEmpty(permaLink)) {
            ThingBundle.putCommentUrl(b, Urls.perma(permaLink, null));
        }

        return b;
    }
}
