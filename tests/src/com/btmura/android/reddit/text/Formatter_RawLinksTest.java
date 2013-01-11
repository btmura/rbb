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

package com.btmura.android.reddit.text;

import com.btmura.android.reddit.text.Formatter.RawLinks;

public class Formatter_RawLinksTest extends AbstractFormatterTest {

    public void testPattern() {
        // Test some examples.
        assertMatch("http://a.com");
        assertMatch("http://abcd.org");
        assertMatch("http://a.net/b/c");
        assertMatch("https://a.net/b/c");
        assertMatch("https://1.2.3.4/b/c");
    }

    public void testFormat() {
        CharSequence cs = assertRawLinksFormat("http://abcd.com", "http://abcd.com");
        assertUrlSpan(cs, 0, 11, "http://abcd.com");

        cs = assertRawLinksFormat("https://abcd.com", "https://abcd.com");
        assertUrlSpan(cs, 0, 12, "https://abcd.com");
    }

    public void testFormat_multipleLine() {
        CharSequence cs = assertRawLinksFormat("http://abcd.net\ndef", "http://abcd.net\ndef");
        assertUrlSpan(cs, 0, 11, "http://abcd.net");
    }

    public void testFormat_endings() {
        CharSequence cs = assertRawLinksFormat("(http://abcd.org)", "(http://abcd.org)");
        assertUrlSpan(cs, 1, 12, "http://abcd.org");

        cs = assertRawLinksFormat("end paren: http://abcd.org)", "end paren: http://abcd.org)");
        assertUrlSpan(cs, 11, 22, "http://abcd.org");

        cs = assertRawLinksFormat("look at http://abcd.org?", "look at http://abcd.org?");
        assertUrlSpan(cs, 8, 19, "http://abcd.org");

        cs = assertRawLinksFormat("see diagram (http://abcd.org)", "see diagram (http://abcd.org)");
        assertUrlSpan(cs, 13, 24, "http://abcd.org");
    }

    private void assertMatch(String input) {
        assertTrue(input, RawLinks.PATTERN.matcher(input).matches());
    }
}
