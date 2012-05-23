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
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.btmura.android.reddit.Provider.Accounts;
import com.btmura.android.reddit.R;

public class AccountAdapter extends SimpleCursorAdapter {

    private static final String[] PROJECTION = {
            Accounts._ID, Accounts.COLUMN_LOGIN,
    };

    private static final String[] FROM = {};
    private static final int[] TO = {};

    public static Loader<Cursor> createLoader(Context context) {
        return new CursorLoader(context, Accounts.CONTENT_URI, PROJECTION, null, null,
                Accounts.SORT);
    }

    public AccountAdapter(Context context) {
        super(context, R.layout.account_row, null, FROM, TO, 0);        
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tv = (TextView) view;
        tv.setText(cursor.getString(1));
    }
}
