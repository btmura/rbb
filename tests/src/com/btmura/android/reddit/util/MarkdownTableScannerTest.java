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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import android.view.Gravity;

import com.btmura.android.reddit.util.MarkdownTableScanner.OnTableScanListener;
import com.btmura.android.reddit.util.MarkdownTableScanner.OnTableScanListener.Cell;

public class MarkdownTableScannerTest extends TestCase {

    private static final String OUTER_PIPES = "|a|b|c|\n"
            + "|:-|::|-:|\n"
            + "|d|e|f|";

    private static final String NO_OUTER_PIPES = "a|b|c\n"
            + ":-|::|-:\n"
            + "d|e|f";

    private static final String EMPTY_HEADERS = "||\n"
            + ":-|::|-:\n"
            + "a|b|c";

    private static final String MISSING_CELLS = "a||\n"
            + ":-|::|-:\n"
            + "|b|\n"
            + "||c\n"
            + "|";

    public void testScan_outerPipes() {
        Table table = scan(OUTER_PIPES);
        assertTableSize(table, 2, 3);

        assertCell(table, 0, 0, "a", Gravity.LEFT, true);
        assertCell(table, 0, 1, "b", Gravity.CENTER, true);
        assertCell(table, 0, 2, "c", Gravity.RIGHT, true);

        assertCell(table, 1, 0, "d", Gravity.LEFT, false);
        assertCell(table, 1, 1, "e", Gravity.CENTER, false);
        assertCell(table, 1, 2, "f", Gravity.RIGHT, false);
    }

    public void testScan_noOuterPipes() {
        Table table = scan(NO_OUTER_PIPES);
        assertTableSize(table, 2, 3);

        assertCell(table, 0, 0, "a", Gravity.LEFT, true);
        assertCell(table, 0, 1, "b", Gravity.CENTER, true);
        assertCell(table, 0, 2, "c", Gravity.RIGHT, true);

        assertCell(table, 1, 0, "d", Gravity.LEFT, false);
        assertCell(table, 1, 1, "e", Gravity.CENTER, false);
        assertCell(table, 1, 2, "f", Gravity.RIGHT, false);
    }

    public void testScan_emptyHeaders() {
        Table table = scan(EMPTY_HEADERS);
        assertTableSize(table, 2, 3);

        assertCell(table, 0, 0, "", Gravity.LEFT, true);
        assertCell(table, 0, 1, "", Gravity.CENTER, true);
        assertCell(table, 0, 2, "", Gravity.RIGHT, true);

        assertCell(table, 1, 0, "a", Gravity.LEFT, false);
        assertCell(table, 1, 1, "b", Gravity.CENTER, false);
        assertCell(table, 1, 2, "c", Gravity.RIGHT, false);
    }

    public void testScan_missingCells() {
        Table table = scan(MISSING_CELLS);
        assertTableSize(table, 4, 3);

        assertCell(table, 0, 0, "a", Gravity.LEFT, true);
        assertCell(table, 0, 1, "", Gravity.CENTER, true);
        assertCell(table, 0, 2, "", Gravity.RIGHT, true);

        assertCell(table, 1, 0, "", Gravity.LEFT, false);
        assertCell(table, 1, 1, "b", Gravity.CENTER, false);
        assertCell(table, 1, 2, "", Gravity.RIGHT, false);

        assertCell(table, 2, 0, "", Gravity.LEFT, false);
        assertCell(table, 2, 1, "", Gravity.CENTER, false);
        assertCell(table, 2, 2, "c", Gravity.RIGHT, false);

        assertCell(table, 3, 0, "", Gravity.LEFT, false);
        assertCell(table, 3, 1, "", Gravity.CENTER, false);
        assertCell(table, 3, 2, "", Gravity.RIGHT, false);
    }

    private static Table scan(String tableData) {
        Table table = new Table();
        MarkdownTableScanner.scan(tableData, table);
        return table;
    }

    private static void assertTableSize(Table table, int expectedRows, int expectedColumns) {
        assertEquals(expectedRows, table.rows.size());
        for (int i = 0; i < expectedRows; i++) {
            assertEquals("row: " + i, expectedColumns, table.rows.get(i).size());
        }
    }

    private static void assertCell(Table table,
            int row,
            int column,
            String expectedContent,
            int expectedGravity,
            boolean expectedIsHeader) {
        Cell cell = table.rows.get(row).get(column);
        assertEquals(expectedContent, cell.contents);
        assertEquals(expectedGravity, cell.gravity);
        assertEquals(expectedIsHeader, cell.isHeader);
    }

    static class Table implements OnTableScanListener<List<Cell>> {

        private final List<List<Cell>> rows = new ArrayList<List<Cell>>();

        @Override
        public List<Cell> onRowStart() {
            return new ArrayList<Cell>();
        }

        @Override
        public void onCell(List<Cell> rowObject, Cell cell) {
            rowObject.add(cell.copy());
        }

        @Override
        public void onRowEnd(List<Cell> row) {
            rows.add(row);
        }
    }
}
