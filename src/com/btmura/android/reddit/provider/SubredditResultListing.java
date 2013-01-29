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
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.util.JsonReader;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.database.SubredditResults;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.JsonParser;

class SubredditResultListing extends JsonParser implements Listing {

    public static final String TAG = "SubredditResultListing";

    private final String accountName;
    private final String query;
    private final String cookie;

    private final ArrayList<ContentValues> values = new ArrayList<ContentValues>(25);
    private long networkTimeMs;
    private long parseTimeMs;

    static SubredditResultListing newInstance(String accountName, String query, String cookie) {
        return new SubredditResultListing(accountName, query, cookie);
    }

    SubredditResultListing(String accountName, String query, String cookie) {
        this.accountName = accountName;
        this.query = query;
        this.cookie = cookie;
    }

    public ArrayList<ContentValues> getValues() throws IOException {
        long t1 = System.currentTimeMillis();
        CharSequence url = Urls.subredditSearch(query, null);
        HttpURLConnection conn = RedditApi.connect(url, cookie, true, false);
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

    public void performExtraWork(Context context) {
    }

    public void addCursorExtras(Bundle bundle) {
    }

    public long getNetworkTimeMs() {
        return networkTimeMs;
    }

    public long getParseTimeMs() {
        return parseTimeMs;
    }

    public String getTargetTable() {
        return SubredditResults.TABLE_NAME;
    }

    public boolean isAppend() {
        return false;
    }

    @Override
    public void onEntityStart(int index) {
        ContentValues v = new ContentValues(5);
        v.put(Things.COLUMN_ACCOUNT, accountName);
        values.add(v);
    }

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        values.get(index).put(SubredditResults.COLUMN_NAME, readTrimmedString(reader, ""));
    }

    @Override
    public void onOver18(JsonReader reader, int index) throws IOException {
        values.get(index).put(SubredditResults.COLUMN_OVER_18, reader.nextBoolean());
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        values.get(index).put(SubredditResults.COLUMN_SUBSCRIBERS, reader.nextInt());
    }
}
