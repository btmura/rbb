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
import android.widget.BaseAdapter;

import com.btmura.android.reddit.R;

/**
 * {@link BaseAdapter} that defines filters used across the application.
 */
abstract class BaseFilterAdapter extends BaseAdapter {

    // TODO(btmura): move constants out of the widget class

    public static final int COMMENTS_BEST = 0;
    public static final int COMMENTS_TOP = 1;
    public static final int COMMENTS_NEW = 2;
    public static final int COMMENTS_HOT = 3;
    public static final int COMMENTS_CONTROVERSIAL = 4;
    public static final int COMMENTS_OLD = 5;

    public static final int MESSAGE_INBOX = 0;
    public static final int MESSAGE_UNREAD = 1;
    public static final int MESSAGE_SENT = 2;

    public static final int PROFILE_OVERVIEW = 0;
    public static final int PROFILE_COMMENTS = 1;
    public static final int PROFILE_SUBMITTED = 2;
    public static final int PROFILE_LIKED = 3;
    public static final int PROFILE_DISLIKED = 4;
    public static final int PROFILE_HIDDEN = 5;
    public static final int PROFILE_SAVED = 6;

    public static final int SEARCH_RELEVANCE = 0;
    public static final int SEARCH_NEW = 1;
    public static final int SEARCH_HOT = 2;
    public static final int SEARCH_TOP = 3;
    public static final int SEARCH_COMMENTS = 4;

    public static final int SUBREDDIT_HOT = 0;
    public static final int SUBREDDIT_TOP = 1;
    public static final int SUBREDDIT_CONTROVERSIAL = 2;
    public static final int SUBREDDIT_NEW = 3;
    public static final int SUBREDDIT_RISING = 4;

    public void addMessageFilters(Context context) {
        clear();
        add(context, R.string.filter_message_inbox, MESSAGE_INBOX);
        add(context, R.string.filter_message_unread, MESSAGE_UNREAD);
        add(context, R.string.filter_message_sent, MESSAGE_SENT);
        notifyDataSetChanged();
    }

    public void addProfileFilters(Context context, boolean hasAccount) {
        clear();
        add(context, R.string.filter_profile_overview, PROFILE_OVERVIEW);
        add(context, R.string.filter_profile_comments, PROFILE_COMMENTS);
        add(context, R.string.filter_profile_submitted, PROFILE_SUBMITTED);
        if (hasAccount) {
            add(context, R.string.filter_profile_liked, PROFILE_LIKED);
            add(context, R.string.filter_profile_disliked, PROFILE_DISLIKED);
            add(context, R.string.filter_profile_hidden, PROFILE_HIDDEN);
            add(context, R.string.filter_profile_saved, PROFILE_SAVED);
        }
        notifyDataSetChanged();
    }

    public void addSubredditFilters(Context context) {
        clear();
        add(context, R.string.filter_subreddit_hot, SUBREDDIT_HOT);
        add(context, R.string.filter_subreddit_top, SUBREDDIT_TOP);
        add(context, R.string.filter_subreddit_controversial, SUBREDDIT_CONTROVERSIAL);
        add(context, R.string.filter_subreddit_new, SUBREDDIT_NEW);
        add(context, R.string.filter_subreddit_rising, SUBREDDIT_RISING);
        notifyDataSetChanged();
    }

    protected abstract void clear();

    protected abstract void add(Context context, int resId, int value);
}
