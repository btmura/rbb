package com.btmura.android.reddit.data;

import android.test.AndroidTestCase;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

public class FormatterTest extends AndroidTestCase {
	
	public void testFormatNamedLinks() {
		SpannableStringBuilder b = formatNamedLinks("[Link](/abc) is here");
		assertEquals("Link is here", b.toString());
		assertUrlSpan(b, 0, 4, "http://www.reddit.com/abc");
	}
	
	public void testFormatNamedLinks_nestedParens() {
		SpannableStringBuilder b = formatNamedLinks("Here is a [link](/abc (123) (456)).");
		assertEquals("Here is a link.", b.toString());
	}
	
	public void testFormatNamedLinks_nestedParensMultipleLines() {
		SpannableStringBuilder b = formatNamedLinks("[Link 1](/a (123))\n[Link 2](/b (456))");
		assertEquals("Link 1\nLink 2", b.toString());
	}
	
	private SpannableStringBuilder formatNamedLinks(String text) {
		SpannableStringBuilder b = new SpannableStringBuilder(text);
		Formatter.formatNamedLinks(b, Formatter.NAMED_LINK_PATTERN.matcher(text));
		return b;
	}
	
	private void assertUrlSpan(SpannableStringBuilder b, int start, int end, String expectedUrl) {
		URLSpan[] spans = b.getSpans(start, end, URLSpan.class);
		assertEquals(1, spans.length);
		assertEquals(expectedUrl, spans[0].getURL());
		
	}
}
