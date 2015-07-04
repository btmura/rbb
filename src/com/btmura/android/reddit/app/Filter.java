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

  int COMMENTS_BEST = 0;
  int COMMENTS_TOP = 1;
  int COMMENTS_NEW = 2;
  int COMMENTS_HOT = 3;
  int COMMENTS_CONTROVERSIAL = 4;
  int COMMENTS_OLD = 5;

  int MESSAGE_INBOX = 0;
  int MESSAGE_UNREAD = 1;
  int MESSAGE_SENT = 2;

  int PROFILE_OVERVIEW = 0;
  int PROFILE_COMMENTS = 1;
  int PROFILE_SUBMITTED = 2;
  int PROFILE_UPVOTED = 3;
  int PROFILE_DOWNVOTED = 4;
  int PROFILE_HIDDEN = 5;
  int PROFILE_SAVED = 6;

  int SEARCH_RELEVANCE = 0;
  int SEARCH_NEW = 1;
  int SEARCH_HOT = 2;
  int SEARCH_TOP = 3;
  int SEARCH_COMMENTS = 4;

  int SUBREDDIT_HOT = 0;
  int SUBREDDIT_TOP = 1;
  int SUBREDDIT_CONTROVERSIAL = 2;
  int SUBREDDIT_NEW = 3;
  int SUBREDDIT_RISING = 4;
}
