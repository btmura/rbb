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

package com.btmura.android.reddit.provider;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

abstract class BaseProvider extends ContentProvider {

    static final String ID_SELECTION = BaseColumns._ID + "= ?";

    protected DbHelper helper;

    @Override
    public boolean onCreate() {
        helper = new DbHelper(getContext(), DbHelper.DATABASE_REDDIT, DbHelper.LATEST_VERSION);
        return true;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    static String appendIdSelection(String selection) {
        if (selection == null) {
            return ID_SELECTION;
        } else {
            return selection + " AND " + ID_SELECTION;
        }
    }

    static String[] idSelectionArg(long id) {
        return appendIdSelectionArg(null, id);
    }

    static String[] appendIdSelectionArg(String[] selectionArgs, long id) {
        return appendIdSelectionArg(selectionArgs, Long.toString(id));
    }

    static String[] appendIdSelectionArg(String[] selectionArgs, String id) {
        if (selectionArgs == null) {
            return new String[] {id};
        } else {
            String[] newSelectionArgs = new String[selectionArgs.length + 1];
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 0, selectionArgs.length);
            newSelectionArgs[newSelectionArgs.length - 1] = id;
            return newSelectionArgs;
        }
    }
}
