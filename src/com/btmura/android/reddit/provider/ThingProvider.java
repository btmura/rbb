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

import java.io.IOException;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.database.Votes;
import com.btmura.android.reddit.util.Array;

public class ThingProvider extends SessionProvider {

    public static final String TAG = "ThingProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.things";
    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    static final String PATH_THINGS = "things";
    public static final Uri THINGS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_THINGS);

    public static final String PARAM_FETCH = "fetch";
    public static final String PARAM_ACCOUNT = "account";
    public static final String PARAM_SESSION_ID = "sessionId";
    public static final String PARAM_SUBREDDIT = "subreddit";
    public static final String PARAM_FILTER = "filter";
    public static final String PARAM_MORE = "more";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_USER = "user";

    private static final String TABLE_NAME_WITH_VOTES = Things.TABLE_NAME
            + " LEFT OUTER JOIN (SELECT "
            + Votes.COLUMN_ACCOUNT + ", "
            + Votes.COLUMN_THING_ID + ", "
            + Votes.COLUMN_VOTE
            + " FROM " + Votes.TABLE_NAME + ") USING ("
            + Votes.COLUMN_ACCOUNT + ", "
            + Things.COLUMN_THING_ID + ")";

    public ThingProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri, boolean isQuery) {
        return isQuery ? TABLE_NAME_WITH_VOTES : Things.TABLE_NAME;
    }

    @Override
    protected void processUri(Uri uri, SQLiteDatabase db, ContentValues values) {
        if (uri.getBooleanQueryParameter(PARAM_FETCH, false)) {
            handleFetch(uri, db);
        }
    }

    private void handleFetch(Uri uri, SQLiteDatabase db) {
        try {
            long sessionTimestamp = getSessionTimestamp();

            String accountName = uri.getQueryParameter(PARAM_ACCOUNT);
            String sessionId = uri.getQueryParameter(PARAM_SESSION_ID);
            String subredditName = uri.getQueryParameter(PARAM_SUBREDDIT);
            String filterParameter = uri.getQueryParameter(PARAM_FILTER);
            int filter = filterParameter != null ? Integer.parseInt(filterParameter) : 0;
            String query = uri.getQueryParameter(PARAM_QUERY);
            String user = uri.getQueryParameter(PARAM_USER);
            String more = uri.getQueryParameter(PARAM_MORE);

            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            ThingListing listing = ThingListing.get(context, accountName, sessionId,
                    sessionTimestamp, subredditName, filter, query, user, more, cookie);

            long cleaned;
            long t1 = System.currentTimeMillis();
            db.beginTransaction();
            try {
                // Delete old things that can't possibly be viewed anymore.
                cleaned = db.delete(Things.TABLE_NAME, Things.SELECT_BEFORE_TIMESTAMP,
                        Array.of(sessionTimestamp));

                // Delete the loading more element before appending more.
                db.delete(Things.TABLE_NAME, Things.SELECT_BY_SESSION_ID_AND_MORE,
                        Array.of(sessionId));

                InsertHelper insertHelper = new InsertHelper(db, Things.TABLE_NAME);
                int count = listing.values.size();
                for (int i = 0; i < count; i++) {
                    insertHelper.insert(listing.values.get(i));
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            if (BuildConfig.DEBUG) {
                long t2 = System.currentTimeMillis();
                Log.d(TAG, "sync network: " + listing.networkTimeMs
                        + " parse: " + listing.parseTimeMs
                        + " db: " + (t2 - t1)
                        + " cleaned: " + cleaned);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

}
