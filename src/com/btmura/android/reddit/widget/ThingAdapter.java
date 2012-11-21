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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.util.Objects;

public class ThingAdapter extends BaseCursorAdapter {

    public static final String TAG = "ThingAdapter";

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

    public static int INDEX_ID = 0;
    public static int INDEX_AUTHOR = 1;
    public static int INDEX_BODY = 2;
    public static int INDEX_CREATED_UTC = 3;
    public static int INDEX_DOMAIN = 4;
    public static int INDEX_DOWNS = 5;
    public static int INDEX_KIND = 6;
    public static int INDEX_LIKES = 7;
    public static int INDEX_LINK_ID = 8;
    public static int INDEX_NUM_COMMENTS = 9;
    public static int INDEX_OVER_18 = 10;
    public static int INDEX_PERMA_LINK = 11;
    public static int INDEX_SCORE = 12;
    public static int INDEX_SELF = 13;
    public static int INDEX_SUBREDDIT = 14;
    public static int INDEX_TITLE = 15;
    public static int INDEX_THING_ID = 16;
    public static int INDEX_THUMBNAIL_URL = 17;
    public static int INDEX_UPS = 18;
    public static int INDEX_URL = 19;
    public static int INDEX_VOTE = 20;

    private final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    private final long nowTimeMs = System.currentTimeMillis();
    private final LayoutInflater inflater;
    private final String parentSubreddit;
    private final OnVoteListener listener;
    private String accountName;
    private String selectedThingId;
    private String selectedLinkId;
    private int thingBodyWidth;
    private boolean singleChoice;

    public static Loader<Cursor> getLoader(Context context, String accountName, String sessionId,
            String subreddit, String query, String user, int filter, String more, boolean sync) {
        Uri uri = getUri(accountName, sessionId, subreddit, query, user, filter, more, sync);
        return new CursorLoader(context, uri, PROJECTION, Things.SELECT_BY_SESSION_ID,
                Array.of(sessionId), null);
    }

    public static void updateLoader(Context context, String accountName, String sessionId,
            String subreddit, String query, String user, int filter, String more, boolean sync,
            Loader<Cursor> loader) {
        if (loader instanceof CursorLoader) {
            CursorLoader cl = (CursorLoader) loader;
            cl.setUri(getUri(accountName, sessionId, subreddit, query, user, filter, more, sync));
        }
    }

    public static void deleteSessionData(final Context context, final String sessionId) {
        // Use application context to allow activity to be collected and
        // schedule the session deletion in the background thread pool rather
        // than serial pool.
        final Context appContext = context.getApplicationContext();
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            public void run() {
                ContentResolver cr = appContext.getContentResolver();
                cr.delete(ThingProvider.THINGS_URI, Things.SELECT_BY_SESSION_ID,
                        Array.of(sessionId));
            }
        });
    }

    private static Uri getUri(String accountName, String sessionId, String subreddit,
            String query, String user, int filter, String more, boolean fetch) {
        Uri.Builder b = ThingProvider.THINGS_URI.buildUpon()
                .appendQueryParameter(ThingProvider.PARAM_FETCH, Boolean.toString(fetch))
                .appendQueryParameter(ThingProvider.PARAM_ACCOUNT, accountName)
                .appendQueryParameter(ThingProvider.PARAM_SESSION_ID, sessionId)
                .appendQueryParameter(ThingProvider.PARAM_FILTER, Integer.toString(filter));
        if (!TextUtils.isEmpty(subreddit)) {
            b.appendQueryParameter(ThingProvider.PARAM_SUBREDDIT, subreddit);
        }
        if (!TextUtils.isEmpty(query)) {
            b.appendQueryParameter(ThingProvider.PARAM_QUERY, query);
        }
        if (!TextUtils.isEmpty(user)) {
            b.appendQueryParameter(ThingProvider.PARAM_USER, user);
        }
        if (!TextUtils.isEmpty(more)) {
            b.appendQueryParameter(ThingProvider.PARAM_MORE, more);
        }
        return b.build();
    }

    public ThingAdapter(Context context, String accountName, String parentSubreddit,
            boolean singleChoice, OnVoteListener listener) {
        super(context, null, 0);
        this.inflater = LayoutInflater.from(context);
        this.accountName = accountName;
        this.parentSubreddit = parentSubreddit;
        this.singleChoice = singleChoice;
        this.listener = listener;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        int kind = getInt(position, INDEX_KIND);
        switch (kind) {
            case Things.KIND_MORE:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int kind = cursor.getInt(INDEX_KIND);
        switch (kind) {
            case Things.KIND_MORE:
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
            tv.setData(accountName, author, body, createdUtc, domain, downs, kind, likes,
                    nowTimeMs, numComments, over18, parentSubreddit, score, subreddit,
                    thingBodyWidth, thingId, thumbnailUrl, title, ups);
            tv.setChosen(singleChoice
                    && Objects.equals(selectedThingId, thingId)
                    && Objects.equals(selectedLinkId, linkId));
            tv.setOnVoteListener(listener);
            thumbnailLoader.setThumbnail(context, tv, thumbnailUrl);
        }
    }

    public String getSelectedThingId() {
        return selectedThingId;
    }

    public String getSelectedLinkId() {
        return selectedLinkId;
    }

    public void setSelectedThing(String thingId, String linkId) {
        if (!Objects.equals(selectedThingId, thingId)
                || !Objects.equals(selectedLinkId, linkId)) {
            selectedThingId = thingId;
            selectedLinkId = linkId;
            notifyDataSetChanged();
        }
    }

    public void setSelectedPosition(int position) {
        String thingId = getString(position, INDEX_THING_ID);
        String linkId = getString(position, INDEX_LINK_ID);
        setSelectedThing(thingId, linkId);
    }

    public String getMoreThingId() {
        Cursor c = getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Things.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
    }

    public Bundle getThingBundle(int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeBundle(c);
        }
        return null;
    }

    private static Bundle makeBundle(Cursor c) {
        Bundle b = new Bundle(PROJECTION.length);
        b.putLong(Things._ID, c.getLong(INDEX_THING_ID));
        b.putString(Things.COLUMN_AUTHOR, c.getString(INDEX_AUTHOR));
        b.putLong(Things.COLUMN_CREATED_UTC, c.getLong(INDEX_CREATED_UTC));
        b.putString(Things.COLUMN_DOMAIN, c.getString(INDEX_DOMAIN));
        b.putInt(Things.COLUMN_DOWNS, c.getInt(INDEX_DOWNS));
        b.putInt(Things.COLUMN_LIKES, c.getInt(INDEX_LIKES));
        b.putInt(Things.COLUMN_KIND, c.getInt(INDEX_KIND));
        b.putString(Things.COLUMN_LINK_ID, c.getString(INDEX_LINK_ID));
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
