package com.btmura.android.reddit.data;

import android.test.AndroidTestCase;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

import com.btmura.android.reddit.data.Formatter.Escaped;
import com.btmura.android.reddit.data.Formatter.Styles;
import com.btmura.android.reddit.data.Formatter.Subreddits;

abstract class AbstractFormatterTest extends AndroidTestCase {

    static void assertEscapedFormat(String input, String expected) {
        String actual = Escaped.format(input).toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
    }

    static CharSequence assertStyleFormat(int style, String input, String expected) {
        CharSequence cs = Styles.format(input, style);
        String actual = cs.toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
        return cs;
    }

    static CharSequence assertBulletFormat(String input, String expected) {
        CharSequence cs = Formatter.Bullets.format(input);
        String actual = cs.toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
        return cs;
    }

    static CharSequence assertRawLinksFormat(String input, String expected) {
        CharSequence cs = Formatter.RawLinks.format(input);
        String actual = cs.toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
        return cs;
    }

    static CharSequence assertNamedLinksFormat(String input, String expected) {
        CharSequence cs = Formatter.NamedLinks.format(input);
        String actual = cs.toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
        return cs;
    }

    static CharSequence assertSubredditFormat(String input, String expected) {
        CharSequence cs = Subreddits.format(input);
        String actual = cs.toString();
        assertEquals("Expected: " + expected + " Actual: " + actual, expected, actual);
        return cs;
    }

    static void assertStyleSpan(CharSequence cs, int start, int end, int expectedStyle) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        StyleSpan[] spans = b.getSpans(start, end, StyleSpan.class);
        assertEquals(expectedStyle, spans[0].getStyle());
    }

    static void assertBulletSpan(CharSequence cs, int start, int end) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        BulletSpan[] spans = b.getSpans(start, end, BulletSpan.class);
        assertEquals(1, spans.length);
    }

    static void assertStrikethroughSpan(CharSequence cs, int start, int end) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        StrikethroughSpan[] spans = b.getSpans(start, end, StrikethroughSpan.class);
        assertEquals(1, spans.length);
    }

    static void assertSubredditSpan(CharSequence cs, int start, int end, String expectedUrl) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        SubredditSpan[] spans = b.getSpans(start, end, SubredditSpan.class);
        assertEquals(1, spans.length);
        assertEquals("assertSubredditSpan expected: " + expectedUrl + " actual: "
                + spans[0].subreddit, expectedUrl, spans[0].subreddit);
    }

    static void assertUrlSpan(CharSequence cs, int start, int end, String expectedUrl) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        URLSpan[] spans = b.getSpans(start, end, URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("assertUrlSpan expected: " + expectedUrl + " actual: " + spans[0].getURL(),
                expectedUrl, spans[0].getURL());
    }

    static void assertImageSpan(CharSequence cs, int start, int end) {
        SpannableStringBuilder b = (SpannableStringBuilder) cs;
        ImageSpan[] spans = b.getSpans(start, end, ImageSpan.class);
        assertEquals(1, spans.length);
    }
}
