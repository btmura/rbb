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
package com.btmura.android.reddit.net;

import android.net.Uri;

import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.app.ThingBundle;

import junit.framework.TestCase;

public class UriHelperTest extends TestCase {

  public void testGetSubreddit() {
    assertSubreddit("pics", -1, "http://www.reddit.com/r/pics");

    assertSubreddit("pics", Filter.SUBREDDIT_HOT,
        "http://reddit.com/r/pics/hot");
    assertSubreddit("pics", Filter.SUBREDDIT_NEW,
        "http://www.reddit.com/r/pics/new");
    assertSubreddit("pics", Filter.SUBREDDIT_TOP,
        "http://reddit.com/r/pics/top");
    assertSubreddit("pics", Filter.SUBREDDIT_CONTROVERSIAL,
        "http://www.reddit.com/r/pics/controversial");

    assertSubreddit("funny", -1, "http://reddit.com/r/funny");
    assertSubreddit("rbb", -1, "http://www.reddit.com/r/rbb/comments/12zl0q/");
    assertSubreddit("rbb", -1,
        "http://www.reddit.com/r/rbb/comments/12zl0q/test_1");
    assertSubreddit(null, -1, "http://www.reddit.com/u/btmura");
  }

  public void testGetThingBundle() {
    assertThingBundle("rbb", "t3_12zl0q", null,
        "http://reddit.com/r/rbb/comments/12zl0q/");
    assertThingBundle("rbb", "t3_12zl0q", null,
        "http://www.reddit.com/r/rbb/comments/12zl0q/test_1");
    assertThingBundle("rbb", "t1_1976x5", "t3_12zl0q",
        "http://www.reddit.com/r/rbb/comments/12zl0q/test_1/1976x5");

    assertNullThingBundle("http://www.reddit.com/r/pics");
  }

  public void testGetUser() {
    assertUser("btmura", -1, "http://www.reddit.com/u/btmura");
    assertUser("rbbtest1", -1, "http://reddit.com/u/rbbtest1");

    assertUser("rbbtest1", Filter.PROFILE_OVERVIEW,
        "http://reddit.com/u/rbbtest1/overview");
    assertUser("rbbtest2", Filter.PROFILE_COMMENTS,
        "http://www.reddit.com/u/rbbtest2/comments");
    assertUser("rbbtest1", Filter.PROFILE_SUBMITTED,
        "http://reddit.com/u/rbbtest1/submitted");

    assertUser(null, -1, "http://www.reddit.com/r/pics");
  }

  private void assertSubreddit(
      String expectedSubreddit,
      int expectedFilter,
      String url) {
    assertEquals(expectedSubreddit, UriHelper.getSubreddit(Uri.parse(url)));
    assertEquals(expectedFilter, UriHelper.getSubredditFilter(Uri.parse(url)));
  }

  private void assertThingBundle(
      String expectedSubreddit, String expectedThingId,
      String expectedLinkId, String url) {
    ThingBundle b = UriHelper.getThingBundle(Uri.parse(url));
    assertEquals(expectedSubreddit, b.getSubreddit());
    assertEquals(expectedThingId, b.getThingId());
    assertEquals(expectedLinkId, b.getLinkId());
  }

  private void assertNullThingBundle(String url) {
    assertNull(UriHelper.getThingBundle(Uri.parse(url)));
  }

  private void assertUser(String expectedUser, int expectedFilter, String url) {
    assertEquals(expectedUser, UriHelper.getUser(Uri.parse(url)));
    assertEquals(expectedFilter, UriHelper.getUserFilter(Uri.parse(url)));
  }
}
