/*
 * Copyright (C) 2013 Brian Muramatsu
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

import com.btmura.android.reddit.text.MarkdownFormatter.Tables;

public class MarkdownFormatter_TablesTest extends AbstractFormatterTest {

  public void testPattern_singleLine() {
    matcher = Tables.PATTERN.matcher("a|b|c");
    assertFalse(matcher.find());
  }

  public void testPattern_emptyLastColumn() {
    matcher = Tables.PATTERN.matcher("h1|h2|h3\n-|-|-\na|b|");
    assertTrue(matcher.find());
    assertEquals(0, matcher.start());
    assertEquals(19, matcher.end());
    assertFalse(matcher.find());
  }

  public void testPattern_surrounded() {
    matcher = Tables.PATTERN.matcher("hello\nh1|h2|h3\n-|-|-\na|b|c\nbye");
    assertTrue(matcher.find());
    assertEquals(6, matcher.start());
    assertEquals(27, matcher.end());
    assertFalse(matcher.find());
  }

  public void testPattern_noJustificationDashes() {
    matcher = Tables.PATTERN.matcher("h1|h2|h3\n||\na|b|c");
    assertTrue(matcher.find());
    assertEquals(0, matcher.start());
    assertEquals(17, matcher.end());
    assertFalse(matcher.find());
  }

  public void testPattern_missingJustificationRow() {
    matcher = Tables.PATTERN.matcher("h1|h2|h3\na|b|c\nd|e|f");
    assertFalse(matcher.find());
  }
}
