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
import android.view.View;

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.MessageProvider;
import com.btmura.android.reddit.widget.ThingAdapter.ProviderAdapter;

public class MessageThreadProviderAdapter extends ProviderAdapter {

    private static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_KIND,
            Messages.COLUMN_SESSION_ID,
            Messages.COLUMN_THING_ID,
    };

    private static final int INDEX_AUTHOR = 1;
    private static final int INDEX_BODY = 2;
    private static final int INDEX_CREATED_UTC = 3;
    private static final int INDEX_KIND = 4;
    private static final int INDEX_SESSION_ID = 5;
    private static final int INDEX_THING_ID = 6;

    public static final String EXTRA_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_THING_ID = "thingId";

    @Override
    Uri getLoaderUri(Bundle args) {
        return MessageProvider.MESSAGES_URI.buildUpon()
                .appendPath(getMessageThreadId(args))
                .appendQueryParameter(MessageProvider.PARAM_FETCH, Boolean.toString(true))
                .appendQueryParameter(MessageProvider.PARAM_ACCOUNT, getAccountName(args))
                .build();
    }

    @Override
    Loader<Cursor> getLoader(Context context, Uri uri, Bundle args) {
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    @Override
    boolean isLoadable(Bundle args) {
        return getAccountName(args) != null && getMessageThreadId(args) != null;
    }

    @Override
    String createSessionId(Bundle args) {
        return null;
    }

    @Override
    void deleteSessionData(Context context, Bundle args) {
    }

    @Override
    String getThingId(ThingAdapter adapter, int position) {
        return null;
    }

    @Override
    String getLinkId(ThingAdapter adapter, int position) {
        return null;
    }

    @Override
    String getAuthor(ThingAdapter adapter, int position) {
        return adapter.getString(position, INDEX_AUTHOR);
    }

    @Override
    String getTitle(ThingAdapter adapter, int position) {
        return adapter.getString(position, INDEX_BODY);
    }

    @Override
    String getUrl(ThingAdapter adapter, int position) {
        return null;
    }

    @Override
    int getKind(ThingAdapter adapter, int position) {
        return adapter.getInt(position, INDEX_KIND);
    }

    @Override
    Bundle getReplyExtras(ThingAdapter adapter, Bundle args, int position) {
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_PARENT_THING_ID, getMessageThreadId(args));
        extras.putLong(EXTRA_SESSION_ID, adapter.getLong(position, INDEX_SESSION_ID));
        extras.putString(EXTRA_THING_ID, adapter.getString(position, INDEX_THING_ID));
        return extras;
    }

    @Override
    String getMoreThingId(ThingAdapter adapter) {
        return null;
    }

    @Override
    void bindThingView(ThingAdapter adapter, View view, Context context, Cursor cursor) {
        String author = cursor.getString(INDEX_AUTHOR);
        String body = cursor.getString(INDEX_BODY);
        long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int kind = cursor.getInt(INDEX_KIND);

        ThingView tv = (ThingView) view;
        tv.setData(adapter.accountName, author, body, createdUtc, null, 0, true, kind, 0,
                null, 0, adapter.nowTimeMs, 0, false, adapter.parentSubreddit, 0, null,
                adapter.thingBodyWidth, null, null, null, 0);
        tv.setOnVoteListener(adapter.listener);
    }

    @Override
    Bundle makeThingBundle(Context context, Cursor cursor) {
        return null;
    }
}
