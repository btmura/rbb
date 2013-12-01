package com.btmura.android.reddit.text;

import com.btmura.android.reddit.text.MarkdownFormatter.Tables;

public class MarkdownFormatter_TablesTest extends AbstractFormatterTest {

    public void testPattern_singleLine() {
        matcher = Tables.PATTERN.matcher("a|b|c");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(5, matcher.end());
        assertFalse(matcher.find());
    }

    public void testPattern_emptyLastColumn() {
        matcher = Tables.PATTERN.matcher("a|b|");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(4, matcher.end());
        assertFalse(matcher.find());
    }

    public void testPattern_surrounded() {
        matcher = Tables.PATTERN.matcher("hello\na|b|c\nbye");
        assertTrue(matcher.find());
        assertEquals(6, matcher.start());
        assertEquals(12, matcher.end());
        assertFalse(matcher.find());
    }

    public void testPattern_multiLine() {
        matcher = Tables.PATTERN.matcher("a|b|c\nd|e|f\ng|h|i");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(17, matcher.end());
        assertFalse(matcher.find());
    }
}
