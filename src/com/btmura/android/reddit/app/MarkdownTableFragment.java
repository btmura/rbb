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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;

public class MarkdownTableFragment extends DialogFragment {

    static final String TAG = "MarkdownTableFragment";

    private static final String EXTRA_TABLE_DATA = "tableData";

    public interface OnMarkdownTableFragmentEventListener {
        void onCancel();
    }

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
        setStyle(DialogFragment.STYLE_NO_TITLE, ThemePrefs.getDialogTheme(getActivity()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.markdown_table, container, false);
        TableLayout tableLayout = (TableLayout) view.findViewById(R.id.table);

        String tableData = getTableDataExtra();
        Scanner scanner = new Scanner(tableData);
        for (int row = 0; scanner.hasNextLine(); row++) {
            String[] cells = scanner.nextLine().split("\\|");
            int cellCount = cells.length;
            TableRow tableRow = new TableRow(getActivity());
            for (int j = 0; j < cellCount; j++) {
                int layout = row == 0
                        ? R.layout.markdown_table_cell_header
                        : R.layout.markdown_table_cell;
                TextView tv = (TextView) inflater.inflate(layout, container, false);
                tv.setText(cells[j]);
                tableRow.addView(tv);
            }
            tableLayout.addView(tableRow);
        }
        scanner.close();

        return view;
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
