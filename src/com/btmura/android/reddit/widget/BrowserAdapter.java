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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class BrowserAdapter extends SimpleCursorAdapter {

    private static final String[] FROM = {};
    private static final int[] TO = {};

    private final LayoutInflater inflater;

    public BrowserAdapter(Context context) {
        super(context, R.layout.browser_row, null, FROM, TO, 0);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tv = (TextView) view;
        tv.setText(cursor.getString(1));
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) convertView;
        if (tv == null) {
            tv = (TextView) inflater.inflate(R.layout.browser_dropdown_row, parent, false);
        }
        tv.setText(getCursor().getString(1));
        return tv;
    }

    public String getString(int position, int columnIndex) {
        Cursor c = getCursor();
        if (c.moveToPosition(position)) {
            return c.getString(columnIndex);
        }
        return null;
    }
}
