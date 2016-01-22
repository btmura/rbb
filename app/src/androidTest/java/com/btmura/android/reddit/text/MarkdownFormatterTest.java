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

public class MarkdownFormatterTest extends AbstractFormatterTest {

  protected MarkdownFormatter formatter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    formatter = new MarkdownFormatter();
  }

  public void testFormatAll() {
    assertFormatAll("this is **&amp;lt;bold&gt;** text",
        "this is <bold> text");
    assertFormatAll(
        "this is &gt;*italics&amp;lt;* and this is ~~strikethrough~~",
        "this is >italics< and this is strikethrough");
    assertFormatAll("    code **line** 1\nnot a code line\n\tcode *line* 2",
        "code **line** 1\nnot a code line\ncode *line* 2");
    assertFormatAll("    **bullet1\n\t[hello](http://hello.com)\n\t### HEADING",
        "**bullet1\n[hello](http://hello.com)\n### HEADING");
  }

  private void assertFormatAll(String input, String expected) {
    CharSequence formatted = formatter.formatAll(mContext, input);
    String actual = formatted.toString();
    assertEquals("expected: " + expected + " actual: " + actual, expected,
        actual);
  }
}
