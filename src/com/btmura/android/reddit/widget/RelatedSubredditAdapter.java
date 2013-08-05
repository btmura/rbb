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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.btmura.android.reddit.content.RelatedSubredditLoader;
import com.btmura.android.reddit.util.Objects;

public class RelatedSubredditAdapter extends SubredditAdapter {

    public RelatedSubredditAdapter(Context context, boolean singleChoice) {
        super(context, singleChoice);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String name = cursor.getString(RelatedSubredditLoader.INDEX_NAME);
        SubredditView v = (SubredditView) view;
        v.setData(name, false, -1);
        v.setChosen(singleChoice && Objects.equalsIgnoreCase(selectedSubreddit, name));
    }

    @Override
    public String getName(int position) {
        return getString(position, RelatedSubredditLoader.INDEX_NAME);
    }
}
