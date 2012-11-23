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

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;
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
            Things.COLUMN_SCORE,
            Things.COLUMN_SELF,
            Things.COLUMN_SUBREDDIT,
            Things.COLUMN_TITLE,
            Things.COLUMN_THING_ID,
            Things.COLUMN_THUMBNAIL_URL,
            Things.COLUMN_UPS,
            Things.COLUMN_URL,
            Things.COLUMN_VOTE,
    };

    private static int INDEX_AUTHOR = 1;
    private static int INDEX_BODY = 2;
    private static int INDEX_CREATED_UTC = 3;
    private static int INDEX_DOMAIN = 4;
    private static int INDEX_DOWNS = 5;
    private static int INDEX_KIND = 6;
    private static int INDEX_LIKES = 7;
    private static int INDEX_LINK_ID = 8;
    private static int INDEX_LINK_TITLE = 9;
    private static int INDEX_NUM_COMMENTS = 10;
    private static int INDEX_OVER_18 = 11;
    private static int INDEX_PERMA_LINK = 12;
    private static int INDEX_SCORE = 13;
    private static int INDEX_SELF = 14;
    private static int INDEX_SUBREDDIT = 15;
    private static int INDEX_TITLE = 16;
    private static int INDEX_THING_ID = 17;
    private static int INDEX_THUMBNAIL_URL = 18;
    private static int INDEX_UPS = 19;
    private static int INDEX_URL = 20;
    private static int INDEX_VOTE = 21;

    @Override
    Uri getLoaderUri(Bundle args) {
        Uri.Builder b = ThingProvider.THINGS_URI.buildUpon()
                .appendQueryParameter(ThingProvider.PARAM_FETCH, Boolean.toString(getFetch(args)))
                .appendQueryParameter(ThingProvider.PARAM_ACCOUNT, getAccountName(args))
                .appendQueryParameter(ThingProvider.PARAM_SESSION_ID, getSessionId(args))
                .appendQueryParameter(ThingProvider.PARAM_FILTER,
                        Integer.toString(getFilter(args)));

        // Empty but non-null subreddit means front page.
        if (getSubreddit(args) != null) {
            b.appendQueryParameter(ThingProvider.PARAM_SUBREDDIT, getSubreddit(args));
        }

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
        return new CursorLoader(context, uri, PROJECTION, Things.SELECT_BY_SESSION_ID,
                Array.of(getSessionId(args)), null);
    }

    @Override
    boolean isLoadable(Bundle args) {
        return getAccountName(args) != null
                && getSessionId(args) != null
                && (getSubreddit(args) != null
                        || getQuery(args) != null
                        || getProfileUser(args) != null);
    }

    @Override
    String createSessionId(Bundle args) {
        if (!TextUtils.isEmpty(getProfileUser(args))) {
            return getProfileUser(args) + "-" + System.currentTimeMillis();
        } else if (!TextUtils.isEmpty(getQuery(args))) {
            return getQuery(args) + "-" + System.currentTimeMillis();
        } else if (getSubreddit(args) != null) {
            return getSubreddit(args) + "-" + System.currentTimeMillis();
        } else {
            return null;
        }
    }

    @Override
    void deleteSessionData(Context context, final Bundle args) {
        // Use application context to allow activity to be collected and
        // schedule the session deletion in the background thread pool rather
        // than serial pool.
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentResolver cr = appContext.getContentResolver();
                cr.delete(ThingProvider.THINGS_URI, Things.SELECT_BY_SESSION_ID,
                        Array.of(getSessionId(args)));
            }
        });
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
    int getKind(ThingAdapter adapter, int position) {
        return adapter.getInt(position, INDEX_KIND);
    }

    @Override
    String getMoreThingId(ThingAdapter adapter) {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Things.KIND_MORE) {
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
        if (kind == Things.KIND_COMMENT) {
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
        tv.setData(adapter.accountName, author, body, createdUtc, domain, downs, kind, likes,
                linkTitle, adapter.nowTimeMs, numComments, over18, adapter.parentSubreddit, score,
                subreddit,
                adapter.thingBodyWidth, thingId, thumbnailUrl, title, ups);
        tv.setChosen(adapter.singleChoice
                && Objects.equals(adapter.selectedThingId, thingId)
                && Objects.equals(adapter.selectedLinkId, linkId));
        tv.setOnVoteListener(adapter.listener);
        adapter.thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
    }

    @Override
    Bundle makeThingBundle(Cursor c) {
        Bundle b = new Bundle(PROJECTION.length);
        b.putLong(Things._ID, c.getLong(INDEX_THING_ID));
        b.putString(Things.COLUMN_AUTHOR, c.getString(INDEX_AUTHOR));
        b.putString(Things.COLUMN_BODY, c.getString(INDEX_BODY));
        b.putLong(Things.COLUMN_CREATED_UTC, c.getLong(INDEX_CREATED_UTC));
        b.putString(Things.COLUMN_DOMAIN, c.getString(INDEX_DOMAIN));
        b.putInt(Things.COLUMN_DOWNS, c.getInt(INDEX_DOWNS));
        b.putInt(Things.COLUMN_LIKES, c.getInt(INDEX_LIKES));
        b.putInt(Things.COLUMN_KIND, c.getInt(INDEX_KIND));
        b.putString(Things.COLUMN_LINK_ID, c.getString(INDEX_LINK_ID));
        b.putString(Things.COLUMN_LINK_TITLE, c.getString(INDEX_LINK_TITLE));
        b.putInt(Things.COLUMN_NUM_COMMENTS, c.getInt(INDEX_NUM_COMMENTS));
        b.putBoolean(Things.COLUMN_OVER_18, c.getInt(INDEX_OVER_18) == 1);
        b.putString(Things.COLUMN_PERMA_LINK, c.getString(INDEX_PERMA_LINK));
        b.putInt(Things.COLUMN_SCORE, c.getInt(INDEX_SCORE));
        b.putBoolean(Things.COLUMN_SELF, c.getInt(INDEX_SELF) == 1);
        b.putString(Things.COLUMN_SUBREDDIT, c.getString(INDEX_SUBREDDIT));
        b.putString(Things.COLUMN_TITLE, c.getString(INDEX_TITLE));
        b.putString(Things.COLUMN_THING_ID, c.getString(INDEX_THING_ID));
        b.putString(Things.COLUMN_THUMBNAIL_URL, c.getString(INDEX_THUMBNAIL_URL));
        b.putInt(Things.COLUMN_UPS, c.getInt(INDEX_UPS));
        b.putString(Things.COLUMN_URL, c.getString(INDEX_URL));
        return b;
    }
}
