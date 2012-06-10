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
import java.io.OutputStream;
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
            AccountSubredditUtils.setCommonHeaders(conn, credentials);
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

    static void subscribe(SQLiteDatabase db, long accountId, String subreddit, boolean subscribe)
            throws IOException {
        String[] credentials = getCredentials(db, accountId);
        URL url = Urls.subscribeUrl();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setCommonHeaders(conn, credentials);
        setFormDataHeaders(conn);
        conn.connect();

        String data = Urls.subscribeQuery(credentials[INDEX_MODHASH], subreddit, subscribe);
        writeFormData(conn, data);

        InputStream in = conn.getInputStream();
        in.close();
        conn.disconnect();
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

    private static void setCommonHeaders(HttpURLConnection conn, String[] credentials) {
        conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
        conn.setRequestProperty("User-Agent", Urls.USER_AGENT);
        conn.setRequestProperty("Cookie", Urls.loginCookie(credentials[INDEX_COOKIE]));
    }

    private static void setFormDataHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", Urls.CONTENT_TYPE);
        conn.setDoOutput(true);
    }

    private static void writeFormData(HttpURLConnection conn, String data) throws IOException {
        OutputStream output = null;
        try {
            output = conn.getOutputStream();
            output.write(data.getBytes(Urls.CHARSET));
            output.close();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
