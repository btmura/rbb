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
import android.os.Bundle;

import com.btmura.android.reddit.util.Objects;

public abstract class BaseLoaderAdapter extends LoaderAdapter {

    public static final String TAG = "BaseLoaderAdapter";

    protected long sessionId = -1;
    protected String accountName;
    protected String parentSubreddit;
    protected String subreddit;
    protected String query;
    protected int filter;
    protected String more;

    protected boolean singleChoice;
    protected OnVoteListener listener;
    protected int thingBodyWidth;

    protected String selectedThingId;
    protected String selectedLinkId;

    BaseLoaderAdapter(Context context, String query) {
        super(context, null, 0);
        this.query = query;
    }

    public void setSingleChoice(boolean singleChoice) {
        this.singleChoice = singleChoice;
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setThingBodyWidth(int thingBodyWidth) {
        this.thingBodyWidth = thingBodyWidth;
    }

    public String getSelectedThingId() {
        return selectedThingId;
    }

    public String getSelectedLinkId() {
        return selectedLinkId;
    }

    public void setSelectedThing(String thingId, String linkId) {
        if (!Objects.equals(selectedThingId, thingId)
                || !Objects.equals(selectedLinkId, linkId)) {
            selectedThingId = thingId;
            selectedLinkId = linkId;
            notifyDataSetChanged();
        }
    }

    public void setSelectedPosition(int position) {
        setSelectedThing(getThingId(position), getLinkId(position));
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
        this.accountName = accountName;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getParentSubreddit() {
        return parentSubreddit;
    }

    public void setParentSubreddit(String parentSubreddit) {
        this.parentSubreddit = parentSubreddit;
    }

    public String getQuery() {
        return query;
    }

    public int getFilterValue() {
        return filter;
    }

    public String getMore() {
        return more;
    }

    public void setMore(String more) {
        this.more = more;
    }

    public abstract String getAuthor(int position);

    public abstract String getLinkId(int position);

    public abstract Bundle getReplyExtras(int position);

    public abstract boolean isSaved(int position);

    public abstract String getThingId(int position);

    public abstract String getTitle(int position);

    public abstract CharSequence getUrl(int position);

    public Bundle getThingBundle(Context context, int position) {
        Cursor c = getCursor();
        if (c != null && c.moveToPosition(position)) {
            return makeThingBundle(context, c);
        }
        return null;
    }

    protected abstract Bundle makeThingBundle(Context context, Cursor c);
}
