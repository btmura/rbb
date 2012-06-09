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
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.Provider.AccountSubreddits;
import com.btmura.android.reddit.provider.Provider.Accounts;
import com.btmura.android.reddit.provider.Provider.Subreddits;

/**
 * {@link NetProvider} queries reddit.com for account info when {@link Provider}
 * asks for it.
 */
class NetProvider {

    public static String TAG = "NetProvider";

    private static final String[] CREDENTIALS_PROJECTION = new String[] {
            Accounts.COLUMN_COOKIE,
            Accounts.COLUMN_MODHASH};

    private static final int INDEX_COOKIE = 0;
    private static final int INDEX_MODHASH = 1;
    
    static Cursor querySubreddits(SQLiteDatabase db, long accountId) {
        String[] credentials = getCredentials(db, accountId);
        try {
            URL url = Urls.subredditListUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn);
            conn.setRequestProperty("Cookie", Urls.loginCookie(credentials[INDEX_COOKIE]));            
            conn.connect();

            InputStream in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            in.close();
            conn.disconnect();

            return new SubredditCursor(parser.results);

        } catch (IOException e) {
            Log.e(TAG, "querySubreddits", e);
        }
        return null;
    }

    static long insertSubreddit(SQLiteDatabase db, long accountId, ContentValues values) {
        String[] credentials = getCredentials(db, accountId);
        try {
            URL url = Urls.subscribeUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn);            
            conn.setRequestProperty("Content-Type", Urls.CONTENT_TYPE);
            conn.setRequestProperty("Cookie", Urls.loginCookie(credentials[INDEX_COOKIE]));
            conn.setDoOutput(true);
            conn.connect();

            String subreddit = values.getAsString(AccountSubreddits.COLUMN_NAME);
            String data = Urls.subscribeQuery(credentials[INDEX_MODHASH], subreddit, true);
            writeData(conn, data);
            
            InputStream in = conn.getInputStream();
            Scanner sc = new Scanner(in);
            while (sc.hasNextLine()) {
                Log.v(TAG, sc.nextLine());
            }
            in.close();
            conn.disconnect();

            return 1337;

        } catch (IOException e) {
            Log.e(TAG, "insertSubreddit", e);
        }
        return -1;
    }
    
    static int deleteSubreddit(SQLiteDatabase db, long accountId, String[] selectionArgs) {
        String[] credentials = getCredentials(db, accountId);
        try {
            URL url = Urls.subscribeUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn);
            conn.setRequestProperty("Content-Type", Urls.CONTENT_TYPE);
            conn.setRequestProperty("Cookie", Urls.loginCookie(credentials[INDEX_COOKIE]));
            conn.setDoOutput(true);
            conn.connect();

            String subreddit = selectionArgs[0];
            String data = Urls.subscribeQuery(credentials[INDEX_MODHASH], subreddit, false);
            writeData(conn, data);
            
            InputStream in = conn.getInputStream();
            Scanner sc = new Scanner(in);
            while (sc.hasNextLine()) {
                Log.v(TAG, sc.nextLine());
            }
            in.close();
            conn.disconnect();

            return 1;

        } catch (IOException e) {
            Log.e(TAG, "deleteSubreddit", e);
        }
        return -1;
    }

    private static void setCommonHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
        conn.setRequestProperty("User-Agent", Urls.USER_AGENT);
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

    private static void writeData(HttpURLConnection conn, String data) throws IOException {
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

    static class SubredditParser extends JsonParser {

        List<Subreddit> results = new ArrayList<Subreddit>();

        @Override
        public void onEntityStart(int index) {
            results.add(Subreddit.emptyInstance());
        }

        @Override
        public void onDisplayName(JsonReader reader, int index) throws IOException {
            results.get(index).name = reader.nextString();
        }

        @Override
        public void onSubscribers(JsonReader reader, int index) throws IOException {
            results.get(index).subscribers = reader.nextInt();
        }

        @Override
        public void onParseEnd() {
            Collections.sort(results);
        }
    }

    static class SubredditCursor extends AbstractCursor {

        private static final String FAKE_COLUMN_SUBSCRIBERS = "subscribers";

        private static final String[] PROJECTION = {
                Subreddits._ID,
                Subreddits.COLUMN_NAME,
                FAKE_COLUMN_SUBSCRIBERS,};

        private final List<Subreddit> results;

        SubredditCursor(List<Subreddit> results) {
            this.results = results;
        }

        @Override
        public String[] getColumnNames() {
            return PROJECTION;
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public String getString(int column) {
            return results.get(getPosition()).name;
        }

        @Override
        public double getDouble(int column) {
            return 0;
        }

        @Override
        public float getFloat(int column) {
            return 0;
        }

        @Override
        public int getInt(int column) {
            return results.get(getPosition()).subscribers;
        }

        @Override
        public long getLong(int column) {
            return getPosition();
        }

        @Override
        public short getShort(int column) {
            return 0;
        }

        @Override
        public boolean isNull(int column) {
            return false;
        }
    }
}
