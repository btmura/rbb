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
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.btmura.android.reddit.content.RelatedSubredditLoader;
import com.btmura.android.reddit.widget.RelatedSubredditAdapter;

class RelatedSubredditListController implements SubredditListController<RelatedSubredditAdapter> {

    static final String EXTRA_SIDEBAR_SUBREDDIT = "sidebarSubreddit";
    static final String EXTRA_SELECTED_SUBREDDIT = "selectedSubreddit";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";

    private final Context context;
    private final String sidebarSubreddit;
    private final RelatedSubredditAdapter adapter;

    RelatedSubredditListController(Context context, Bundle args) {
        this.context = context;
        this.sidebarSubreddit = getSidebarSubredditExtra(args);
        this.adapter = new RelatedSubredditAdapter(context, getSingleChoiceExtra(args));
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setSelectedSubreddit(getSelectedSubredditExtra(savedInstanceState));
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        outState.putString(EXTRA_SELECTED_SUBREDDIT, getSelectedSubreddit());
    }

    // Loader related methods

    @Override
    public Loader<Cursor> createLoader() {
        return new RelatedSubredditLoader(context, sidebarSubreddit);
    }

    @Override
    public void swapCursor(Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    // Getters

    @Override
    public RelatedSubredditAdapter getAdapter() {
        return adapter;
    }

    @Override
    public String getSelectedSubreddit() {
        return adapter.getSelectedSubreddit();
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    // Setters

    @Override
    public String setSelectedPosition(int position) {
        return adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedSubreddit(String selectedSubreddit) {
        adapter.setSelectedSubreddit(selectedSubreddit);
    }

    // Getters for extras.

    private static String getSidebarSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SIDEBAR_SUBREDDIT);
    }

    private static String getSelectedSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_SUBREDDIT);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }
}
