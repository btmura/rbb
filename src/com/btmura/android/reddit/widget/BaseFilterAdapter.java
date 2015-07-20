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

import static com.btmura.android.reddit.app.Filter.MESSAGE_INBOX;
import static com.btmura.android.reddit.app.Filter.MESSAGE_SENT;
import static com.btmura.android.reddit.app.Filter.MESSAGE_UNREAD;
import static com.btmura.android.reddit.app.Filter.PROFILE_COMMENTS;
import static com.btmura.android.reddit.app.Filter.PROFILE_DOWNVOTED;
import static com.btmura.android.reddit.app.Filter.PROFILE_HIDDEN;
import static com.btmura.android.reddit.app.Filter.PROFILE_OVERVIEW;
import static com.btmura.android.reddit.app.Filter.PROFILE_SAVED;
import static com.btmura.android.reddit.app.Filter.PROFILE_SUBMITTED;
import static com.btmura.android.reddit.app.Filter.PROFILE_UPVOTED;
import static com.btmura.android.reddit.app.Filter.SUBREDDIT_CONTROVERSIAL;
import static com.btmura.android.reddit.app.Filter.SUBREDDIT_HOT;
import static com.btmura.android.reddit.app.Filter.SUBREDDIT_NEW;
import static com.btmura.android.reddit.app.Filter.SUBREDDIT_RISING;
import static com.btmura.android.reddit.app.Filter.SUBREDDIT_TOP;

/**
 * {@link BaseAdapter} that defines filters used across the application.
 */
abstract class BaseFilterAdapter extends BaseAdapter {

  public void addMessageFilters(Context ctx) {
    clear();
    add(ctx, R.string.filter_message_inbox, MESSAGE_INBOX);
    add(ctx, R.string.filter_message_unread, MESSAGE_UNREAD);
    add(ctx, R.string.filter_message_sent, MESSAGE_SENT);
    notifyDataSetChanged();
  }

  public void addProfileFilters(Context ctx, boolean hasAccount) {
    clear();
    add(ctx, R.string.filter_profile_overview, PROFILE_OVERVIEW);
    add(ctx, R.string.filter_profile_comments, PROFILE_COMMENTS);
    add(ctx, R.string.filter_profile_submitted, PROFILE_SUBMITTED);
    if (hasAccount) {
      add(ctx, R.string.filter_profile_upvoted, PROFILE_UPVOTED);
      add(ctx, R.string.filter_profile_downvoted, PROFILE_DOWNVOTED);
      add(ctx, R.string.filter_profile_hidden, PROFILE_HIDDEN);
      add(ctx, R.string.filter_profile_saved, PROFILE_SAVED);
    }
    notifyDataSetChanged();
  }

  public void addSubredditFilters(Context ctx) {
    clear();
    add(ctx, R.string.filter_subreddit_hot, SUBREDDIT_HOT);
    add(ctx, R.string.filter_subreddit_top, SUBREDDIT_TOP);
    add(ctx, R.string.filter_subreddit_controversial,
        SUBREDDIT_CONTROVERSIAL);
    add(ctx, R.string.filter_subreddit_new, SUBREDDIT_NEW);
    add(ctx, R.string.filter_subreddit_rising, SUBREDDIT_RISING);
    notifyDataSetChanged();
  }

  protected abstract void clear();

  protected abstract void add(Context ctx, int resId, int value);
}
