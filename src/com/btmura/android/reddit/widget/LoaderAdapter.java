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

public abstract class LoaderAdapter extends BaseCursorAdapter {

    public LoaderAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    public abstract boolean isLoadable();

    public Loader<Cursor> getLoader(Context context) {
        return new CursorLoader(context, getLoaderUri(), getProjection(), getSelection(),
                getSelectionArgs(), getSortOrder());
    }

    public void updateLoaderUri(Context context, Loader<Cursor> loader) {
        ((CursorLoader) loader).setUri(getLoaderUri());
    }

    protected abstract Uri getLoaderUri();

    protected String[] getProjection() {
        return null;
    }

    protected String getSelection() {
        return null;
    }

    protected String[] getSelectionArgs() {
        return null;
    }

    protected String getSortOrder() {
        return null;
    }
}
