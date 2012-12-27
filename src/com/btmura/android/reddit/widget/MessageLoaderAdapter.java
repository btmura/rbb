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
import android.view.ViewGroup;

import com.btmura.android.reddit.database.Messages;
import com.btmura.android.reddit.provider.ThingProvider;

public class MessageLoaderAdapter extends LoaderAdapter {

    private static final String[] PROJECTION = {
            Messages._ID,
            Messages.COLUMN_AUTHOR,
            Messages.COLUMN_BODY,
            Messages.COLUMN_CREATED_UTC,
            Messages.COLUMN_KIND,
    };

    private static final int INDEX_AUTHOR = 1;
    private static final int INDEX_BODY = 2;
    private static final int INDEX_CREATED_UTC = 3;
    private static final int INDEX_KIND = 4;

    public MessageLoaderAdapter(Context context) {
        super(context);
    }

    public boolean isLoadable() {
        return getAccountName() != null && getThingId() != null;
    }

    public Loader<Cursor> getLoader(Context context, Bundle args) {
        Uri uri = ThingProvider.messageThreadUri(getSessionId(), getAccountName(), getThingId());
        return new CursorLoader(context, uri, PROJECTION, null, null, null);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new ThingView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String author = cursor.getString(INDEX_AUTHOR);
        String body = cursor.getString(INDEX_BODY);
        long createdUtc = cursor.getLong(INDEX_CREATED_UTC);
        int kind = cursor.getInt(INDEX_KIND);

        ThingView tv = (ThingView) view;
        tv.setData(getAccountName(), author, body, createdUtc, null, 0, true, kind, 0,
                null, 0, System.currentTimeMillis(), 0, false, null, 0, null,
                0, null, null, null, 0);
    }
}
