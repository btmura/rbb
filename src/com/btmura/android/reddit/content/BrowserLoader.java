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

package com.btmura.android.reddit.content;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.Provider.Accounts;
import com.btmura.android.reddit.content.BrowserLoader.BrowserResult;

public class BrowserLoader extends AsyncTaskLoader<BrowserResult> {

    public static final String TAG = "BrowserLoader";

    public static class BrowserResult {
        public Cursor accounts;
        public SharedPreferences prefs;

        private BrowserResult(Cursor accounts, SharedPreferences prefs) {
            this.accounts = accounts;
            this.prefs = prefs;
        }
    }

    private static final String[] PROJECTION = {
            Accounts._ID,
            Accounts.COLUMN_LOGIN,
            Accounts.COLUMN_COOKIE,
    };

    private static final String PREFS = "prefs";

    private ForceLoadContentObserver observer = new ForceLoadContentObserver();
    private BrowserResult result;

    public BrowserLoader(Context context) {
        super(context);
    }

    @Override
    public BrowserResult loadInBackground() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "loadInBackground (id " + getId() + ")");
        }
        Cursor cursor = getContext().getContentResolver().query(Accounts.CONTENT_URI, PROJECTION,
                null, null, Accounts.SORT);
        if (cursor != null) {
            cursor.getCount();
            cursor.registerContentObserver(observer);
            SharedPreferences prefs = getContext().getSharedPreferences(PREFS, 0);
            return new BrowserResult(cursor, prefs);
        }
        return null;
    }

    @Override
    public void deliverResult(BrowserResult newResult) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "deliverResult (id " + getId() + ")");
        }
        if (isReset()) {
            closeResult(newResult);
            return;
        }

        BrowserResult oldResult = this.result;
        this.result = newResult;

        if (isStarted()) {
            super.deliverResult(newResult);
        }

        if (oldResult != newResult) {
            closeResult(oldResult);
        }
    }

    @Override
    protected void onStartLoading() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onStartLoading (id " + getId() + ")");
        }
        if (result != null) {
            deliverResult(result);
        }
        if (takeContentChanged() || result == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onStopLoading (id " + getId() + ")");
        }
        cancelLoad();
    }

    @Override
    public void onCanceled(BrowserResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onCanceled (id " + getId() + ")");
        }
        closeResult(result);
    }

    @Override
    protected void onReset() {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "onReset (id " + getId() + ")");
        }
        super.onReset();
        onStopLoading();
        closeResult(result);
        result = null;
    }

    private void closeResult(BrowserResult result) {
        if (Debug.DEBUG_LOADERS) {
            Log.d(TAG, "closeResult");
        }
        if (result != null && result.accounts != null && !result.accounts.isClosed()) {
            result.accounts.close();
        }
    }
}
