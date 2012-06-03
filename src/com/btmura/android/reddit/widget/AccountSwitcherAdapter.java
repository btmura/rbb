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
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleCursorAdapter;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.BrowserLoader;

public class AccountSwitcherAdapter extends SimpleCursorAdapter {

    private static final String[] FROM = {};
    private static final int[] TO = {};

    private final LayoutInflater inflater;

    public AccountSwitcherAdapter(Context context) {
        super(context, R.layout.account_switcher_row, null, FROM, TO, 0);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int findLogin(String login) {
        Cursor cursor = getCursor();
        if (cursor != null) {
            for (cursor.moveToPosition(-1); cursor.moveToNext();) {
                String accountLogin = cursor.getString(1);
                if (login != null && login.equals(accountLogin)) {
                    return cursor.getPosition();
                }
            }
        }
        return -1;
    }
    
    public String getLogin(int position) {
        return getString(position, 1);
    }

    public String getCookie(int position) {
        return getString(position, 2);
    }
    
    public String getModhash(int position) {
        return getString(position, 3);
    }

    private String getString(int position, int columnIndex) {
        Cursor c = getCursor();
        if (c.moveToPosition(position)) {
            return c.getString(columnIndex);
        }
        return null;
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
            tv = (TextView) inflater.inflate(R.layout.account_switcher_dropdown_row, parent, false);
        }
        tv.setText(getLogin(position));
        return tv;
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        if (cursor != null) {
            // Wrap the cursor to make sure onItemSelected is always fired.
            cursor = new NoAccountCursorWrapper(cursor);
        }
        return super.swapCursor(cursor);
    }

    public boolean hasNoAccounts() {
        NoAccountCursorWrapper wrapper = (NoAccountCursorWrapper) getCursor();
        return wrapper == null || wrapper.hasNoAccounts();
    }

    /**
     * {@link CursorWrapper} for making sure there is always one item in the
     * cursor so that
     * {@link OnItemSelectedListener#onItemSelected(AdapterView, View, int, long)}
     * is always fired.
     */
    static class NoAccountCursorWrapper extends CursorWrapper {

        private static final MatrixCursor NO_ACCOUNT_CURSOR = new MatrixCursor(
                BrowserLoader.PROJECTION);

        static {
            NO_ACCOUNT_CURSOR.addRow(new Object[] {
                    AdapterView.INVALID_ROW_ID, "", null, null,
            });
        }

        public NoAccountCursorWrapper(Cursor cursor) {
            super(cursor);
        }

        @Override
        public boolean move(int offset) {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.move(offset);
            } else {
                return super.move(offset);
            }
        }

        @Override
        public boolean moveToPosition(int position) {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.moveToPosition(position);
            } else {
                return super.moveToPosition(position);
            }
        }

        @Override
        public boolean moveToFirst() {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.moveToFirst();
            } else {
                return super.moveToFirst();
            }
        }

        @Override
        public boolean moveToPrevious() {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.moveToPrevious();
            } else {
                return super.moveToPrevious();
            }
        }

        @Override
        public boolean moveToNext() {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.moveToNext();
            } else {
                return super.moveToNext();
            }
        }

        @Override
        public boolean moveToLast() {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.moveToLast();
            } else {
                return super.moveToLast();
            }
        }

        @Override
        public int getCount() {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.getCount();
            } else {
                return super.getCount();
            }
        }

        @Override
        public long getLong(int columnIndex) {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.getLong(columnIndex);
            } else {
                return super.getLong(columnIndex);
            }
        }

        @Override
        public String getString(int columnIndex) {
            if (hasNoAccounts()) {
                return NO_ACCOUNT_CURSOR.getString(columnIndex);
            } else {
                return super.getString(columnIndex);
            }
        }

        boolean hasNoAccounts() {
            return super.getCount() == 0;
        }
    }
}
