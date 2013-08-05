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

import java.io.IOException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.SidebarResult;
import com.btmura.android.reddit.util.Array;

public class RelatedSubredditLoader extends CursorLoader {

    private static final String TAG = "RelatedSubredditLoader";

    public static final int INDEX_NAME = 1;

    private static final Pattern SUBREDDIT_PATTERN = Pattern.compile("r/([0-9A-Za-z_]+)");

    private static final String[] COLUMN_NAMES = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME,
    };

    private final String subreddit;

    public RelatedSubredditLoader(Context context, String subreddit) {
        super(context);
        this.subreddit = subreddit;
    }

    @Override
    public Cursor loadInBackground() {
        try {
            SidebarResult result = RedditApi.getSidebar(getContext(), subreddit, null);
            result.recycle();
            return buildCursor(findSubreddits(result.description));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private TreeSet<String> findSubreddits(CharSequence description) {
        TreeSet<String> subreddits = new TreeSet<String>();
        Matcher matcher = SUBREDDIT_PATTERN.matcher(description);
        while (matcher.find()) {
            subreddits.add(matcher.group(1));
        }
        return subreddits;
    }

    private MatrixCursor buildCursor(TreeSet<String> subreddits) {
        int i = 0;
        MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES, subreddits.size());
        for (String subreddit : subreddits) {
            cursor.addRow(Array.of(i++, subreddit));
        }
        return cursor;
    }
}
