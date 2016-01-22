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
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.btmura.android.reddit.R;

public class ContentAdapter extends BaseCursorAdapter {

  private final LayoutInflater inflater;

  public ContentAdapter(Context ctx) {
    super(ctx, null, 0);
    inflater = LayoutInflater.from(ctx);
  }

  @Override
  public boolean isEnabled(int position) {
    return false;
  }

  @Override
  public View newView(Context ctx, Cursor c, ViewGroup parent) {
    return inflater.inflate(R.layout.content_row, parent, false);
  }

  @Override
  public void bindView(View view, Context ctx, Cursor c) {
    SpannableStringBuilder row = (SpannableStringBuilder) view.getTag();
    if (row == null) {
      row = new SpannableStringBuilder();
      view.setTag(row);
    }
    row.clear();
    row.clearSpans();

    int columnCount = c.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      // Append the column name and make it bold.
      row.append(String.format("%16s: ", c.getColumnName(i)));

      // Append the column value and add a newline if it's not the last
      // one.
      String value = c.getString(i);
      if (value != null) {
        row.append(value);
      }
      if (i + 1 < columnCount) {
        row.append("\n");
      }
    }
    TextView tv = (TextView) view;
    tv.setText(row);
  }
}
