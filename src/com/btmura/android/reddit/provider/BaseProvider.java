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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.CursorExtrasWrapper;
import com.btmura.android.reddit.database.DbHelper;
import com.btmura.android.reddit.util.Array;

abstract class BaseProvider extends ContentProvider {

    public static final String ID_SELECTION = BaseColumns._ID + "= ?";

    public static final String TRUE = Boolean.toString(true);

    public static final String FALSE = Boolean.toString(false);

    /**
     * Sync changes back to the network. Don't set this in sync adapters or else
     * we'll get stuck in a syncing loop. This is by default false.
     */
    public static final String PARAM_SYNC = "sync";

    /**
     * Parameter to control whether to notify registered listeners of changes.
     * This is by default true.
     */
    public static final String PARAM_NOTIFY = "notify";

    /** Selection is a bundle of a selection and its arguments. */
    static class Selection {
        String selection;
        String[] selectionArgs;
        Bundle extras;
    }

    protected String logTag;
    protected DbHelper helper;

    BaseProvider(String logTag) {
        this.logTag = logTag;
    }

    @Override
    public boolean onCreate() {
        helper = DbHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "query uri: " + uri);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        String table = getTable(uri);
        Cursor c = null;
        db.beginTransaction();
        try {
            Selection newSelection = processUri(uri, db, null, selection, selectionArgs);
            if (newSelection != null) {
                selection = newSelection.selection;
                selectionArgs = newSelection.selectionArgs;
            }

            c = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            if (newSelection != null && newSelection.extras != null) {
                c = new CursorExtrasWrapper(c, newSelection.extras);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = getTable(uri);
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = -1;

        db.beginTransaction();
        try {
            processUri(uri, db, values, null, null);
            id = db.insert(table, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (BuildConfig.DEBUG) {
            Log.d(logTag, "insert table: " + table + " id: " + id);
        }
        if (id != -1) {
            notifyChange(uri);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String table = getTable(uri);
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            Selection newSelection = processUri(uri, db, values, selection, selectionArgs);
            if (newSelection != null) {
                selection = newSelection.selection;
                selectionArgs = newSelection.selectionArgs;
            }

            count = db.update(table, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (BuildConfig.DEBUG) {
            Log.d(logTag, "update table: " + table + " count: " + count);
        }
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String table = getTable(uri);
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            Selection newSelection = processUri(uri, db, null, selection, selectionArgs);
            if (newSelection != null) {
                selection = newSelection.selection;
                selectionArgs = newSelection.selectionArgs;
            }

            count = db.delete(table, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (BuildConfig.DEBUG) {
            Log.d(logTag, "delete table: " + table + " count: " + count);
        }
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    protected abstract String getTable(Uri uri);

    /**
     * Processes the database inputs and returns a new selection to use for
     * querying or null to indicate that no changes to the selection need to be
     * made to the selection.
     */
    protected Selection processUri(Uri uri, SQLiteDatabase db, ContentValues values,
            String selection, String[] selectionArgs) {
        // Do nothing by default. Override to do more processing of URIs.
        return null;
    }

    protected void notifyChange(Uri uri) {
        if (uri.getBooleanQueryParameter(PARAM_NOTIFY, true)) {
            boolean sync = uri.getBooleanQueryParameter(PARAM_SYNC, false);
            getContext().getContentResolver().notifyChange(uri, null, sync);
        }
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

    static int getIntParameter(Uri uri, String key, int defValue) {
        String value = uri.getQueryParameter(key);
        return !TextUtils.isEmpty(value) ? Integer.parseInt(value) : defValue;
    }

    static long getLongParameter(Uri uri, String key, long defValue) {
        String value = uri.getQueryParameter(key);
        return !TextUtils.isEmpty(value) ? Long.parseLong(value) : defValue;
    }

    static String appendSelection(String selection, String clause) {
        if (TextUtils.isEmpty(selection)) {
            return clause;
        }
        return selection + " AND " + clause;
    }

    static String[] appendSelectionArg(String[] selectionArgs, String arg) {
        return Array.append(selectionArgs, arg);
    }

    static String toString(boolean value) {
        return Boolean.toString(value);
    }

    static String toString(long value) {
        return Long.toString(value);
    }

    static String toString(int value) {
        return Integer.toString(value);
    }
}
