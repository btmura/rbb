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
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;

import com.btmura.android.reddit.util.Objects;

/** {@link SubredditAdapter} that handles searching for subreddits. */
public class SubredditSearchAdapter extends SubredditAdapter {

    private static final int INDEX_NAME = 1;
    private static final int INDEX_SUBSCRIBERS = 2;
    private static final int INDEX_OVER_18 = 3;

    private final String query;

    public SubredditSearchAdapter(Context context, String query, boolean singleChoice) {
        super(context, singleChoice);
        this.query = query;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String name = cursor.getString(INDEX_NAME);
        int subscribers = query != null ? cursor.getInt(INDEX_SUBSCRIBERS) : -1;
        boolean over18 = query != null && cursor.getInt(INDEX_OVER_18) == 1;
        SubredditView v = (SubredditView) view;
        v.setData(name, over18, subscribers);
        v.setChosen(singleChoice && Objects.equalsIgnoreCase(selectedSubreddit, name));
    }

    @Override
    public String getName(int position) {
        return getString(position, INDEX_NAME);
    }

    public boolean isQuery() {
        return !TextUtils.isEmpty(query);
    }

    public String getQuery() {
        return query;
    }
}
