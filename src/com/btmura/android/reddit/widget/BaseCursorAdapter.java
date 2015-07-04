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
import android.widget.CursorAdapter;

/**
 * {@link CursorAdapter} that adds some convenience methods to get values by
 * adapter position and column index.
 */
abstract class BaseCursorAdapter extends CursorAdapter {

  BaseCursorAdapter(Context context, Cursor c, int flags) {
    super(context, c, flags);
  }

  public boolean getBoolean(int position, int columnIndex) {
    Cursor c = getCursor();
    if (c != null && c.moveToPosition(position)) {
      return c.getInt(columnIndex) != 0;
    }
    return false;
  }

  public int getInt(int position, int columnIndex) {
    Cursor c = getCursor();
    if (c != null && c.moveToPosition(position)) {
      return c.getInt(columnIndex);
    }
    return -1;
  }

  public long getLong(int position, int columnIndex) {
    Cursor c = getCursor();
    if (c != null && c.moveToPosition(position)) {
      return c.getLong(columnIndex);
    }
    return -1;
  }

  public String getString(int position, int columnIndex) {
    Cursor c = getCursor();
    if (c != null && c.moveToPosition(position)) {
      return c.getString(columnIndex);
    }
    return null;
  }
}
