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

package com.btmura.android.reddit.database;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * Cursor that deletes data when it is closed. It can be used to show a
 * temporary result set that deletes itself afterwards to avoid leaving residue
 * in the database.
 */
public class SessionCursor extends CursorWrapper {

    public static final String TAG = "SessionCursor";

    private final Context context;
    private final Uri uri;
    private final String selection;
    private final String[] selectionArgs;

    /** Indicates whether to cancel deleting data on close. */
    private boolean cancelDeletion;

    public SessionCursor(Context context, Uri uri, String selection, String[] selectionArgs,
            Cursor cursor) {
        super(cursor);
        this.context = context;
        this.uri = uri;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
    }

    @Override
    public Bundle respond(Bundle extras) {
        if (extras != null) {
            cancelDeletion = true;
        }
        return super.respond(extras);
    }

    public static void cancelDeletion(Cursor cursor) {
        if (cursor != null) {
            cursor.respond(Bundle.EMPTY);
        }
    }

    @Override
    public void close() {
        super.close();

        if (cancelDeletion) {
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidRays) {
                // May be more efficient to pass in database rather than
                // resolving the content provider.
                context.getContentResolver().delete(uri, selection, selectionArgs);
                return null;
            }
        }.execute();
    }
}
