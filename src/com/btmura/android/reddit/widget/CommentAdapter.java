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
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.btmura.android.reddit.provider.Comments;
import com.btmura.android.reddit.util.ArrayUtils;

public class CommentAdapter extends CursorAdapter {

    private static final String[] PROJECTION = {
            Comments._ID,
            Comments.COLUMN_BODY,
            Comments.COLUMN_DOWNS,
            Comments.COLUMN_LIKES,
            Comments.COLUMN_NESTING,
            Comments.COLUMN_TITLE,
            Comments.COLUMN_UPS,
    };

    private static final int INDEX_BODY = 1;
    private static final int INDEX_DOWNS = 2;
    private static final int INDEX_LIKES = 3;
    private static final int INDEX_NESTING = 4;
    private static final int INDEX_TITLE = 5;
    private static final int INDEX_UP = 6;

    private final OnVoteListener listener;

    public static CursorLoader createLoader(Context context, String accountName, String thingId) {
        Uri uri = Comments.CONTENT_URI.buildUpon()
                .appendQueryParameter(Comments.PARAM_SYNC, Boolean.toString(true))
                .appendQueryParameter(Comments.PARAM_ACCOUNT_NAME, accountName)
                .appendQueryParameter(Comments.PARAM_THING_ID, thingId)
                .build();
        return new CursorLoader(context, uri, PROJECTION, Comments.PARENT_ID_SELECTION,
                ArrayUtils.toArray(thingId), null);
    }

    public CommentAdapter(Context context, OnVoteListener listener) {
        super(context, null, 0);
        this.listener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new CommentView(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String body = cursor.getString(INDEX_BODY);
        int downs = cursor.getInt(INDEX_DOWNS);
        int likes = cursor.getInt(INDEX_LIKES);
        int nesting = cursor.getInt(INDEX_NESTING);
        String title = cursor.getString(INDEX_TITLE);
        int ups = cursor.getInt(INDEX_UP);

        CommentView cv = (CommentView) view;
        cv.setOnVoteListener(listener);
        cv.setData(body, downs, likes, nesting, title, ups);
    }
}
