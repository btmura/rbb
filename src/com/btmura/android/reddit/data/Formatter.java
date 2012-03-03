package com.btmura.android.reddit.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

public class Formatter {
	private static Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
	private static Pattern BOLD2_PATTERN = Pattern.compile("###(.+?)###");
	private static Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*");
	private static Pattern STRIKE_THROUGH_PATTERN = Pattern.compile("~~(.+?)~~");
	private static Pattern ESCAPED_PATTERN = Pattern.compile("&([A-Za-z]+?);");
	private static Pattern BULLET_PATTERN = Pattern.compile("\\* ([^\\n]+)");
	static Pattern NAMED_LINK_PATTERN = Pattern.compile("\\[([^\\]]*?)\\]([ ]?)\\(([^\\[]*)\\)");
	private static Pattern RAW_LINK_PATTERN = Pattern.compile("http[s]?://([^ \\n]+)");
	private static Pattern SUBREDDIT_PATTERN = Pattern.compile("/r/([0-9A-Za-z_]+)");
	
	private static final int SPAN_BOLD = 0;
	private static final int SPAN_ITALIC = 1;
	private static final int SPAN_STRIKETHROUGH = 2;
	private static final int SPAN_BULLET = 3;

	private static final String REDDIT_URL = "http://www.reddit.com";
	
	private static final Matcher MATCHER = BOLD_PATTERN.matcher("");	
	private static final StringBuilder TMP = new StringBuilder();

	
	public static CharSequence formatTitle(String text) {
		if (text.indexOf("&") != -1) {
			SpannableStringBuilder b = new SpannableStringBuilder(text);
			MATCHER.usePattern(ESCAPED_PATTERN);
			MATCHER.reset(b);
			formatEscaped(b, MATCHER);
			return b;
		}
		return text;
	}
	
	public static CharSequence format(String text) {		
		SpannableStringBuilder b = null;
	
		if (text.indexOf("**") != -1) {
			MATCHER.usePattern(BOLD_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatBold(b, MATCHER);
		}
		
		if (text.indexOf("###") != -1) {
			MATCHER.usePattern(BOLD2_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatBold2(b, MATCHER);
		}
		
		if (text.indexOf("*") != -1) {
			MATCHER.usePattern(ITALIC_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatItalic(b, MATCHER);
		}
		
		if (text.indexOf("~~") != -1) {
			MATCHER.usePattern(STRIKE_THROUGH_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatStrikeThrough(b, MATCHER);
		}
		
		if (text.indexOf("&") != -1) {
			MATCHER.usePattern(ESCAPED_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatEscaped(b, MATCHER);
		}
		
		if (text.indexOf("*") != -1) {
			MATCHER.usePattern(BULLET_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatBullets(b, MATCHER);
		}
		
		if (text.indexOf("/r/") != -1) {
			MATCHER.usePattern(SUBREDDIT_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatSubreddits(b, MATCHER);
		}
		
		if (text.indexOf("[") != -1) {
			MATCHER.usePattern(NAMED_LINK_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatNamedLinks(b, MATCHER);
		}
		
		if (text.indexOf("http") != -1) {
			MATCHER.usePattern(RAW_LINK_PATTERN);
			if (b == null) {
				b = new SpannableStringBuilder(text);
			}
			MATCHER.reset(b);
			formatRawLinks(b, MATCHER);
		}
		
		return b != null ? b : text;
	}
	
	private static void formatBold(SpannableStringBuilder b, Matcher m) {
		replaceWithSpan(b, m, 4, SPAN_BOLD);
	}
	
	private static void formatBold2(SpannableStringBuilder b, Matcher m) {
		replaceWithSpan(b, m, 6, SPAN_BOLD);
	}
	
	private static void formatItalic(SpannableStringBuilder b, Matcher m) {
		replaceWithSpan(b, m, 2, SPAN_ITALIC);
	}
	
	private static void formatStrikeThrough(SpannableStringBuilder b, Matcher m) {
		replaceWithSpan(b, m, 4, SPAN_STRIKETHROUGH);
	}
	
	private static void formatBullets(SpannableStringBuilder b, Matcher m) {
		replaceWithSpan(b, m, 2, SPAN_BULLET);
	}
	
	private static void replaceWithSpan(SpannableStringBuilder b, Matcher m, int charsDeleted, int spanType) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String value = m.group(1);
			b.replace(s, e, value);
			deleted += charsDeleted;			
			
			Object span = null;
			switch (spanType) {
			case SPAN_BOLD:
				span = new StyleSpan(Typeface.BOLD);
				break;
				
			case SPAN_ITALIC:
				span = new StyleSpan(Typeface.ITALIC);
				break;
				
			case SPAN_STRIKETHROUGH:
				span = new StrikethroughSpan();
				break;
				
			case SPAN_BULLET:
				span = new BulletSpan(10);
				break;
			}
			
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
	
	static void formatSubreddits(SpannableStringBuilder b, Matcher m) {
		while (m.find()) {
			SubredditSpan s = new SubredditSpan(m.group(1));
			b.setSpan(s, m.start(), m.end(), 0);
		}
	}
	
	static void formatNamedLinks(SpannableStringBuilder b, Matcher m) {
		for (int deleted = 0; m.find(); ) {
			int s = m.start() - deleted;
			int e = m.end() - deleted;
			String name = m.group(1);
			String spacing = m.group(2);
			String url = m.group(3);
			b.replace(s, e, name);
			deleted += 4 + spacing.length() + url.length();
			
			int space = url.indexOf(" ");
			if (space != -1) {
				url = url.substring(0, space);
			}
			
			Object span = getLinkSpan(url);
			if (span != null) {
				b.setSpan(span, s, s + name.length(), 0);
			}
		}
	}
	
	static void formatRawLinks(SpannableStringBuilder b, Matcher m) {
		while (m.find()) {
			Object span = getLinkSpan(m.group());
			if (span != null) {
				b.setSpan(span, m.start(), m.end(), 0);
			}
		}
	}
	
	private static Object getLinkSpan(String url) {
		int srIndex = url.indexOf("/r/");
		if (srIndex != -1 && srIndex + 3 < url.length()) {
			int slash = url.indexOf('/', srIndex + 3);
			if (slash == -1) {
				return new SubredditSpan(url.substring(srIndex + 3));
			} else if (slash + 1 == url.length()) {
				return new SubredditSpan(url.substring(srIndex + 3, slash));
			}
		}
		
		Object span = null;
		if (url.startsWith("/")) {
			url = TMP.delete(0, TMP.length()).append(REDDIT_URL).append(url).toString();
			span = new URLSpan(url);
		} else if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = TMP.delete(0, TMP.length()).append("http://").append(url).toString();
			span = new URLSpan(url);
		} else {
			url = TMP.delete(0, TMP.length()).append(url).toString();
			span = new URLSpan(url);
		}
		return span;
	}
}
