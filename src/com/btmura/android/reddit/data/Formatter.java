package com.btmura.android.reddit.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;

import com.btmura.android.reddit.R;

public class Formatter {

    public static CharSequence formatTitle(Context context, CharSequence title) {
        CharSequence c = Escaped.format(title);
        c = Disapproval.format(context, c);
        return c;
    }

    public static CharSequence formatComment(Context context, CharSequence comment) {
        CharSequence c = Escaped.format(comment);
        c = Styles.format(c, Styles.STYLE_BOLD);
        c = Styles.format(c, Styles.STYLE_ITALIC);
        c = Styles.format(c, Styles.STYLE_STRIKETHROUGH);
        c = Bullets.format(c);
        c = NamedLinks.format(c);
        c = RawLinks.format(c);
        c = Subreddits.format(c);
        c = Disapproval.format(context, c);
        return c;
    }

    static class Escaped {

        private static final Pattern PATTERN = Pattern.compile("&(gt|lt|amp|quot|apos|nbsp);");

        static CharSequence format(CharSequence text) {
            CharSequence s = text;
            Matcher m = PATTERN.matcher(text);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                String value = m.group(1);

                deleted += 2;
                if ("amp".equals(value)) {
                    s = Formatter.replace(s, start, end, "&");
                    deleted += 2;
                } else if ("gt".equals(value)) {
                    s = Formatter.replace(s, start, end, ">");
                    deleted += 1;
                } else if ("lt".equals(value)) {
                    s = Formatter.replace(s, start, end, "<");
                    deleted += 1;
                } else if ("quot".equals(value)) {
                    s = Formatter.replace(s, start, end, "\"");
                    deleted += 3;
                } else if ("apos".equals(value)) {
                    s = Formatter.replace(s, start, end, "'");
                    deleted += 3;
                } else if ("nbsp".equals(value)) {
                    s = Formatter.replace(s, start, end, " ");
                    deleted += 3;
                } else {
                    throw new IllegalStateException();
                }
            }
            return s;
        }
    }

    static class Styles {

        static final int STYLE_BOLD = 0;
        static final int STYLE_ITALIC = 1;
        static final int STYLE_STRIKETHROUGH = 2;

        private static final Pattern PATTERN_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
        private static final Pattern PATTERN_ITALIC = Pattern.compile("\\*(.+?)\\*");
        private static final Pattern PATTERN_STRIKETHROUGH = Pattern.compile("~~(.+?)~~");

        static CharSequence format(CharSequence text, int style) {
            Pattern p = null;
            int charsDeleted = -1;
            switch (style) {
                case STYLE_BOLD:
                    p = PATTERN_BOLD;
                    charsDeleted = 4;
                    break;

                case STYLE_ITALIC:
                    p = PATTERN_ITALIC;
                    charsDeleted = 2;
                    break;

                case STYLE_STRIKETHROUGH:
                    p = PATTERN_STRIKETHROUGH;
                    charsDeleted = 4;
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported style: " + style);
            }

            Matcher m = p.matcher(text);
            CharSequence s = text;

            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                String value = m.group(1);
                s = Formatter.replace(s, start, end, value);
                deleted += charsDeleted;

                Object span = null;
                switch (style) {
                    case STYLE_BOLD:
                        span = new StyleSpan(Typeface.BOLD);
                        break;

                    case STYLE_ITALIC:
                        span = new StyleSpan(Typeface.ITALIC);
                        break;

                    case STYLE_STRIKETHROUGH:
                        span = new StrikethroughSpan();
                        break;

                    default:
                        throw new IllegalArgumentException("Unsupported style: " + style);
                }

                s = Formatter.setSpan(s, start, start + value.length(), span);
            }

            return s;
        }
    }

    static class Bullets {

        private static Pattern PATTERN = Pattern.compile("^\\* (.+)$", Pattern.MULTILINE);

        static CharSequence format(CharSequence text) {
            CharSequence s = text;
            Matcher m = PATTERN.matcher(text);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                String value = m.group(1);

                s = Formatter.setSpan(s, start, end, new BulletSpan(20));
                s = Formatter.replace(s, start, end, value);
                deleted += 2;
            }
            return s;
        }
    }

    static class RawLinks {

        private static final Pattern PATTERN = Pattern
                .compile("https?://[^ $]+", Pattern.MULTILINE);

        static CharSequence format(CharSequence text) {
            CharSequence s = text;
            Matcher m = PATTERN.matcher(text);
            while (m.find()) {
                String url = m.group();
                URLSpan span = new URLSpan(url);
                s = Formatter.setSpan(s, m.start(), m.end(), span);
            }
            return s;
        }
    }

    static class NamedLinks {

        static CharSequence format(CharSequence text) {
            CharSequence s = text;
            for (int i = 0; i < s.length();) {
                int startBrack = TextUtils.indexOf(s, '[', i);
                if (startBrack == -1) {
                    break;
                }

                int endBrack = findClosingMarker(s, '[', ']', startBrack + 1);
                if (endBrack == -1) {
                    i = moveToNextLine(s, startBrack + 1);
                    continue;
                }

                int startParen = findOpeningParen(s, '(', endBrack + 1);
                if (startParen == -1) {
                    i = moveToNextLine(s, endBrack + 1);
                    continue;
                }

                int endParen = findClosingMarker(s, '(', ')', startParen + 1);
                if (endParen == -1) {
                    i = moveToNextLine(s, startParen + 1);
                    continue;
                }

                int endUrl = TextUtils.indexOf(s, ' ', startParen + 1, endParen);
                if (endUrl == -1) {
                    endUrl = endParen;
                }

                String url = s.subSequence(startParen + 1, endUrl).toString();
                Object span = Formatter.getUrlSpan(url);

                s = Formatter.setSpan(s, startBrack + 1, endParen, span);
                s = Formatter.delete(s, startBrack, startBrack + 1);
                s = Formatter.delete(s, endBrack - 1, endParen);
            }
            return s;
        }

        private static int findOpeningParen(CharSequence s, char c, int start) {
            boolean spaceFound = false;
            int len = s.length();
            for (int i = start; i < len; i++) {
                char ch = s.charAt(i);
                if (ch == c) {
                    return i;
                } else if (ch == ' ') {
                    if (spaceFound) {
                        break;
                    }
                    spaceFound = true;
                } else {
                    break;
                }
            }
            return -1;
        }

        private static int findClosingMarker(CharSequence s, char open, char close, int start) {
            int nesting = 0;
            int len = s.length();
            for (int i = start; i < len; i++) {
                char ch = s.charAt(i);
                if (ch == '\n') {
                    break;
                } else if (ch == open) {
                    nesting++;
                } else if (ch == close) {
                    if (nesting == 0) {
                        return i;
                    }
                    nesting--;
                }
            }
            return -1;
        }
        private static int moveToNextLine(CharSequence s, int start) {
            int len = s.length();
            if (start < len) {
                int eol = TextUtils.indexOf(s, '\n', start);
                return eol == -1 ? len : eol + 1;
            } else {
                return len;
            }
        }
    }

    static class Subreddits {

        static Pattern SUBREDDIT_PATTERN = Pattern.compile("/r/([0-9A-Za-z_]+)/?");

        static CharSequence format(CharSequence text) {
            CharSequence s = text;
            Matcher m = SUBREDDIT_PATTERN.matcher(s);
            while (m.find()) {
                SubredditSpan span = new SubredditSpan(m.group(1));
                s = Formatter.setSpan(s, m.start(), m.end(), span);
            }
            return s;
        }
    }

    static class Disapproval {

        static Pattern DISAPPROVAL_PATTERN = Pattern.compile("(ಠ_ಠ|&#3232;\\\\_&#3232;)");

        static CharSequence format(Context context, CharSequence text) {
            CharSequence s = text;
            Matcher m = DISAPPROVAL_PATTERN.matcher(s);
            for (; m.find();) {
                ImageSpan span = new ImageSpan(context, R.drawable.disapproval_face,
                        ImageSpan.ALIGN_BOTTOM);
                s = setSpan(s, m.start(), m.end(), span);
            }
            return s;
        }
    }

    static CharSequence replace(CharSequence s, int start, int end, String r) {
        if (!(s instanceof SpannableStringBuilder)) {
            s = new SpannableStringBuilder(s);
        }
        SpannableStringBuilder b = (SpannableStringBuilder) s;
        b.replace(start, end, r);
        return s;
    }

    static CharSequence delete(CharSequence s, int start, int end) {
        if (!(s instanceof SpannableStringBuilder)) {
            s = new SpannableStringBuilder(s);
        }
        SpannableStringBuilder b = (SpannableStringBuilder) s;
        b.delete(start, end);
        return s;
    }

    static CharSequence setSpan(CharSequence s, int start, int end, Object span) {
        if (!(s instanceof SpannableStringBuilder)) {
            s = new SpannableStringBuilder(s);
        }
        SpannableStringBuilder b = (SpannableStringBuilder) s;
        b.setSpan(span, start, end, 0);
        return s;
    }

    private static final String REDDIT_URL = "http://www.reddit.com";
    private static final StringBuilder TMP = new StringBuilder();

    static Object getUrlSpan(String url) {
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
