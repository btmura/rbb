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

import com.btmura.android.reddit.R;

import android.content.Context;
import android.widget.BaseAdapter;

/**
 * {@link BaseAdapter} that defines filters used across the application.
 */
abstract class BaseFilterAdapter extends BaseAdapter {

    public static final int MESSAGE_INBOX = 0;
    public static final int MESSAGE_UNREAD = 1;
    public static final int MESSAGE_SENT = 2;

    public static final int PROFILE_OVERVIEW = 0;
    public static final int PROFILE_COMMENTS = 1;
    public static final int PROFILE_SUBMITTED = 2;
    public static final int PROFILE_LIKED = 3;
    public static final int PROFILE_DISLIKED = 4;
    public static final int PROFILE_SAVED = 5;

    public static final int SUBREDDIT_HOT = 0;
    public static final int SUBREDDIT_TOP = 1;
    public static final int SUBREDDIT_CONTROVERSIAL = 2;
    public static final int SUBREDDIT_NEW = 3;

    public void addMessageFilters(Context context) {
        add(context, R.string.filter_message_inbox);
        add(context, R.string.filter_message_unread);
        add(context, R.string.filter_message_sent);
        notifyDataSetChanged();
    }

    public void addProfileFilters(Context context, boolean hasAccount) {
        add(context, R.string.filter_profile_overview);
        add(context, R.string.filter_profile_comments);
        add(context, R.string.filter_profile_submitted);
        if (hasAccount) {
            add(context, R.string.filter_profile_liked);
            add(context, R.string.filter_profile_disliked);
            add(context, R.string.filter_profile_saved);
        }
        notifyDataSetChanged();
    }

    public void addSubredditFilters(Context context) {
        add(context, R.string.filter_subreddit_hot);
        add(context, R.string.filter_subreddit_top);
        add(context, R.string.filter_subreddit_controversial);
        add(context, R.string.filter_subreddit_new);
        notifyDataSetChanged();
    }

    protected abstract void add(Context context, int resId);
}
