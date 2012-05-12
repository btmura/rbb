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

package com.btmura.android.reddit.data;

import com.btmura.android.reddit.data.Formatter.RawLinks;

public class Formatter_RawLinksTest extends AbstractFormatterTest {

    public void testPattern() {
        // Test basic structure.
        assertMatch("scheme://authority");
        assertMatch("scheme://authority?query");
        assertMatch("scheme://authority?query#fragment");
        assertMatch("scheme://authority#fragment");

        // Test some examples.
        assertMatch("http://a");
        assertMatch("http://abcd");
        assertMatch("http://a/b/c");
        assertMatch("https://a/b/c");
        assertMatch("https://a/b/c");
        assertMatch("HTTP://a/b/c");
        assertMatch("ftp://a.b.c.d");
    }

    public void testFormat() {
        CharSequence cs = assertRawLinksFormat("http://abcd", "http://abcd");
        assertUrlSpan(cs, 0, 11, "http://abcd");

        cs = assertRawLinksFormat("https://abcd", "https://abcd");
        assertUrlSpan(cs, 0, 12, "https://abcd");
    }

    public void testFormat_multipleLine() {
        CharSequence cs = assertRawLinksFormat("http://abcd\ndef", "http://abcd\ndef");
        assertUrlSpan(cs, 0, 11, "http://abcd");
    }

    public void testFormat_endings() {
        CharSequence cs = assertRawLinksFormat("(http://abcd)", "(http://abcd)");
        assertUrlSpan(cs, 1, 12, "http://abcd");

        cs = assertRawLinksFormat("end paren: http://abcd)", "end paren: http://abcd)");
        assertUrlSpan(cs, 11, 22, "http://abcd");

        cs = assertRawLinksFormat("look at http://abcd?", "look at http://abcd?");
        assertUrlSpan(cs, 8, 19, "http://abcd");

        cs = assertRawLinksFormat("see diagram (http://abcd)", "see diagram (http://abcd)");
        assertUrlSpan(cs, 13, 24, "http://abcd");
    }

    private void assertMatch(String input) {
        assertTrue(input, RawLinks.PATTERN.matcher(input).matches());
    }
}
