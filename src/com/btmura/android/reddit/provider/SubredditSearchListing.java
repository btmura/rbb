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
import android.content.Context;
import android.util.JsonReader;

import com.btmura.android.reddit.database.SubredditSearches;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.util.JsonParser;

class SubredditSearchListing extends JsonParser {

    final ArrayList<ContentValues> values = new ArrayList<ContentValues>(30);

    public void get(Context context, String query, String cookie) throws IOException {
        URL url = Urls.subredditSearchUrl(query, null);
        HttpURLConnection conn = NetApi.connect(context, url, cookie);
        InputStream input = new BufferedInputStream(conn.getInputStream());
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(input));
            parseListingArray(reader);
        } finally {
            input.close();
            conn.disconnect();
        }
    }

    @Override
    public void onEntityStart(int index) {
        values.add(new ContentValues());
    }

    @Override
    public void onDisplayName(JsonReader reader, int index) throws IOException {
        values.get(index).put(SubredditSearches.COLUMN_NAME, readTrimmedString(reader, ""));
    }

    @Override
    public void onSubscribers(JsonReader reader, int index) throws IOException {
        values.get(index).put(SubredditSearches.COLUMN_SUBSCRIBERS, reader.nextInt());
    }

    @Override
    public void onParseEnd() {
    }
}
