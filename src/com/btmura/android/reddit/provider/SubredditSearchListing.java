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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.ContentValues;
import android.util.JsonReader;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.JsonParser;

class SubredditSearchListing extends JsonParser implements Listing {

    public static final String TAG = "SubredditSearchListing";

    private final String accountName;
    private final String sessionId;
    private final long sessionTimestamp;
    private final String query;
    private final String cookie;

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(25);
    private long networkTimeMs;
    private long parseTimeMs;

    SubredditSearchListing(String accountName, String sessionId, long sessionTimestamp,
            String query, String cookie) {
        this.accountName = accountName;
        this.sessionId = sessionId;
        this.sessionTimestamp = sessionTimestamp;
        this.query = query;
        this.cookie = cookie;
    }

    public ArrayList<ContentValues> getValues() throws IOException {
        long t1 = System.currentTimeMillis();

        URL url = Urls.subredditSearchUrl(query, null);
        HttpURLConnection conn = RedditApi.connect(url, cookie, false);
        InputStream input = new BufferedInputStream(conn.getInputStream());
        long t2 = System.currentTimeMillis();
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingObject(reader);
            if (BuildConfig.DEBUG) {
                long t3 = System.currentTimeMillis();
                networkTimeMs = t2 - t1;
                parseTimeMs = t3 - t2;
            }
            return values;
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    @Override
    public void onEntityStart(int index) {
        ContentValues v = new ContentValues(6);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        v.put(Things.COLUMN_SESSION_ID, sessionId);
        v.put(Things.COLUMN_SESSION_TIMESTAMP, sessionTimestamp);
        values.add(v);
    }

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_NAME, readTrimmedString(reader, ""));
    }

    @Override
    public void onOver18(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_OVER_18, reader.nextBoolean());
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        values.get(index).put(Things.COLUMN_SUBSCRIBERS, reader.nextInt());
    }
}
