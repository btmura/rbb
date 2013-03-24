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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Patterns;

import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.style.SubredditSpan;
import com.btmura.android.reddit.text.style.URLSpan;
import com.btmura.android.reddit.text.style.UserSpan;

public class Formatter {

    private final Matcher matcher = RawLinks.PATTERN.matcher("");
    private final StringBuilder builder = new StringBuilder();

    public CharSequence formatNoSpans(Context context, CharSequence c) {
        if (c != null) {
            c = Escaped.format(matcher, c);
            return Disapproval.format(context, matcher, c);
        }
        return null;
    }

    public CharSequence formatSpans(Context context, CharSequence c) {
        if (c != null) {
            c = Styles.format(matcher, c, Styles.STYLE_BOLD);
            c = Styles.format(matcher, c, Styles.STYLE_ITALIC);
            c = Styles.format(matcher, c, Styles.STYLE_STRIKETHROUGH);
            c = Heading.format(matcher, c);
            c = Bullets.format(matcher, c);
            c = CodeBlock.format(matcher, c);
            c = NamedLinks.format(c, builder);
            c = RawLinks.format(matcher, c);
            return RelativeLinks.format(matcher, c);
        }
        return null;
    }

    public CharSequence formatAll(Context context, CharSequence c) {
        if (c != null) {
            c = formatNoSpans(context, c);
            return formatSpans(context, c);
        }
        return null;
    }

    static class Escaped {

        private static final Pattern AMP_PATTERN = Pattern.compile("&(amp);");
        private static final Pattern FULL_PATTERN = Pattern.compile("&(gt|lt|amp|quot|apos|nbsp|mdash);");

        static CharSequence format(Matcher matcher, CharSequence text) {
            return format(FULL_PATTERN, matcher, format(AMP_PATTERN, matcher, text));
        }

        static CharSequence format(Pattern pattern, Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(pattern).reset(text);
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
                } else if ("mdash".equals(value)) {
                    s = Formatter.replace(s, start, end, "—");
                    deleted += 4;
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

        static CharSequence format(Matcher matcher, CharSequence text, int style) {
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

            Matcher m = matcher.usePattern(p).reset(text);
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

        private static Pattern PATTERN = Pattern.compile("^( *\\* )(.+)$", Pattern.MULTILINE);

        static CharSequence format(Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(PATTERN).reset(text);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                deleted += m.group(1).length();
                String value = m.group(2);

                s = Formatter.setSpan(s, start, end, new BulletSpan(20));
                s = Formatter.replace(s, start, end, value);
            }
            return s;
        }
    }

    static class CodeBlock {

        static Pattern PATTERN_CODE_BLOCK = Pattern.compile("^(    |\t)(.*)$", Pattern.MULTILINE);

        static CharSequence format(Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(PATTERN_CODE_BLOCK).reset(text);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                deleted += m.group(1).length();
                String value = m.group(2);

                s = Formatter.setSpan(s, start, end, new TypefaceSpan("monospace"));
                s = Formatter.replace(s, start, end, value);
            }
            return s;
        }
    }

    static class RawLinks {

        static final Pattern PATTERN = Patterns.WEB_URL;

        static CharSequence format(Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(PATTERN).reset(text);
            while (m.find()) {
                String url = m.group();
                URLSpan span = new URLSpan(url);
                s = Formatter.setSpan(s, m.start(), m.end(), span);
            }
            return s;
        }
    }

    static class NamedLinks {

        static CharSequence format(CharSequence text, StringBuilder builder) {
            CharSequence s = text;
            for (int i = 0; i < s.length();) {
                int startBrack = TextUtils.indexOf(s, '[', i);
                if (startBrack == -1) {
                    break;
                }

                int endBrack = findClosingMarker(s, '[', ']', startBrack + 1);
                if (endBrack == -1) {
                    i = startBrack + 1;
                    continue;
                }

                int startParen = findOpeningParen(s, '(', endBrack + 1);
                if (startParen == -1) {
                    i = startBrack + 1;
                    continue;
                }

                int endParen = findClosingMarker(s, '(', ')', startParen + 1);
                if (endParen == -1) {
                    i = startParen + 1;
                    continue;
                }

                int endUrl = TextUtils.indexOf(s, ' ', startParen + 1, endParen);
                if (endUrl == -1) {
                    endUrl = endParen;
                }

                String url = s.subSequence(startParen + 1, endUrl).toString();
                Object span = Formatter.getUrlSpan(url, builder);

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
                } else if (ch == ' ' || ch == '\n') {
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
                if (ch == open) {
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
    }

    static class RelativeLinks {

        static Pattern RELATIVE_LINK_PATTERN = Pattern.compile("/([ru])/([0-9A-Za-z_+]+)/?");

        static CharSequence format(Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(RELATIVE_LINK_PATTERN).reset(s);
            while (m.find()) {
                String value = m.group(2);
                ClickableSpan span;
                if ("r".equals(m.group(1))) {
                    span = new SubredditSpan(value);
                } else {
                    span = new UserSpan(value);
                }
                s = Formatter.setSpan(s, m.start(), m.end(), span);
            }
            return s;
        }
    }

    static class Disapproval {

        static final Pattern PATTERN = Pattern.compile("&#3232;\\\\_&#3232;");
        static final String FACE = "ಠ_ಠ";

        static CharSequence format(Context context, Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(PATTERN).reset(s);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                deleted += m.end() - m.start() - FACE.length();
                s = Formatter.replace(s, start, end, FACE);
            }
            return s;
        }
    }

    static class Heading {

        private static Pattern PATTERN = Pattern.compile("^(#{1,} ?)(.+?)(#*)$", Pattern.MULTILINE);

        static CharSequence format(Matcher matcher, CharSequence text) {
            CharSequence s = text;
            Matcher m = matcher.usePattern(PATTERN).reset(text);
            for (int deleted = 0; m.find();) {
                int start = m.start() - deleted;
                int end = m.end() - deleted;
                deleted += m.group(1).length() + m.group(3).length();
                String value = m.group(2);

                s = Formatter.setSpan(s, start, end, new StyleSpan(Typeface.BOLD_ITALIC));
                s = Formatter.replace(s, start, end, value);
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

    static Object getUrlSpan(String url, StringBuilder builder) {
        if (url.startsWith("/")) {
            url = builder.delete(0, builder.length()).append(Urls.BASE_URL).append(url).toString();
            return new URLSpan(url);
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = builder.delete(0, builder.length()).append("http://").append(url).toString();
            return new URLSpan(url);
        } else {
            url = builder.delete(0, builder.length()).append(url).toString();
            return new URLSpan(url);
        }
    }
}
