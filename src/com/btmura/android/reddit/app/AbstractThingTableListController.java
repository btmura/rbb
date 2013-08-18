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
import android.text.TextUtils;

import com.btmura.android.reddit.content.ThingProjection;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingListAdapter;

abstract class AbstractThingTableListController
        implements ThingListController<ThingListAdapter>, ThingProjection {

    static final String EXTRA_ACCOUNT_NAME = "accountName";
    static final String EXTRA_PARENT_SUBREDDIT = "parentSubreddit";
    static final String EXTRA_SUBREDDIT = "subreddit";
    static final String EXTRA_FILTER = "filter";
    static final String EXTRA_SINGLE_CHOICE = "singleChoice";
    static final String EXTRA_SELECTED_LINK_ID = "selectedLinkId";
    static final String EXTRA_SELECTED_THING_ID = "selectedThingId";
    static final String EXTRA_CURSOR_EXTRAS = "cursorExtras";
    static final String EXTRA_EMPTY_TEXT = "emptyText";

    protected final Context context;
    protected final ThingListAdapter adapter;

    private final String accountName;
    private int emptyText;
    private int filter;
    private String moreId;
    private Bundle cursorExtras;

    AbstractThingTableListController(Context context, Bundle args, OnVoteListener listener) {
        this.context = context;
        this.accountName = getAccountNameExtra(args);
        this.filter = getFilterExtra(args);
        this.adapter = new ThingListAdapter(context, accountName, listener,
                getSingleChoiceExtra(args));
        restoreInstanceState(args);
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        setParentSubreddit(getParentSubredditExtra(savedInstanceState));
        setSubreddit(getSubredditExtra(savedInstanceState));
        setEmptyText(getEmptyTextExtra(savedInstanceState));
        setSelectedThing(getSelectedThingId(savedInstanceState),
                getSelectedLinkId(savedInstanceState));
        cursorExtras = savedInstanceState.getBundle(EXTRA_CURSOR_EXTRAS);
    }

    @Override
    public void saveInstanceState(Bundle state) {
        state.putInt(EXTRA_EMPTY_TEXT, getEmptyText());
        state.putString(EXTRA_PARENT_SUBREDDIT, getParentSubreddit());
        state.putString(EXTRA_SELECTED_LINK_ID, getSelectedLinkId());
        state.putString(EXTRA_SELECTED_THING_ID, getSelectedThingId());
        state.putBundle(EXTRA_CURSOR_EXTRAS, cursorExtras);
        state.putString(EXTRA_SUBREDDIT, getSubreddit());
    }

    @Override
    public void swapCursor(Cursor cursor) {
        setMoreId(null);
        adapter.swapCursor(cursor);
        if (cursor != null && cursor.getExtras() != null) {
            cursorExtras = cursor.getExtras();
        }
    }

    @Override
    public ThingBundle getThingBundle(int position) {
        return adapter.getThingBundle(position);
    }

    // Actions to be done on things.

    public void onThingSelected(int position) {
        // TODO: What was this for?
    }

    // Getters.

    @Override
    public String getNextMoreId() {
        Cursor c = adapter.getCursor();
        if (c != null && c.moveToLast()) {
            if (c.getInt(INDEX_KIND) == Kinds.KIND_MORE) {
                return c.getString(INDEX_THING_ID);
            }
        }
        return null;
    }

    @Override
    public boolean hasAccountName() {
        return !TextUtils.isEmpty(getAccountName());
    }

    @Override
    public boolean hasCursor() {
        return adapter.getCursor() != null;
    }

    @Override
    public boolean hasNextMoreId() {
        return !TextUtils.isEmpty(getNextMoreId());
    }

    @Override
    public boolean isSingleChoice() {
        return adapter.isSingleChoice();
    }

    // Simple getters for state members.

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public ThingListAdapter getAdapter() {
        return adapter;
    }

    @Override
    public int getEmptyText() {
        return emptyText;
    }

    @Override
    public int getFilter() {
        return filter;
    }

    @Override
    public String getMoreId() {
        return moreId;
    }

    @Override
    public String getParentSubreddit() {
        return adapter.getParentSubreddit();
    }

    // TODO: Remove this method.
    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSelectedLinkId() {
        return adapter.getSelectedLinkId();
    }

    @Override
    public String getSelectedThingId() {
        return adapter.getSelectedThingId();
    }

    @Override
    public String getSubreddit() {
        return adapter.getSubreddit();
    }

    @Override
    public boolean hasQuery() {
        return !TextUtils.isEmpty(getQuery());
    }

    protected Bundle getCursorExtras() {
        return cursorExtras;
    }

    // Simple setters for state members.

    @Override
    public void setEmptyText(int emptyText) {
        this.emptyText = emptyText;
    }

    @Override
    public void setMoreId(String moreId) {
        this.moreId = moreId;
    }

    @Override
    public void setParentSubreddit(String parentSubreddit) {
        adapter.setParentSubreddit(parentSubreddit);
    }

    @Override
    public void setSelectedPosition(int position) {
        adapter.setSelectedPosition(position);
    }

    @Override
    public void setSelectedThing(String thingId, String linkId) {
        adapter.setSelectedThing(thingId, linkId);
    }

    @Override
    public void setSubreddit(String subreddit) {
        adapter.setSubreddit(subreddit);
    }

    @Override
    public void setThingBodyWidth(int thingBodyWidth) {
        adapter.setThingBodyWidth(thingBodyWidth);
    }

    // Getters for extras

    private static String getAccountNameExtra(Bundle extras) {
        return extras.getString(EXTRA_ACCOUNT_NAME);
    }

    private static String getParentSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_PARENT_SUBREDDIT);
    }

    private static String getSubredditExtra(Bundle extras) {
        return extras.getString(EXTRA_SUBREDDIT);
    }

    private static int getFilterExtra(Bundle extras) {
        return extras.getInt(EXTRA_FILTER);
    }

    private static boolean getSingleChoiceExtra(Bundle extras) {
        return extras.getBoolean(EXTRA_SINGLE_CHOICE);
    }

    private static String getSelectedThingId(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_THING_ID);
    }

    private static String getSelectedLinkId(Bundle extras) {
        return extras.getString(EXTRA_SELECTED_LINK_ID);
    }

    private static int getEmptyTextExtra(Bundle extras) {
        return extras.getInt(EXTRA_EMPTY_TEXT);
    }
}
