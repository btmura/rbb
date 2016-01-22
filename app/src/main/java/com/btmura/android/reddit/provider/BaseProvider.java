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
import com.btmura.android.reddit.util.Array;

import java.util.ArrayList;

abstract class BaseProvider extends ContentProvider {

  public static final String ID_SELECTION = BaseColumns._ID + "= ?";

  public static final String TRUE = Boolean.toString(true);

  static final boolean SYNC = true;
  static final boolean NO_SYNC = false;

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

  /** Returns a URI that tickles the sync adapter to activate after. */
  protected static Uri makeSyncUri(Uri uri) {
    return uri.buildUpon().appendQueryParameter(PARAM_SYNC, TRUE).build();
  }

  protected String tag;
  protected DbHelper helper;

  BaseProvider(String tag) {
    this.tag = tag;
  }

  @Override
  public boolean onCreate() {
    helper = DbHelper.getInstance(getContext());
    return true;
  }

  @Override
  public Cursor query(
      Uri uri, String[] projection, String selection, String[] args,
      String sortOrder) {
    String table = getTable(uri);
    SQLiteDatabase db = helper.getWritableDatabase();

    Cursor c = null;
    db.beginTransaction();
    try {
      c = db.query(table, projection, selection, args, null, null, sortOrder);
      if (c != null) {
        c.setNotificationUri(getContext().getContentResolver(), uri);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    if (BuildConfig.DEBUG) {
      Log.d(tag,
          "q t: " + table + " s: " + selection + " a: " + Array.asList(args));
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
      id = db.replace(table, null, values);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    if (BuildConfig.DEBUG) {
      Log.d(tag, "i t: " + table + " i: " + id);
    }
    if (id != -1) {
      notifyChange(uri);
      return ContentUris.withAppendedId(uri, id);
    }
    return null;
  }

  @Override
  public int update(
      Uri uri,
      ContentValues values,
      String selection,
      String[] args) {
    String table = getTable(uri);
    SQLiteDatabase db = helper.getWritableDatabase();
    int count = 0;

    db.beginTransaction();
    try {
      count = db.update(table, values, selection, args);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    if (BuildConfig.DEBUG) {
      Log.d(tag, "u t: " + table + " c: " + count
          + " s: " + selection + " a: " + Array.asList(args));
    }
    if (count > 0) {
      notifyChange(uri);
    }
    return count;
  }

  @Override
  public int delete(Uri uri, String selection, String[] args) {
    String table = getTable(uri);
    SQLiteDatabase db = helper.getWritableDatabase();
    int count = 0;

    db.beginTransaction();
    try {
      count = db.delete(table, selection, args);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }

    if (BuildConfig.DEBUG) {
      Log.d(tag, "d t: " + table + " c: " + count
          + " s: " + selection + " a: " + Array.asList(args));
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
}
