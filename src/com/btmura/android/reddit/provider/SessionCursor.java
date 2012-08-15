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

package com.btmura.android.reddit.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.btmura.android.reddit.Debug;

class SessionCursor extends CursorWrapper {

    public static final String TAG = "SessionCursor";
    public static final boolean DEBUG = Debug.DEBUG;

    private final Context context;
    private final Uri uri;
    private final String selection;
    private final String[] selectionArgs;

    public SessionCursor(Context context, Uri uri, String selection,
            String[] selectionArgs, Cursor cursor) {
        super(cursor);
        this.context = context;
        this.uri = uri;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
    }

    @Override
    public void close() {
        super.close();
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                int count = cr.delete(uri, selection, selectionArgs);
                if (DEBUG) {
                    Log.d(TAG, "close: " + count);
                }
            }
        });
    }
}
