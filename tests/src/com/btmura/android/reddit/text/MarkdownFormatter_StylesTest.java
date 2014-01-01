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

import android.graphics.Typeface;

import com.btmura.android.reddit.text.MarkdownFormatter.Styles;

public class MarkdownFormatter_StylesTest extends AbstractFormatterTest {

    public void testFormatComment_bold() {
        CharSequence cs = assertStyleFormat(Styles.STYLE_BOLD, "**bold**", "bold");
        assertStyleSpan(cs, 0, 4, Typeface.BOLD);
    }

    public void testFormatComment_boldBadFormat() {
        assertStyleFormat(Styles.STYLE_BOLD, "**bold\n**bold", "**bold\n**bold");
    }

    public void testFormatComment_italic() {
        CharSequence cs = assertStyleFormat(Styles.STYLE_ITALIC, "*italic*", "italic");
        assertStyleSpan(cs, 0, 6, Typeface.ITALIC);
    }

    public void testFormatComment_strikethrough() {
        CharSequence cs = assertStyleFormat(Styles.STYLE_STRIKETHROUGH, "~~strikethrough~~",
                "strikethrough");
        assertStrikethroughSpan(cs, 0, 13);
    }

    public void testFormat_bold() {
        CharSequence cs = assertStyleFormat(Styles.STYLE_BOLD, "**bold**", "bold");
        assertStyleSpan(cs, 0, 4, Typeface.BOLD);
    }

    public void testFormat_boldMultiple() {
        CharSequence cs = assertStyleFormat(Styles.STYLE_BOLD, "**yes** no **yes**", "yes no yes");
        assertStyleSpan(cs, 0, 3, Typeface.BOLD);
        assertStyleSpan(cs, 7, 10, Typeface.BOLD);
    }

    public void testFormat_boldBadFormat() {
        assertStyleFormat(Styles.STYLE_BOLD, "**bold\n**bold", "**bold\n**bold");
    }
}
