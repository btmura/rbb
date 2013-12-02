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
