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

public class Urls2Test extends TestCase {

  private static final String NO_ACCOUNT = AccountUtils.NO_ACCOUNT;
  private static final String ACCOUNT = "account";

  public void testMySubreddits() {
    assertCharSequenceEquals(
        "https://oauth.reddit.com/subreddits/mine/subscriber?limit=1000",
        Urls2.mySubreddits());
  }

  public void testSubreddit() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/androiddev/hot.json",
        Urls2.subreddit(NO_ACCOUNT, "androiddev", Filter.SUBREDDIT_HOT, null));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/r/androiddev/new",
        Urls2.subreddit(ACCOUNT, "androiddev", Filter.SUBREDDIT_NEW, null));
  }

  public void testSubredditLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/androiddev",
        Urls2.subredditLink("androiddev"));
  }

  // TODO(btmura): add test for comment URLs

  public void testProfile() {
    assertCharSequenceEquals(
        "https://www.reddit.com/user/btmura/overview.json",
        Urls2.profile(NO_ACCOUNT, "btmura", Filter.PROFILE_OVERVIEW, null));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/user/btmura/comments",
        Urls2.profile(ACCOUNT, "btmura", Filter.PROFILE_COMMENTS, null));
  }

  public void testProfileLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/u/btmura",
        Urls2.profileLink("btmura"));
  }

  public void testMessageThread() {
    assertCharSequenceEquals(
        "https://oauth.reddit.com/message/messages/123abc",
        Urls2.messageThread("123abc"));
  }

  public void testMessageThreadLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/message/messages/123abc",
        Urls2.messageThreadLink("123abc"));
  }

  public void testUserInfo() {
    assertCharSequenceEquals(
        "https://www.reddit.com/user/btmura/about.json",
        Urls2.userInfo(NO_ACCOUNT, "btmura"));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/user/btmura/about",
        Urls2.userInfo(ACCOUNT, "btmura"));
  }

  private void assertCharSequenceEquals(
      CharSequence expected,
      CharSequence actual) {
    assertEquals(expected.toString(), actual.toString());
  }
}
