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
	private static Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*");
	private static Pattern STRIKE_THROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
	private static Pattern ESCAPED_PATTERN = Pattern.compile("&([A-Za-z]+?);");
	private static Pattern NAMED_LINK_PATTERN = Pattern.compile("\\[([^\\]]*?)\\]\\(([^\\)]+?)\\)");
	private static Pattern RAW_LINK_PATTERN = Pattern.compile("http[s]?://([A-Za-z0-9\\./\\-_#\\?&=;,+%']+)");
	
	private static final Matcher m = BOLD_PATTERN.matcher("");
	
	public static CharSequence formatTitle(String text) {
		if (text.indexOf("&") != -1) {
			SpannableStringBuilder b = new SpannableStringBuilder(text);
			m.usePattern(ESCAPED_PATTERN);
			m.reset(b);
			formatEscaped(b, m);
			return b;
		}
		return text;
	}
	
	public static CharSequence format(String text) {
		SpannableStringBuilder b = null;
	
		if (text.indexOf("**") != -1) {
			m.usePattern(BOLD_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatBold(b, m);
		}
		
		if (text.indexOf("*") != -1) {
			m.usePattern(ITALIC_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatItalic(b, m);
		}
		
		if (text.indexOf("~~") != -1) {
			m.usePattern(STRIKE_THROUGH_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatStrikeThrough(b, m);
		}
		
		if (text.indexOf("&") != -1) {
			m.usePattern(ESCAPED_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatEscaped(b, m);
		}
		
		if (text.indexOf("[") != -1) {
			m.usePattern(NAMED_LINK_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatNamedLinks(b, m);
		}
		
		if (text.indexOf("http") != -1) {
			m.usePattern(RAW_LINK_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			m.reset(b);
			formatRawLinks(b, m);
		}
		
		return b != null ? b : text;
	}
	
	private static void formatBold(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += 4;			
			
			StyleSpan span = new StyleSpan(Typeface.BOLD);
			b.setSpan(span, s, s + value.length(), 0);
		}
	}
	
	private static void formatItalic(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += 2;			
			
			StyleSpan span = new StyleSpan(Typeface.ITALIC);
			b.setSpan(span, s, s + value.length(), 0);
		}
	}
	
	private static void formatStrikeThrough(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += 4;			
			
			StrikethroughSpan span = new StrikethroughSpan();
			b.setSpan(span, s, s + value.length(), 0);
		}
	}
	
	private static void formatEscaped(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			
			deleted += 2;
			if ("amp".equals(value)) {
				b.replace(s, e, "&");
				deleted += 2;
			} else if ("gt".equals(value)) {
				b.replace(s, e, ">");
				deleted += 1;
			} else if ("lt".equals(value)) {
				b.replace(s, e, "<");
				deleted += 1;
			} else if ("quot".equals(value)) {
				b.replace(s, e, "\"");
				deleted += 3;
			} else if ("apos".equals(value)) {
				b.replace(s, e, "'");
				deleted += 3;
			} else if ("nbsp".equals(value)) {
				b.replace(s, e, " ");
				deleted += 3;
			}
		}
	}
	
	private static void formatNamedLinks(SpannableStringBuilder b, Matcher m) {
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
	}
	
	private static void formatRawLinks(SpannableStringBuilder b, Matcher m) {
		while (m.find()) {
			URLSpan span = new URLSpan(m.group());
			b.setSpan(span, m.start(), m.end(), 0);
		}
	}
}
