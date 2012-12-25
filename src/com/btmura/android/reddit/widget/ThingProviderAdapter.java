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
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Saves;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.ThingAdapter.ProviderAdapter;

class ThingProviderAdapter extends ProviderAdapter {

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
            Saves.COLUMN_ACTION,
            Votes.COLUMN_VOTE,
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

    // Following 2 colums are from joined tables at the end.
    private static final int INDEX_SAVE_ACTION = 22;
    private static final int INDEX_VOTE = 23;

    @Override
    Uri getLoaderUri(Bundle args) {
        Uri.Builder b = null;

        // Empty but non-null subreddit means front page.
        if (getSubreddit(args) != null) {
            b = ThingProvider.SUBREDDIT_URI.buildUpon();
            b.appendPath(getSubreddit(args));
        } else if (!TextUtils.isEmpty(getProfileUser(args))) {
            b = ThingProvider.USER_URI.buildUpon();
            b.appendPath(getProfileUser(args));
        }

        b.appendQueryParameter(ThingProvider.PARAM_FETCH,
                Boolean.toString(getFetch(args)))
                .appendQueryParameter(ThingProvider.PARAM_ACCOUNT,
                        getAccountName(args))
                .appendQueryParameter(ThingProvider.PARAM_FILTER,
                        Integer.toString(getFilter(args)))
                .appendQueryParameter(ThingProvider.PARAM_JOIN,
                        Boolean.toString(true));

        // All other parameters must be non-null and not empty.
        if (!TextUtils.isEmpty(getQuery(args))) {
            b.appendQueryParameter(ThingProvider.PARAM_QUERY, getQuery(args));
        }
        if (!TextUtils.isEmpty(getProfileUser(args))) {
            b.appendQueryParameter(ThingProvider.PARAM_PROFILE_USER, getProfileUser(args));
        }
        if (!TextUtils.isEmpty(getMessageUser(args))) {
            b.appendQueryParameter(ThingProvider.PARAM_MESSAGE_USER, getMessageUser(args));
        }
        if (!TextUtils.isEmpty(getMore(args))) {
            b.appendQueryParameter(ThingProvider.PARAM_MORE, getMore(args));
        }
        return b.build();
    }

    @Override
    Loader<Cursor> getLoader(Context context, Uri uri, Bundle args) {
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    @Override
    boolean isLoadable(Bundle args) {
        return getAccountName(args) != null
                && (getSubreddit(args) != null
                        || getQuery(args) != null
                        || getProfileUser(args) != null);
    }

    @Override
    String createSessionId(Bundle args) {
        return null;
    }

    @Override
    void deleteSessionData(Context context, final Bundle args) {
    }

    @Override
    String getThingId(ThingAdapter adapter, int position) {
        return adapter.getString(position, INDEX_THING_ID);
    }

    @Override
    String getLinkId(ThingAdapter adapter, int position) {
        return adapter.getString(position, INDEX_LINK_ID);
    }

    @Override
    String getAuthor(ThingAdapter adapter, int position) {
        return adapter.getString(position, INDEX_AUTHOR);
    }

    @Override
    boolean isSaved(ThingAdapter adapter, int position) {
        return adapter.getBoolean(position, INDEX_SAVED)
                || adapter.getInt(position, INDEX_SAVE_ACTION) == Saves.ACTION_SAVE;
    }

    @Override
    String getTitle(ThingAdapter adapter, int position) {
        // Link and comment posts have a title.
        String title = adapter.getString(position, INDEX_TITLE);
        if (!TextUtils.isEmpty(title)) {
            return title;
        }

        // Comments don't have titles so use the body.
        return adapter.getString(position, INDEX_BODY);
    }

    @Override
    CharSequence getUrl(ThingAdapter adapter, int position) {
        // Most things and comments have the url attribute set.
        String url = adapter.getString(position, INDEX_URL);
        if (!TextUtils.isEmpty(url)) {
            return url;
        }

        // Comment references just provide a thing and link id.
        String thingId = adapter.getString(position, INDEX_THING_ID);
        String linkId = adapter.getString(position, INDEX_LINK_ID);
        return Urls.commentListing(thingId, linkId, Urls.TYPE_HTML);
    }

    @Override
    int getKind(ThingAdapter adapter, int position) {
        return adapter.getInt(position, INDEX_KIND);
    }

    @Override
    Bundle getReplyExtras(ThingAdapter adapter, Bundle args, int position) {
        return null;
    }

    @Override
    String getMoreThingId(ThingAdapter adapter) {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Kinds.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
    }

    @Override
    void bindThingView(ThingAdapter adapter, View view, Context context, Cursor cursor) {
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

        // Comments don't have a score so calculate our own.
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
        tv.setData(adapter.accountName, author, body, createdUtc, domain, downs, true, kind,
                likes, linkTitle, 0, adapter.nowTimeMs, numComments, over18,
                adapter.parentSubreddit, score, subreddit, adapter.thingBodyWidth, thingId,
                thumbnailUrl, title, ups);
        tv.setChosen(adapter.singleChoice
                && Objects.equals(adapter.selectedThingId, thingId)
                && Objects.equals(adapter.selectedLinkId, linkId));
        tv.setOnVoteListener(adapter.listener);
        adapter.thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
    }

    @Override
    Bundle makeThingBundle(Context context, Cursor c) {
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
