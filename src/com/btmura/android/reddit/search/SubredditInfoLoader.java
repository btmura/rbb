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

package com.btmura.android.reddit.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;

class SubredditInfoLoader extends AsyncTaskLoader<List<SubredditInfo>> {

    private static final String TAG = "SubredditLoader";

    private List<SubredditInfo> results;

    private String query;

    public SubredditInfoLoader(Context context, String query) {
        super(context);
        this.query = query;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (results != null) {
            deliverResult(results);
        } else {
            forceLoad();
        }
    }

    @Override
    public List<SubredditInfo> loadInBackground() {
        try {
            URL subredditUrl = new URL("http://www.reddit.com/reddits/search.json?q="
                    + URLEncoder.encode(query, "UTF-8"));

            HttpURLConnection connection = (HttpURLConnection) subredditUrl.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            SearchParser parser = new SearchParser();
            parser.parseListingObject(reader);
            stream.close();

            connection.disconnect();

            return parser.results;

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
    }

    class SearchParser extends JsonParser {

        private List<SubredditInfo> results = new ArrayList<SubredditInfo>();

        @Override
        public void onEntityStart(int index) {
            results.add(new SubredditInfo());
        }

        @Override
        public void onDisplayName(JsonReader reader, int index) throws IOException {
            results.get(index).displayName = reader.nextString();
        }

        @Override
        public void onTitle(JsonReader reader, int index) throws IOException {
            results.get(index).title = Formatter.formatTitle(getContext(),
                    readTrimmedString(reader, ""));
        }

        @Override
        public void onDescription(JsonReader reader, int index) throws IOException {
            results.get(index).description = readTrimmedString(reader, "");
        }

        @Override
        public void onSubscribers(JsonReader reader, int index) throws IOException {
            results.get(index).subscribers = reader.nextInt();
        }

        @Override
        public void onEntityEnd(int index) {
            SubredditInfo srInfo = results.get(index);
            srInfo.status = getContext().getString(R.string.sr_info_status, srInfo.displayName,
                    srInfo.subscribers);
        }
    }
}
