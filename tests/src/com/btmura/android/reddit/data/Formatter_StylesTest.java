package com.btmura.android.reddit.data;

import android.graphics.Typeface;

import com.btmura.android.reddit.data.Formatter.Styles;

public class Formatter_StylesTest  extends AbstractFormatterTest {

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
        CharSequence cs = assertStyleFormat(Styles.STYLE_STRIKETHROUGH, "~~strikethrough~~", "strikethrough");
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
