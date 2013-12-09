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

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;
import com.btmura.android.reddit.text.MarkdownFormatter;
import com.btmura.android.reddit.util.MarkdownTableScanner;
import com.btmura.android.reddit.util.MarkdownTableScanner.OnTableScanListener;

public class MarkdownTableFragment extends DialogFragment {

    static final String TAG = "MarkdownTableFragment";

    private static final String EXTRA_TABLE_DATA = "tableData";

    private static final String STATE_SCROLL_X = "scrollX";
    private static final String STATE_SCROLL_Y = "scrollY";

    private ScrollView tableScrollView;

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
        populateTable(view, inflater);
        setupScrollView(view, savedInstanceState);
        return view;
    }

    private void populateTable(final View view, final LayoutInflater inflater) {
        final TableLayout table = (TableLayout) view.findViewById(R.id.table);
        MarkdownTableScanner.scan(getTableDataExtra(),
                new OnTableScanListener<TableRow>() {
                    @Override
                    public TableRow onRowStart() {
                        return new TableRow(getActivity());
                    }

                    @Override
                    public void onCell(TableRow container, Cell cell) {
                        int layout = cell.isHeader
                                ? R.layout.markdown_table_cell_header
                                : R.layout.markdown_table_cell;

                        TextView tv = (TextView) inflater.inflate(layout, container, false);
                        tv.setGravity(cell.gravity);
                        tv.setMovementMethod(LinkMovementMethod.getInstance());
                        tv.setText(formatter.formatAll(getActivity(), cell.contents));
                        container.addView(tv);

                        table.setColumnShrinkable(cell.column, true);
                        table.setColumnStretchable(cell.column, true);
                    }

                    @Override
                    public void onRowEnd(TableRow row) {
                        table.addView(row);
                    }
                });
    }

    private void setupScrollView(View view, Bundle savedInstanceState) {
        tableScrollView = (ScrollView) view.findViewById(R.id.table_scroller);
        if (savedInstanceState != null) {
            final int scrollX = savedInstanceState.getInt(STATE_SCROLL_X);
            final int scrollY = savedInstanceState.getInt(STATE_SCROLL_Y);
            tableScrollView.post(new Runnable() {
                @Override
                public void run() {
                    tableScrollView.setScrollX(scrollX);
                    tableScrollView.setScrollY(scrollY);
                }
            });
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (listener != null) {
            listener.onCancel();
        }
        super.onCancel(dialog);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SCROLL_X, tableScrollView.getScrollX());
        outState.putInt(STATE_SCROLL_Y, tableScrollView.getScrollY());
    }

    private String getTableDataExtra() {
        return getArguments().getString(EXTRA_TABLE_DATA);
    }
}
