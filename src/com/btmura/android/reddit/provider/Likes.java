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
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.util.ArrayUtils;

public class Likes {

    public static final String TAG = "Likes";
    public static final boolean DEBUG = Debug.DEBUG;

    public static void likeInBackground(final Context context, final String thingId,
            final int likes) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                ContentResolver cr = context.getContentResolver();
                String[] selectionArgs = ArrayUtils.toArray(thingId);

                ContentValues values = new ContentValues(1);
                values.put(Things.COLUMN_LIKES, likes);
                int count = cr.update(Things.CONTENT_URI, values, Things.THING_ID_SELECTION,
                        selectionArgs);
                if (DEBUG) {
                    Log.d(TAG, "things updated: " + count);
                }

                values.clear();
                values.put(Comments.COLUMN_LIKES, likes);
                count = cr.update(Comments.CONTENT_URI, values, Comments.THING_ID_SELECTION,
                        selectionArgs);
                if (DEBUG) {
                    Log.d(TAG, "comments updated: " + count);
                }
            }
        });
    }
}
