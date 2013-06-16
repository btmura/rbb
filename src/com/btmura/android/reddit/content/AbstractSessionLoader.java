/*
 * Copyright (C) 2013 Brian Muramatsu
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

package com.btmura.android.reddit.content;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

abstract class AbstractSessionLoader extends CursorLoader {

    private static final String TAG = "AbstractSessionLoader";

    private long sessionId;

    AbstractSessionLoader(Context context, Uri uri, String[] projection, String selection,
            long sessionId, String sortOrder) {
        super(context, uri, projection, selection, null, sortOrder);
        this.sessionId = sessionId;
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }
        if (sessionId == 0) {
            Bundle extras = createSession(sessionId);
            if (extras != null) {
                sessionId = extras.getLong(ThingProvider.EXTRA_SESSION_ID);
                setSelectionArgs(Array.of(sessionId));
            } else {
                return null;
            }
        }
        Bundle extras = new Bundle(1);
        extras.putLong(ThingProvider.EXTRA_SESSION_ID, sessionId);
        return new CursorExtrasWrapper(super.loadInBackground(), extras);
    }

    protected abstract Bundle createSession(long sessionId);
}
