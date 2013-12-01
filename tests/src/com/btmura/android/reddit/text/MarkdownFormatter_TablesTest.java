package com.btmura.android.reddit.text;

import com.btmura.android.reddit.text.MarkdownFormatter.Tables;

public class MarkdownFormatter_TablesTest extends AbstractFormatterTest {

    public void testPattern_singleLine() {
        matcher = Tables.PATTERN.matcher("a|b|c");
        assertFalse(matcher.find());
    }

    public void testPattern_emptyLastColumn() {
        matcher = Tables.PATTERN.matcher("h1|h2|h3\n-|-|-\na|b|");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(19, matcher.end());
        assertFalse(matcher.find());
    }

    public void testPattern_surrounded() {
        matcher = Tables.PATTERN.matcher("hello\nh1|h2|h3\n-|-|-\na|b|c\nbye");
        assertTrue(matcher.find());
        assertEquals(6, matcher.start());
        assertEquals(27, matcher.end());
        assertFalse(matcher.find());
    }
}
