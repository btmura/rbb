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

public class Formatter_EscapedTest extends AbstractFormatterTest {

    public void testFormat_noEscapes() {
        assertEscapedFormat("title", "title");
    }

    public void testFormat_gt() {
        assertEscapedFormat("gt &gt;", "gt >");
    }

    public void testFormat_lt() {
        assertEscapedFormat("lt &lt;", "lt <");
    }

    public void testFormat_amp() {
        assertEscapedFormat("amp &amp;", "amp &");
    }

    public void testFormat_quot() {
        assertEscapedFormat("quot &quot;", "quot \"");
    }

    public void testFormat_apos() {
        assertEscapedFormat("apos &apos;", "apos '");
    }

    public void testFormat_nbsp() {
        assertEscapedFormat("nbsp &nbsp;", "nbsp  ");
    }

    public void testFormat_mdash() {
        assertEscapedFormat("mdash &mdash;", "mdash —");
    }

    public void testFormat_characterReference() {
        assertEscapedFormat("#3232 &#3232;", "#3232 ಠ");
    }

    public void testFormat_multipleEscapes() {
        assertEscapedFormat("gt &gt; lt &lt;", "gt > lt <");
        assertEscapedFormat("&lt;3 &apos;Quote&apos; &mdash;", "<3 'Quote' —");
    }

    public void testFormat_escapedEscapes() {
        assertEscapedFormat("&amp;gt;", ">");
    }
}
