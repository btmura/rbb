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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.Accounts;

class AccountSubredditUtils {

    public static final String TAG = "AccountSubredditUtils";

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH};

    private static final int INDEX_COOKIE = 0;
    private static final int INDEX_MODHASH = 1;

    static ArrayList<Subreddit> querySubreddits(SQLiteDatabase db, long accountId) {
        String[] credentials = AccountSubredditUtils.getCredentials(db, accountId);
        try {
            URL url = Urls.subredditListUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            AccountSubredditUtils.setCommonHeaders(conn, credentials[0]);
            conn.connect();

            InputStream in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            in.close();
            conn.disconnect();

            return parser.results;

        } catch (IOException e) {
            Log.e(TAG, "querySubreddits", e);
        }

        return null;
    }

    private static String[] getCredentials(SQLiteDatabase db, long id) {
        String[] credentials = {null, null};
        String[] selectionArgs = new String[] {Long.toString(id)};
        Cursor c = db.query(Accounts.TABLE_NAME,
                CREDENTIALS_PROJECTION,
                Provider.ID_SELECTION,
                selectionArgs,
                null,
                null,
                null);
        try {
            if (c.moveToNext()) {
                credentials[INDEX_COOKIE] = c.getString(INDEX_COOKIE);
                credentials[INDEX_MODHASH] = c.getString(INDEX_MODHASH);
            }
        } finally {
            c.close();
        }
        return credentials;
    }

    private static void setCommonHeaders(HttpURLConnection conn, String cookie) {
        conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
        conn.setRequestProperty("User-Agent", Urls.USER_AGENT);
        conn.setRequestProperty("Cookie", Urls.loginCookie(cookie));
    }
}
