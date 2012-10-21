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
import android.provider.BaseColumns;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.DbHelper;

abstract class BaseProvider extends ContentProvider {

    public static final String ID_SELECTION = BaseColumns._ID + "= ?";

    /**
     * Sync changes back to the network. Don't set this in sync adapters or else
     * we'll get stuck in a syncing loop.
     */
    public static final String PARAM_SYNC = "sync";

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
        SQLiteDatabase db = helper.getWritableDatabase();
        String table = getTable(uri, true);
        Cursor c = null;

        db.beginTransaction();
        try {
            processUri(uri, db, null);
            c = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (BuildConfig.DEBUG) {
            Log.d(logTag, "query table: " + table);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table = getTable(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        long id = -1;

        db.beginTransaction();
        try {
            processUri(uri, db, values);
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
        String table = getTable(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            processUri(uri, db, values);
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
        String table = getTable(uri, false);
        SQLiteDatabase db = helper.getWritableDatabase();
        int count = 0;

        db.beginTransaction();
        try {
            processUri(uri, db, null);
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

    protected abstract String getTable(Uri uri, boolean isQuery);

    protected abstract void processUri(Uri uri, SQLiteDatabase db, ContentValues values);

    protected void notifyChange(Uri uri) {
        boolean sync = uri.getBooleanQueryParameter(PARAM_SYNC, false);
        getContext().getContentResolver().notifyChange(uri, null, sync);
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
}
