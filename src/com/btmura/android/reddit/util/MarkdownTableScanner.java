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

import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.Gravity;

import com.btmura.android.reddit.util.MarkdownTableScanner.OnTableScanListener.Cell;

import java.util.Scanner;

public class MarkdownTableScanner {

    public interface OnTableScanListener<R> {

        public static class Cell {
            public String contents;
            public int column;
            public int gravity;
            public boolean isHeader;

            public void set(String contents, int column, int gravity, boolean isHeader) {
                this.contents = contents;
                this.column = column;
                this.gravity = gravity;
                this.isHeader = isHeader;
            }

            public Cell copy() {
                Cell cell = new Cell();
                cell.set(contents, column, gravity, isHeader);
                return cell;
            }
        }

        R onRowStart();

        void onCell(R rowObject, Cell cell);

        void onRowEnd(R row);
    }

    public static <R> void scan(String tableData, OnTableScanListener<R> listener) {
        SparseIntArray columnGravity = new SparseIntArray();
        boolean hasOuterPipes = false;

        Scanner scanner = new Scanner(tableData);
        try {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
                if (scanner.hasNextLine()) {
                    String specs = scanner.nextLine();
                    hasOuterPipes = specs.startsWith("|");
                    String[] tokens = specs.split("\\|");
                    for (int i = 0; i < tokens.length; i++) {
                        columnGravity.put(i, getTableColumnGravity(tokens[i]));
                    }
                }
            }
        } finally {
            scanner.close();
        }

        scanner = new Scanner(tableData);
        Cell cell = null;
        try {
            for (int rowIndex = 0; scanner.hasNextLine(); rowIndex++) {
                String line = scanner.nextLine();

                if (rowIndex == 1) {
                    continue;
                }

                R row = listener.onRowStart();
                String[] tokens = line.split("\\|");
                int columnCount = columnGravity.size();
                for (int i = 0; i < columnCount; i++) {
                    if (hasOuterPipes && (i == 0 || i >= columnCount)) {
                        continue;
                    }

                    String contents = i < tokens.length ? tokens[i].trim() : "";
                    int columnIndex = hasOuterPipes ? i - 1 : i;
                    int gravity = columnGravity.get(i);
                    boolean isHeader = rowIndex == 0;

                    if (cell == null) {
                        cell = new Cell();
                    }
                    cell.set(contents, columnIndex, gravity, isHeader);
                    listener.onCell(row, cell);
                }
                listener.onRowEnd(row);
            }
        } finally {
            scanner.close();
        }
    }

    private static int getTableColumnGravity(String cell) {
        if (!TextUtils.isEmpty(cell) && cell.length() >= 2) {
            if (cell.startsWith(":") && cell.endsWith(":")) {
                return Gravity.CENTER;
            } else if (cell.startsWith(":")) {
                return Gravity.LEFT;
            } else if (cell.endsWith(":")) {
                return Gravity.RIGHT;
            }
        }
        return Gravity.LEFT;
    }
}
