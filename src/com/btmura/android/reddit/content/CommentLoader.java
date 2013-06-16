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
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.database.SharedColumns;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.util.Array;

/**
 * {@link CursorLoader} for viewing a thing's comments.
 */
public class CommentLoader extends CursorLoader {

    private static final String TAG = "CommentLoader";

    private static final String[] PROJECTION = {
            Comments._ID,
            Comments.COLUMN_AUTHOR,
            Comments.COLUMN_BODY,
            Comments.COLUMN_CREATED_UTC,
            Comments.COLUMN_DOMAIN,
            Comments.COLUMN_DOWNS,
            Comments.COLUMN_EXPANDED,
            Comments.COLUMN_KIND,
            Comments.COLUMN_LIKES,
            Comments.COLUMN_NESTING,
            Comments.COLUMN_NUM_COMMENTS,
            Comments.COLUMN_OVER_18,
            Comments.COLUMN_PERMA_LINK,
            Comments.COLUMN_SAVED,
            Comments.COLUMN_SCORE,
            Comments.COLUMN_SELF,
            Comments.COLUMN_SEQUENCE,
            Comments.COLUMN_SESSION_ID,
            Comments.COLUMN_SUBREDDIT,
            Comments.COLUMN_TITLE,
            Comments.TABLE_NAME + "." + Comments.COLUMN_THING_ID,
            Comments.COLUMN_THUMBNAIL_URL,
            Comments.COLUMN_UPS,
            Comments.COLUMN_URL,

            // Following columns are from joined tables at the end.
            SharedColumns.COLUMN_SAVE_ACTION,
            SharedColumns.COLUMN_VOTE_ACTION,
    };

    public static final int INDEX_ID = 0;
    public static final int INDEX_AUTHOR = 1;
    public static final int INDEX_BODY = 2;
    public static final int INDEX_CREATED_UTC = 3;
    public static final int INDEX_DOMAIN = 4;
    public static final int INDEX_DOWNS = 5;
    public static final int INDEX_EXPANDED = 6;
    public static final int INDEX_KIND = 7;
    public static final int INDEX_LIKES = 8;
    public static final int INDEX_NESTING = 9;
    public static final int INDEX_NUM_COMMENTS = 10;
    public static final int INDEX_OVER_18 = 11;
    public static final int INDEX_PERMA_LINK = 12;
    public static final int INDEX_SAVED = 13;
    public static final int INDEX_SCORE = 14;
    public static final int INDEX_SELF = 15;
    public static final int INDEX_SEQUENCE = 16;
    public static final int INDEX_SESSION_ID = 17;
    public static final int INDEX_SUBREDDIT = 18;
    public static final int INDEX_TITLE = 19;
    public static final int INDEX_THING_ID = 20;
    public static final int INDEX_THUMBNAIL_URL = 21;
    public static final int INDEX_UPS = 22;
    public static final int INDEX_URL = 23;

    // Following columns are from joined tables at the end.
    public static final int INDEX_SAVE_ACTION = 24;
    public static final int INDEX_VOTE_ACTION = 25;

    private final String accountName;
    private final String thingId;
    private final String linkId;
    private final long sessionId;

    public CommentLoader(Context context, String accountName, String thingId, String linkId,
            long sessionId) {
        super(context);
        this.accountName = accountName;
        this.thingId = thingId;
        this.linkId = linkId;
        this.sessionId = sessionId;

        setUri(ThingProvider.COMMENTS_WITH_ACTIONS_URI);
        setProjection(PROJECTION);
        setSelection(Comments.SELECT_VISIBLE_BY_SESSION_ID);
        setSortOrder(Comments.SORT_BY_SEQUENCE_AND_ID);
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }
        Bundle extras = ThingProvider.getCommentsSession(getContext(), accountName, thingId,
                linkId, sessionId, -1);
        long sessionId = extras.getLong(ThingProvider.EXTRA_SESSION_ID);
        setSelectionArgs(Array.of(sessionId));
        return new CursorExtrasWrapper(super.loadInBackground(), extras);
    }
}
