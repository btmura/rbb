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
package com.btmura.android.reddit.app;

import com.btmura.android.reddit.widget.ThingBundle;

import junit.framework.TestCase;
import android.net.Uri;
import android.os.Bundle;

public class UriHelperTest extends TestCase {

    public void testGetSubreddit() {
        assertSubreddit("pics", "http://www.reddit.com/r/pics");
        assertSubreddit("funny", "http://reddit.com/r/funny");
        assertSubreddit("rbb", "http://www.reddit.com/r/rbb/comments/12zl0q/");
        assertSubreddit("rbb", "http://www.reddit.com/r/rbb/comments/12zl0q/test_1");
        assertSubreddit(null, "http://www.reddit.com/u/btmura");
    }

    public void testGetThingBundle() {
        assertThingBundle("rbb", "12zl0q", "http://reddit.com/r/rbb/comments/12zl0q/");
        assertThingBundle("rbb", "12zl0q", "http://www.reddit.com/r/rbb/comments/12zl0q/test_1");
        assertNullThingBundle("http://www.reddit.com/r/pics");
    }

    public void testGetUser() {
        assertUser("btmura", "http://www.reddit.com/u/btmura");
        assertUser("rbbtest1", "http://reddit.com/u/rbbtest1");
        assertUser(null, "http://www.reddit.com/r/pics");
    }

    private void assertSubreddit(String expected, String url) {
        assertEquals(expected, UriHelper.getSubreddit(Uri.parse(url)));
    }

    private void assertThingBundle(String expectedSubreddit, String expectedThingId, String url) {
        Bundle b = UriHelper.getThingBundle(Uri.parse(url));
        assertEquals(expectedSubreddit, ThingBundle.getSubreddit(b));
        assertEquals(expectedThingId, ThingBundle.getThingId(b));
    }

    private void assertNullThingBundle(String url) {
        assertNull(UriHelper.getThingBundle(Uri.parse(url)));
    }

    private void assertUser(String expected, String url) {
        assertEquals(expected, UriHelper.getUser(Uri.parse(url)));
    }
}
