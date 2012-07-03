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

public class FormatterTest extends AbstractFormatterTest {

    protected Formatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        formatter = new Formatter();
    }

    public void testFormatComment() {
        assertComment("this is <bold> text", "this is **&amp;lt;bold&gt;** text");
        assertComment("this is >italics< and this is strikethrough",
                "this is &gt;*italics&amp;lt;* and this is ~~strikethrough~~");
    }

    private void assertComment(String expected, String input) {
        CharSequence formatted = formatter.formatComment(mContext, input);
        String actual = formatted.toString();
        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }
}
