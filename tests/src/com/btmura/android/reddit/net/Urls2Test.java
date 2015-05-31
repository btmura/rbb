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

  public void testMySubreddits() {
    assertCharSequenceEquals(
        "https://oauth.reddit.com/subreddits/mine/subscriber?limit=1000",
        Urls2.mySubreddits().toString());
  }

  public void testSubreddit() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/androiddev/hot.json",
        Urls2.subreddit(AccountUtils.NO_ACCOUNT, "androiddev",
            Filter.SUBREDDIT_HOT, null));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/r/androiddev/controversial",
        Urls2.subreddit("rbbaccount", "androiddev",
            Filter.SUBREDDIT_CONTROVERSIAL, null));
  }

  public void testSubredditLink() {
    assertCharSequenceEquals(
        "https://www.reddit.com/r/androiddev",
        Urls2.subredditLink("androiddev").toString());
  }

  public void testUserInfo() {
    assertCharSequenceEquals(
        "https://www.reddit.com/u/btmura.json",
        Urls2.userInfo(AccountUtils.NO_ACCOUNT, "btmura"));

    assertCharSequenceEquals(
        "https://oauth.reddit.com/user/btmura",
        Urls2.userInfo("rbbaccount", "btmura"));
  }

  public void testUserInfoLink() {
    assertCharSequenceEquals(
        "http://www.reddit.com/u/btmura",
        Urls2.userInfoLink("btmura"));
  }

  private void assertCharSequenceEquals(
      CharSequence expected,
      CharSequence actual) {
    assertEquals(expected.toString(), actual.toString());
  }
}
