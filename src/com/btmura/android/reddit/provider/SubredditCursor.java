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

import java.util.List;

import android.database.AbstractCursor;

import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.Subreddits;

class SubredditCursor extends AbstractCursor {

    private static final String FAKE_COLUMN_SUBSCRIBERS = "subscribers";

    private static final String[] PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME,
            FAKE_COLUMN_SUBSCRIBERS,};

    private final List<Subreddit> results;

    SubredditCursor(List<Subreddit> results) {
        this.results = results;
    }

    @Override
    public String[] getColumnNames() {
        return PROJECTION;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public String getString(int column) {
        return results.get(getPosition()).name;
    }

    @Override
    public double getDouble(int column) {
        return 0;
    }

    @Override
    public float getFloat(int column) {
        return 0;
    }

    @Override
    public int getInt(int column) {
        return results.get(getPosition()).subscribers;
    }

    @Override
    public long getLong(int column) {
        return getPosition();
    }

    @Override
    public short getShort(int column) {
        return 0;
    }

    @Override
    public boolean isNull(int column) {
        return false;
    }
}