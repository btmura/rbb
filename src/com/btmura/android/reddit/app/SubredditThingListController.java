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

package com.btmura.android.reddit.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.Loader;

import com.btmura.android.reddit.content.SubredditThingLoader;
import com.btmura.android.reddit.widget.ThingListAdapter;

class SubredditThingListController extends AbstractThingListController {

    private final Context context;

    SubredditThingListController(Context context, ThingListAdapter adapter) {
        super(context, adapter);
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isLoadable() {
        return getAccountName() != null && getSubreddit() != null;
    }

    @Override
    public Loader<Cursor> createLoader() {
        return new SubredditThingLoader(context, getAccountName(), getSubreddit(), getFilter(),
                getMoreId(), getSessionId());
    }
}
