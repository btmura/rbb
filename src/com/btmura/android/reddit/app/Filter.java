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

public interface Filter {

    static final int COMMENTS_BEST = 0;
    static final int COMMENTS_TOP = 1;
    static final int COMMENTS_NEW = 2;
    static final int COMMENTS_HOT = 3;
    static final int COMMENTS_CONTROVERSIAL = 4;
    static final int COMMENTS_OLD = 5;

    static final int MESSAGE_INBOX = 0;
    static final int MESSAGE_UNREAD = 1;
    static final int MESSAGE_SENT = 2;

    static final int PROFILE_OVERVIEW = 0;
    static final int PROFILE_COMMENTS = 1;
    static final int PROFILE_SUBMITTED = 2;
    static final int PROFILE_LIKED = 3;
    static final int PROFILE_DISLIKED = 4;
    static final int PROFILE_HIDDEN = 5;
    static final int PROFILE_SAVED = 6;

    static final int SEARCH_RELEVANCE = 0;
    static final int SEARCH_NEW = 1;
    static final int SEARCH_HOT = 2;
    static final int SEARCH_TOP = 3;
    static final int SEARCH_COMMENTS = 4;

    static final int SUBREDDIT_HOT = 0;
    static final int SUBREDDIT_TOP = 1;
    static final int SUBREDDIT_CONTROVERSIAL = 2;
    static final int SUBREDDIT_NEW = 3;
    static final int SUBREDDIT_RISING = 4;
}
