package com.btmura.android.reddit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

public class Formatter {
	private static Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
	private static Pattern STRIKE_THROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
	private static Pattern ESCAPED_PATTERN = Pattern.compile("&([A-Za-z]+);");
	private static Pattern NAMED_LINK_PATTERN = Pattern.compile("\\[([^\\]]*?)\\]\\(([^\\)]+?)\\)");
	private static Pattern RAW_LINK_PATTERN = Pattern.compile("http[s]?://([A-Za-z0-9\\./\\-_#\\?&=;,+%]+)");
	
	public static SpannableStringBuilder formatTitle(CharSequence text) {
		SpannableStringBuilder b = new SpannableStringBuilder(text);
		Matcher m = ESCAPED_PATTERN.matcher(text);
		unescape(b, m);
		return b;
	}

	public static SpannableStringBuilder format(CharSequence text) {
		SpannableStringBuilder b = new SpannableStringBuilder(text);

		Matcher m = BOLD_PATTERN.matcher(text);
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += 4;			
			
			StyleSpan span = new StyleSpan(Typeface.BOLD);
			b.setSpan(span, s, s + value.length(), 0);
		}
		
		m.usePattern(STRIKE_THROUGH_PATTERN);
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += 4;			
			
			StrikethroughSpan span = new StrikethroughSpan();
			b.setSpan(span, s, s + value.length(), 0);
		}
		
		m.usePattern(ESCAPED_PATTERN);
		m.reset(b);
		unescape(b, m);

		m.usePattern(NAMED_LINK_PATTERN);
		m.reset(b);
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String name = m.group(1);
			String url = m.group(2);
			b.replace(s, e, name);
			deleted += 4 + url.length();
			
			if (url.startsWith("/r/")) {
				url = "http://www.reddit.com" + url;
			} else if (!url.startsWith("http://") && !url.startsWith("https://")) {
				url = "http://" + url;
			}
			
			URLSpan span = new URLSpan(url);
			b.setSpan(span, s, s + name.length(), 0);
		}
				
		m.usePattern(RAW_LINK_PATTERN);
		m.reset(b);
		while (m.find()) {
			URLSpan span = new URLSpan(m.group());
			b.setSpan(span, m.start(), m.end(), 0);
		}

		return b;
	}
	
	private static void unescape(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			
			if ("amp".equals(value)) {
				b.replace(s, e, "&");
				deleted += 4;
			} else if ("gt".equals(value)) {
				b.replace(s, e, ">");
				deleted += 3;
			} else if ("lt".equals(value)) {
				b.replace(s, e, "<");
				deleted += 3;
			}
		}
	}
}
