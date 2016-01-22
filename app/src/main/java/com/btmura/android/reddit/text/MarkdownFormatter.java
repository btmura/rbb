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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.text.style.MarkdownTableSpan;
import com.btmura.android.reddit.text.style.SubredditSpan;
import com.btmura.android.reddit.text.style.URLSpan;
import com.btmura.android.reddit.text.style.UserSpan;
import com.btmura.android.reddit.util.Array;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFormatter {

  private final Matcher matcher = RawLinks.PATTERN.matcher("");
  private final StringBuilder builder = new StringBuilder();

  public CharSequence formatNoSpans(CharSequence c) {
    if (!TextUtils.isEmpty(c)) {
      return Escaped.format(matcher, c);
    }
    return "";
  }

  public CharSequence formatSpans(Context ctx, CharSequence c) {
    if (!TextUtils.isEmpty(c)) {
      c = CodeBlock.format(matcher, c);
      c = Styles.format(matcher, c, Styles.STYLE_BOLD);
      c = Styles.format(matcher, c, Styles.STYLE_ITALIC);
      c = Styles.format(matcher, c, Styles.STYLE_STRIKETHROUGH);
      c = Heading.format(matcher, c);
      c = Bullets.format(matcher, c);
      c = NamedLinks.format(c, builder);
      c = RawLinks.format(matcher, c);
      c = Tables.format(ctx, matcher, c);
      return RelativeLinks.format(matcher, c);
    }
    return "";
  }

  public CharSequence formatAll(Context ctx, CharSequence c) {
    if (!TextUtils.isEmpty(c)) {
      c = formatNoSpans(c);
      return formatSpans(ctx, c);
    }
    return "";
  }

  static class Escaped {

    private static final Pattern AMP_PATTERN = Pattern.compile("&(amp);");
    private static final Pattern FULL_PATTERN = Pattern.compile(
        "&(gt|lt|amp|quot|apos|nbsp|mdash|#(\\d+)|#([Xx])([0-9A-Za-z]+));");

    static CharSequence format(Matcher matcher, CharSequence text) {
      return format(FULL_PATTERN, matcher, format(AMP_PATTERN, matcher, text));
    }

    static CharSequence format(
        Pattern pattern,
        Matcher matcher,
        CharSequence text) {
      CharSequence s = text;
      Matcher m = matcher.usePattern(pattern).reset(text);
      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;
        String value = m.group(1);

        deleted += 2;
        if ("amp".equals(value)) {
          s = replace(s, start, end, "&");
          deleted += 2;
        } else if ("gt".equals(value)) {
          s = replace(s, start, end, ">");
          deleted += 1;
        } else if ("lt".equals(value)) {
          s = replace(s, start, end, "<");
          deleted += 1;
        } else if ("quot".equals(value)) {
          s = replace(s, start, end, "\"");
          deleted += 3;
        } else if ("apos".equals(value)) {
          s = replace(s, start, end, "'");
          deleted += 3;
        } else if ("nbsp".equals(value)) {
          s = replace(s, start, end, " ");
          deleted += 3;
        } else if ("mdash".equals(value)) {
          s = replace(s, start, end, "—");
          deleted += 4;
        } else {
          String r = decodeReference(m);
          s = replace(s, start, end, r);
          deleted += value.length() - r.length();
        }
      }
      return s;
    }

    private static String decodeReference(Matcher m) {
      int radix = m.group(3) == null ? 10 : 16;
      String num = radix == 10 ? m.group(2) : m.group(4);
      return String.valueOf(Character.toChars(Integer.parseInt(num, radix)));
    }
  }

  static class CodeBlock {

    static Pattern PATTERN_CODE_BLOCK =
        Pattern.compile("(?m)^(    |\t)(?:.*)$");

    static CharSequence format(Matcher matcher, CharSequence text) {
      CharSequence s = text;
      Matcher m = matcher.usePattern(PATTERN_CODE_BLOCK).reset(text);
      int totalStart = -1;
      int totalEnd = -1;
      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;

        // Remove the leading indentation on the line.
        int headStart = start;
        int headEnd = start + m.group(1).length();
        s = delete(s, headStart, headEnd);

        // Update the end of the match and the deleted count.
        end -= m.group(1).length();
        deleted += m.group(1).length();

        // Continuing line. Update the end but don't span since there may be more lines.
        if (totalEnd != -1 && totalEnd + 1 == start) {
          totalEnd = end;
          continue;
        }

        // New block or 1st time. Set the span on the prior block and reset the markers.
        if (totalStart != -1) {
          s = setSpan(s, totalStart, totalEnd, new TypefaceSpan("monospace"));
        }
        totalStart = start;
        totalEnd = end;
      }

      // There may not have been a new match to flush out the pending block so do it here.
      if (totalStart != -1) {
        s = setSpan(s, totalStart, totalEnd, new TypefaceSpan("monospace"));
      }

      return s;
    }

    static boolean isCodeBlock(CharSequence text, int start, int end) {
      // If we're not a SpannableStringBuilder then there are no spans at all yet.
      if (!(text instanceof SpannableStringBuilder)) {
        return false;
      }
      SpannableStringBuilder s = (SpannableStringBuilder) text;

      // Assume a TypefaceSpan means a code block, since nobody applies other typefaces yet.
      TypefaceSpan[] spans = s.getSpans(start, end, TypefaceSpan.class);
      return !Array.isEmpty(spans);
    }
  }

  static class Styles {

    static final int STYLE_BOLD = 0;
    static final int STYLE_ITALIC = 1;
    static final int STYLE_STRIKETHROUGH = 2;

    private static final Pattern PATTERN_BOLD =
        Pattern.compile("\\*\\*.+?\\*\\*");
    private static final Pattern PATTERN_ITALIC = Pattern.compile("\\*.+?\\*");
    private static final Pattern PATTERN_STRIKETHROUGH =
        Pattern.compile("~~.+?~~");

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

      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;

        // Don't apply formatting within code blocks.
        if (CodeBlock.isCodeBlock(s, start, end)) {
          continue;
        }

        int headStart = start;
        int headEnd = start + charsDeleted / 2;
        s = delete(s, headStart, headEnd);

        int tailEnd = end - charsDeleted / 2;
        int tailStart = tailEnd - charsDeleted / 2;
        s = delete(s, tailStart, tailEnd);

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

        int totalStart = start;
        int totalEnd = end - charsDeleted;
        s = setSpan(s, totalStart, totalEnd, span);

        deleted += charsDeleted;
      }

      return s;
    }
  }

  static class Bullets {

    private static Pattern PATTERN = Pattern.compile("(?m)^( *[*+-] )(?:.+)$");

    static CharSequence format(Matcher matcher, CharSequence text) {
      CharSequence s = text;
      Matcher m = matcher.usePattern(PATTERN).reset(text);
      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;

        // Don't apply formatting within code blocks.
        if (CodeBlock.isCodeBlock(s, start, end)) {
          continue;
        }

        // Apply the bullet span to the entire line.
        s = setSpan(s, start, end, new BulletSpan(20));

        // Delete the beginning *s.
        int headStart = start;
        int headEnd = start + m.group(1).length();
        s = delete(s, headStart, headEnd);

        // Increment how much we deleted.
        deleted += m.group(1).length();
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
        s = setSpan(s, m.start(), m.end(), span);
      }
      return s;
    }
  }

  static class NamedLinks {

    static CharSequence format(CharSequence text, StringBuilder builder) {
      CharSequence s = text;
      for (int i = 0; i < s.length(); ) {
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
          i = startBrack + 1;
          continue;
        }

        // Don't apply formatting within code blocks.
        if (CodeBlock.isCodeBlock(s, startBrack, endParen)) {
          i = startBrack + 1;
          continue;
        }

        int endUrl = TextUtils.indexOf(s, ' ', startParen + 1, endParen);
        if (endUrl == -1) {
          endUrl = endParen;
        }

        String url = s.subSequence(startParen + 1, endUrl).toString();
        Object span = getUrlSpan(url, builder);

        s = setSpan(s, startBrack + 1, endParen, span);
        s = delete(s, startBrack, startBrack + 1);
        s = delete(s, endBrack - 1, endParen);
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

    private static int findClosingMarker(
        CharSequence s,
        char open,
        char close,
        int start) {
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

    static Pattern RELATIVE_LINK_PATTERN =
        Pattern.compile("/([ru])/([0-9A-Za-z_+]+)/?");

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
        s = setSpan(s, m.start(), m.end(), span);
      }
      return s;
    }
  }

  static class Heading {

    private static Pattern PATTERN =
        Pattern.compile("(?m)^(#{1,} ?)(?:.+?)(#*)$");

    static CharSequence format(Matcher matcher, CharSequence text) {
      CharSequence s = text;
      Matcher m = matcher.usePattern(PATTERN).reset(text);
      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;

        // Don't apply formatting within code blocks.
        if (CodeBlock.isCodeBlock(s, start, end)) {
          continue;
        }

        // Apply the span to the entire matching line.
        s = setSpan(s, start, end, new StyleSpan(Typeface.BOLD_ITALIC));

        // Trim off the beginning #s
        int headStart = start;
        int headEnd = start + m.group(1).length();
        s = delete(s, headStart, headEnd);

        // Trim off the ending #s
        int tailEnd = end - m.group(1).length();
        int tailStart = tailEnd - m.group(2).length();
        s = delete(s, tailStart, tailEnd);

        // Increment how much we deleted.
        deleted += m.group(1).length() + m.group(2).length();
      }
      return s;
    }
  }

  static class Tables {

    static final Pattern PATTERN = Pattern.compile("(?m)(?:"
        + "^.*\\|.*[\\r\\n]" // Header row
        + "(?:[-:]*\\|)+.*[\\r\\n]" // Justification row with dashes, colons, or nothing.
        + "(?:^.*\\|.*[\\r\\n]?){1,}" // 1 or more data rows.
        + ")");

    static CharSequence format(
        Context context,
        Matcher matcher,
        CharSequence text) {
      CharSequence s = text;
      Matcher m = matcher.usePattern(PATTERN).reset(text);
      for (int deleted = 0; m.find(); ) {
        int start = m.start() - deleted;
        int end = m.end() - deleted;

        // Don't apply formatting within code blocks.
        if (CodeBlock.isCodeBlock(s, start, end)) {
          continue;
        }

        String replacement = context.getString(R.string.view_table);
        deleted += m.group().length() - replacement.length();
        s = setSpan(s, start, end, new MarkdownTableSpan(m.group()));
        s = replace(s, start, end, replacement);
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
      url = builder.delete(0, builder.length())
          .append(Urls.WWW_REDDIT_COM)
          .append(url)
          .toString();
      return new URLSpan(url);
    } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = builder.delete(0, builder.length())
          .append("http://")
          .append(url)
          .toString();
      return new URLSpan(url);
    } else {
      url = builder.delete(0, builder.length())
          .append(url)
          .toString();
      return new URLSpan(url);
    }
  }
}
