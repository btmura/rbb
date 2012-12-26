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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Objects;

public class ThingAdapter extends BaseCursorAdapter {

    public static final String TAG = "ThingAdapter";

    /** String argument specifying the account being used. */
    public static final String ARG_ACCOUNT_NAME = "accountName";

    /** String argument specifying session ID of the data. */
    public static final String ARG_SESSION_ID = "sessionId";

    /** String argument specifying the subreddit to load. */
    public static final String ARG_SUBREDDIT = "subreddit";

    /** String argument specifying the search query to use. */
    public static final String ARG_QUERY = "query";

    /** String argument specifying the profileUser profile to load. */
    public static final String ARG_PROFILE_USER = "profileUser";

    /** String argument specifying whose messages to load. */
    public static final String ARG_MESSAGE_USER = "messageUser";

    /** String argument specifying what specific message to load. */
    public static final String ARG_MESSAGE_THREAD_ID = "messageThreadId";

    /** Integer argument to filter things, profile, or messages. */
    public static final String ARG_FILTER = "filter";

    /** String argument that is used to paginate things. */
    public static final String ARG_MORE = "more";

    /** Boolean argument to tell some loaders to fetch stuff. */
    public static final String ARG_REFRESH = "refresh";

    /**
     * {@link ProviderAdapter} allows different providers to be used by
     * {@link ThingProvider} to create {@link ThingView}s.
     */
    static abstract class ProviderAdapter {

        abstract Uri getLoaderUri(Bundle args);

        abstract Loader<Cursor> getLoader(Context context, Uri uri, Bundle args);

        abstract boolean isLoadable(Bundle args);

        abstract String createSessionId(Bundle args);

        abstract void deleteSessionData(Context context, Bundle args);

        abstract String getThingId(ThingAdapter adapter, int position);

        abstract String getLinkId(ThingAdapter adapter, int position);

        abstract String getAuthor(ThingAdapter adapter, int position);

        abstract boolean isSaved(ThingAdapter adapter, int position);

        abstract String getTitle(ThingAdapter adapter, int position);

        abstract CharSequence getUrl(ThingAdapter adapter, int position);

        abstract int getKind(ThingAdapter adapter, int position);

        abstract Bundle getReplyExtras(ThingAdapter adapter, Bundle args, int position);

        abstract String getMoreThingId(ThingAdapter adapter);

        abstract void bindThingView(ThingAdapter adapter, View view, Context context, Cursor c);

        abstract Bundle makeThingBundle(Context context, Cursor cursor);

        static String getAccountName(Bundle args) {
            return args.getString(ARG_ACCOUNT_NAME);
        }

        static long getSessionId(Bundle args) {
            return args.getLong(ARG_SESSION_ID, -1);
        }

        static String getSubreddit(Bundle args) {
            return args.getString(ARG_SUBREDDIT);
        }

        static String getQuery(Bundle args) {
            return args.getString(ARG_QUERY);
        }

        static String getProfileUser(Bundle args) {
            return args.getString(ARG_PROFILE_USER);
        }

        static String getMessageUser(Bundle args) {
            return args.getString(ARG_MESSAGE_USER);
        }

        static String getMessageThreadId(Bundle args) {
            return args.getString(ARG_MESSAGE_THREAD_ID);
        }

        static int getFilter(Bundle args) {
            return args.getInt(ARG_FILTER);
        }

        static String getMore(Bundle args) {
            return args.getString(ARG_MORE);
        }

        static boolean getRefresh(Bundle args) {
            return args.getBoolean(ARG_REFRESH);
        }
    }

    private final LayoutInflater inflater;
    private final ProviderAdapter providerAdapter;

    // Package private variables for ProviderAdapters to access.

    final ThumbnailLoader thumbnailLoader = new ThumbnailLoader();
    final long nowTimeMs = System.currentTimeMillis();
    String accountName;
    String parentSubreddit;
    boolean singleChoice;
    OnVoteListener listener;
    int thingBodyWidth;

    String selectedThingId;
    String selectedLinkId;

    public static ThingAdapter newThingInstance(Context context) {
        return new ThingAdapter(context, new ThingProviderAdapter());
    }

    public static ThingAdapter newMessageInstance(Context context) {
        return new ThingAdapter(context, new MessageProviderAdapter());
    }

    public static ThingAdapter newMessageThreadInstance(Context context) {
        return new ThingAdapter(context, new MessageThreadProviderAdapter());
    }

    private ThingAdapter(Context context, ProviderAdapter providerAdapter) {
        super(context, null, 0);
        this.inflater = LayoutInflater.from(context);
        this.providerAdapter = providerAdapter;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setParentSubreddit(String parentSubreddit) {
        this.parentSubreddit = parentSubreddit;
    }

    public void setSingleChoice(boolean singleChoice) {
        this.singleChoice = singleChoice;
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    public Loader<Cursor> createLoader(Context context, Bundle args) {
        Uri uri = providerAdapter.getLoaderUri(args);
        return providerAdapter.getLoader(context, uri, args);
    }

    public void updateLoader(Context context, Loader<Cursor> loader, Bundle args) {
        if (loader instanceof CursorLoader) {
            Uri newUri = providerAdapter.getLoaderUri(args);
            ((CursorLoader) loader).setUri(newUri);
        }
    }

    public boolean isLoadable(Bundle args) {
        return providerAdapter.isLoadable(args);
    }

    public String createSessionId(Bundle args) {
        return providerAdapter.createSessionId(args);
    }

    public void deleteSessionData(Context context, Bundle args) {
        providerAdapter.deleteSessionData(context, args);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        int kind = providerAdapter.getKind(this, position);
        switch (kind) {
            case Kinds.KIND_MORE:
                return 0;

            default:
                return 1;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int kind = providerAdapter.getKind(this, cursor.getPosition());
        switch (kind) {
            case Kinds.KIND_MORE:
                return inflater.inflate(R.layout.thing_more_row, parent, false);

            default:
                return new ThingView(context);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof ThingView) {
            providerAdapter.bindThingView(this, view, context, cursor);
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
        String thingId = providerAdapter.getThingId(this, position);
        String linkId = providerAdapter.getLinkId(this, position);
        setSelectedThing(thingId, linkId);
    }

    public String getAuthor(int position) {
        return providerAdapter.getAuthor(this, position);
    }

    public boolean isSaved(int position) {
        return providerAdapter.isSaved(this, position);
    }

    public Bundle getReplyExtras(Bundle args, int position) {
        return providerAdapter.getReplyExtras(this, args, position);
    }

    public String getTitle(int position) {
        return providerAdapter.getTitle(this, position);
    }

    public String getThingId(int position) {
        return providerAdapter.getThingId(this, position);
    }

    public CharSequence getUrl(int position) {
        return providerAdapter.getUrl(this, position);
    }

    public String getMoreThingId() {
        return providerAdapter.getMoreThingId(this);
    }

    public Bundle getThingBundle(Context context, int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            return providerAdapter.makeThingBundle(context, c);
        }
        return null;
    }

    public static String getAccountName(Bundle args) {
        return ProviderAdapter.getAccountName(args);
    }

    public static long getSessionId(Bundle args) {
        return ProviderAdapter.getSessionId(args);
    }

    public static String getSubreddit(Bundle args) {
        return ProviderAdapter.getSubreddit(args);
    }

    public static String getQuery(Bundle args) {
        return ProviderAdapter.getQuery(args);
    }

    public static String getProfileUser(Bundle args) {
        return ProviderAdapter.getProfileUser(args);
    }

    public static String getMessageUser(Bundle args) {
        return ProviderAdapter.getMessageUser(args);
    }

    public static String getMessageThreadId(Bundle args) {
        return ProviderAdapter.getMessageThreadId(args);
    }

    public static int getFilter(Bundle args) {
        return ProviderAdapter.getFilter(args);
    }

    public static String getMore(Bundle args) {
        return ProviderAdapter.getMore(args);
    }

    public static boolean getRefresh(Bundle args) {
        return ProviderAdapter.getRefresh(args);
    }
}
