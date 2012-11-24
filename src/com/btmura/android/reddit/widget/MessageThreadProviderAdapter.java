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
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.btmura.android.reddit.widget.ThingAdapter.ProviderAdapter;

public class MessageThreadProviderAdapter extends ProviderAdapter {

    @Override
    Uri getLoaderUri(Bundle args) {
        return null;
    }

    @Override
    Loader<Cursor> getLoader(Context context, Uri uri, Bundle args) {
        return null;
    }

    @Override
    boolean isLoadable(Bundle args) {
        return false;
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
        return null;
    }

    @Override
    String getTitle(ThingAdapter adapter, int position) {
        return null;
    }

    @Override
    String getUrl(ThingAdapter adapter, int position) {
        return null;
    }

    @Override
    int getKind(ThingAdapter adapter, int position) {
        return 0;
    }

    @Override
    String getMoreThingId(ThingAdapter adapter) {
        return null;
    }

    @Override
    void bindThingView(ThingAdapter adapter, View view, Context context, Cursor c) {
    }

    @Override
    Bundle makeThingBundle(Context context, Cursor cursor) {
        return null;
    }
}
