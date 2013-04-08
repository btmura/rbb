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
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.util.Objects;

/** {@link BaseLoaderAdapter} that handles lists of subreddits. */
public abstract class SubredditAdapter extends BaseLoaderAdapter {

    public static final String TAG = "SubredditAdapter";

    protected final boolean singleChoice;
    protected long sessionId = -1;
    protected String accountName;
    protected String selectedSubreddit;

    /** Creates an adapter for showing the user's subreddits with presets. */
    public static SubredditAdapter newSubredditsInstance(Context context, boolean singleChoice) {
        return new SubredditListAdapter(context, true, false, singleChoice);
    }

    /** Creates an adapter for searching for subreddits. */
    public static SubredditAdapter newSearchInstance(Context context, String query,
            boolean singleChoice) {
        return new SubredditSearchAdapter(context, query, singleChoice);
    }

    /** Creates an adapter for use with AutoCompleteTextView. */
    public static SubredditAdapter newAutoCompleteInstance(Context context) {
        // Don't make it single choice for AutoCompleteTextView.
        return new SubredditListAdapter(context, false, true, false);
    }

    protected SubredditAdapter(Context context, boolean singleChoice) {
        super(context, null, 0);
        this.singleChoice = singleChoice;
    }

    @Override
    public boolean isLoadable() {
        return accountName != null;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new SubredditView(context);
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        if (!Objects.equals(this.accountName, accountName)) {
            this.accountName = accountName;
            notifyDataSetChanged();
        }
    }

    public String getSelectedSubreddit() {
        return selectedSubreddit;
    }

    public void setSelectedSubreddit(String subreddit) {
        if (!Objects.equals(selectedSubreddit, subreddit)) {
            selectedSubreddit = subreddit;
            notifyDataSetChanged();
        }
    }

    public String setSelectedPosition(int position) {
        String subreddit = getName(position);
        setSelectedSubreddit(subreddit);
        return subreddit;
    }

    // TODO: Remove the need for this method.
    public abstract String getQuery();

    // TODO: Remove the need for this method.
    public abstract boolean isQuery();

    public abstract String getName(int position);

    public boolean isDeletable(int position) {
        return true;
    }
}
