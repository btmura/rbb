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
import android.support.v4.content.CursorLoader;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Accounts;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.util.Objects;

/**
 * Adapter that does not create views and is only used to check mail.
 */
public class AccountMailAdapter extends BaseCursorAdapter {

    private static final String[] PROJECTION = {
            Accounts._ID,
            Accounts.COLUMN_ACCOUNT,
            Accounts.COLUMN_HAS_MAIL,
    };

    private static final int INDEX_ACCOUNT = 1;
    private static final int INDEX_HAS_MAIL = 2;

    public static CursorLoader getLoader(Context context) {
        return new CursorLoader(context.getApplicationContext(), AccountProvider.ACCOUNTS_URI,
                PROJECTION, null, null, null);
    }

    public AccountMailAdapter(Context context) {
        super(context, null, 0);
    }

    public boolean hasMessages(String accountName) {
        if (AccountUtils.isAccount(accountName)) {
            Cursor c = getCursor();
            if (c != null) {
                for (c.moveToPosition(-1); c.moveToNext();) {
                    if (Objects.equals(accountName, c.getString(INDEX_ACCOUNT))) {
                        return c.getInt(INDEX_HAS_MAIL) == 1;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Does nothing since this isn't used for the UI yet.
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Does nothing since this isn't used for the UI yet.
        return null;
    }
}
