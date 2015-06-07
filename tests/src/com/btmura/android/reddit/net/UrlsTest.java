/*
 * Copyright (C) 2015 Brian Muramatsu
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

package com.btmura.android.reddit.net;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;

import junit.framework.TestCase;

public class UrlsTest extends TestCase {

  private static final String NO_ACCOUNT = AccountUtils.NO_ACCOUNT;
  private static final String NO_SUBREDDIT = null;
  private static final int NO_FILTER = -1;
  private static final String NO_MORE = null;

  private static final String ACCOUNT = "account";

  public void testMySubreddits() {
    assertCharSequenceEquals(
        "https://oauth.reddit.com/subreddits/mine/subscriber?limit=1000",
        Urls.mySubreddits());
  }

  public void testSubreddit() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/android/hot.json",
        Urls.subreddit(NO_ACCOUNT, "android", Filter.SUBREDDIT_HOT, NO_MORE));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/r/android/new",
        Urls.subreddit(ACCOUNT, "android", Filter.SUBREDDIT_NEW, NO_MORE));
  }

  public void testSubredditLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/androiddev",
        Urls.subredditLink("androiddev"));
  }

  // TODO(btmura): add test for comment URLs

  public void testProfile() {
    assertCharSequenceEquals(
        "https://www.reddit.com/user/btmura/overview.json",
        Urls.profile(NO_ACCOUNT, "btmura", Filter.PROFILE_OVERVIEW, NO_MORE));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/user/btmura/comments",
        Urls.profile(ACCOUNT, "btmura", Filter.PROFILE_COMMENTS, NO_MORE));
  }

  public void testProfileLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/u/btmura",
        Urls.profileLink("btmura"));
  }

  public void testMessageThread() {
    assertCharSequenceEquals(
        "https://oauth.reddit.com/message/messages/123abc",
        Urls.messageThread("123abc"));
  }

  public void testMessageThreadLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/message/messages/123abc",
        Urls.messageThreadLink("123abc"));
  }

  public void testSearch() {
    assertCharSequenceEquals(
        "https://www.reddit.com/search.json?q=s2000",
        Urls.search(NO_ACCOUNT, NO_SUBREDDIT, "s2000", NO_FILTER, NO_MORE));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/search?q=s2000",
        Urls.search(ACCOUNT, NO_SUBREDDIT, "s2000", NO_FILTER, NO_MORE));

    assertCharSequenceEquals(
        "https://www.reddit.com/r/cars/search.json?q=s2000&restrict_sr=on",
        Urls.search(NO_ACCOUNT, "cars", "s2000", NO_FILTER, NO_MORE));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/r/cars/search?q=s2000&restrict_sr=on",
        Urls.search(ACCOUNT, "cars", "s2000", NO_FILTER, NO_MORE));
  }

  public void testSubredditSearch() {
    assertCharSequenceEquals(
        "https://www.reddit.com/subreddits/search.json?q=s2000",
        Urls.subredditSearch(NO_ACCOUNT, "s2000"));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/subreddits/search?q=s2000",
        Urls.subredditSearch(ACCOUNT, "s2000"));
  }

  public void testSidebar() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/cars/about.json",
        Urls.sidebar(NO_ACCOUNT, "cars"));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/r/cars/about",
        Urls.sidebar(ACCOUNT, "cars"));
  }

  public void testUserInfo() {
    assertCharSequenceEquals(
        "https://www.reddit.com/user/btmura/about.json",
        Urls.userInfo(NO_ACCOUNT, "btmura"));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/user/btmura/about",
        Urls.userInfo(ACCOUNT, "btmura"));
  }

  private void assertCharSequenceEquals(
      CharSequence expected,
      CharSequence actual) {
    assertEquals(expected.toString(), actual.toString());
  }
}
