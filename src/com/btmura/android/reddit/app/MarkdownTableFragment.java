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

package com.btmura.android.reddit.app;

import java.util.Scanner;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.MarkdownUtils;

public class MarkdownTableFragment extends DialogFragment {

    static final String TAG = "MarkdownTableFragment";

    private static final String EXTRA_TABLE_DATA = "tableData";

    public interface OnMarkdownTableFragmentEventListener {
        void onCancel();
    }

    private final MarkdownFormatter formatter = new MarkdownFormatter();
    private OnMarkdownTableFragmentEventListener listener;

    public static MarkdownTableFragment newInstance(String tableData) {
        Bundle args = new Bundle(1);
        args.putString(EXTRA_TABLE_DATA, tableData);
        MarkdownTableFragment frag = new MarkdownTableFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnMarkdownTableFragmentEventListener) {
            listener = (OnMarkdownTableFragmentEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, ThemePrefs.getDialogWhenLargeTheme(getActivity()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.markdown_table, container, false);
        TableLayout tableLayout = (TableLayout) view.findViewById(R.id.table);
        int[] gravitySpecs = getGravitySpecs();
        populateTableLayout(tableLayout, gravitySpecs, inflater);
        return view;
    }

    private void populateTableLayout(TableLayout tableLayout,
            int[] gravitySpecs,
            LayoutInflater inflater) {
        Scanner scanner = new Scanner(getTableDataExtra());
        try {
            for (int row = 0; scanner.hasNextLine(); row++) {
                // Adds empty columns if someone adds optional surrounding pipes, but this is OK,
                // since we do not display column borders at all.
                String[] cells = scanner.nextLine().split("\\|");
                int cellCount = cells.length;
                if (row != 1) {
                    TableRow tableRow = new TableRow(getActivity());
                    for (int j = 0; j < cellCount; j++) {
                        int layout = row == 0
                                ? R.layout.markdown_table_cell_header
                                : R.layout.markdown_table_cell;
                        int gravity = gravitySpecs != null && j < gravitySpecs.length
                                ? gravitySpecs[j]
                                : Gravity.LEFT;

                        TextView tv = (TextView) inflater.inflate(layout, tableRow, false);
                        tv.setGravity(gravity);
                        tv.setMovementMethod(LinkMovementMethod.getInstance());
                        tv.setText(formatter.formatAll(getActivity(), cells[j].trim()));
                        tableRow.addView(tv);

                        tableLayout.setColumnShrinkable(j, true);
                        tableLayout.setColumnStretchable(j, true);
                    }
                    tableLayout.addView(tableRow);
                }
            }
        } finally {
            scanner.close();
        }
    }

    private int[] getGravitySpecs() {
        Scanner scanner = new Scanner(getTableDataExtra());
        try {
            if (scanner.hasNextLine()) {
                scanner.nextLine();
                if (scanner.hasNextLine()) {
                    String[] cells = scanner.nextLine().split("\\|");
                    int cellCount = cells.length;
                    int[] specs = new int[cellCount];
                    for (int i = 0; i < cellCount; i++) {
                        specs[i] = MarkdownUtils.getTableCellGravity(cells[i]);
                    }
                    return specs;
                }
            }
            return null;
        } finally {
            scanner.close();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (listener != null) {
            listener.onCancel();
        }
        super.onCancel(dialog);
    }

    private String getTableDataExtra() {
        return getArguments().getString(EXTRA_TABLE_DATA);
    }
}