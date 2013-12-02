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

package com.btmura.android.reddit.util;

import android.view.Gravity;
import junit.framework.TestCase;

public class MarkdownUtilsTest extends TestCase {

    public void testGetTableCellGravity_left() {
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity(":-"));
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity(":--"));
    }

    public void testGetTableCellGravity_right() {
        assertEquals(Gravity.RIGHT, MarkdownUtils.getTableCellGravity("-:"));
        assertEquals(Gravity.RIGHT, MarkdownUtils.getTableCellGravity("--:"));
    }

    public void testGetTableCellGravity_center() {
        assertEquals(Gravity.CENTER, MarkdownUtils.getTableCellGravity("::"));
        assertEquals(Gravity.CENTER, MarkdownUtils.getTableCellGravity(":-:"));
        assertEquals(Gravity.CENTER, MarkdownUtils.getTableCellGravity(":--:"));
    }

    public void testGetTableCellGravity_defaultToLeft() {
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity(""));
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity("banana"));
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity(":"));
        assertEquals(Gravity.LEFT, MarkdownUtils.getTableCellGravity(null));
    }
}
