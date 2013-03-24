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

import com.btmura.android.reddit.text.Formatter.CodeBlock;

public class Formatter_CodeBlockTest extends AbstractFormatterTest {

	private String[] lines;

	/**
	 * Tests that the underlying regular expression catches indented lines
	 * properly.
	 */
	public void testPattern() throws Exception {
		// Line with 3 spaces shouldn't match.
		setLines("   line1");
		assertFalse(matcher.find());

		// Line with some text before the indentation shouldn't match.
		setLines("first    line1");
		assertFalse(matcher.find());

		// Line with 4 spaces should match.
		setLines("    line1");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertFalse(matcher.find());

		// Line with 5 spaces should still match.
		setLines("     line1");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertFalse(matcher.find());

		// Line with 1 tab should match.
		setLines("\tline1");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertFalse(matcher.find());

		// Line with 2 tabs should match.
		setLines("\t\tline1");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertFalse(matcher.find());

		// Two lines with indentation and a third without.
		setLines("    line1", "    line2", "last");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertTrue(matcher.find());
		assertEquals(lines[1], matcher.group());
		assertFalse(matcher.find());

		// Two lines with spaces and tabs.
		setLines("    line1", "\tline2", "last");
		assertTrue(matcher.find());
		assertEquals(lines[0], matcher.group());
		assertTrue(matcher.find());
		assertEquals(lines[1], matcher.group());
		assertFalse(matcher.find());
	}

	private void setLines(String firstLine, String... moreLines) {
		int lineCount = 1;
		if (moreLines != null) {
			lineCount += moreLines.length;
		}

		// Save the lines in an array to refer to them easily in the test.
		lines = new String[lineCount];
		for (int i = 0; i < lineCount; i++) {
			lines[i] = i == 0 ? firstLine : moreLines[i - 1];
		}

		// Join all the lines together to pass to the matcher.
		StringBuilder joinedLines = new StringBuilder();
		joinedLines.append(firstLine);
		for (String line : moreLines) {
			joinedLines.append("\n").append(line);
		}

		// Set the lines on the matcher.
		matcher = CodeBlock.PATTERN_CODE_BLOCK.matcher(joinedLines);
	}

	/** Tests that the formatter properly applies spans to regions of the text. */
	public void testFormat() throws Exception {
		CharSequence cs = assertCodeBlockFormat("    line1", "line1");
		assertCodeBlockSpan(cs, 0, 5);
	}
}
