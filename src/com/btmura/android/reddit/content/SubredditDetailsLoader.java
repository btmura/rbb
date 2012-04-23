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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.SubredditDetails;

public class SubredditDetailsLoader extends AsyncTaskLoader<List<SubredditDetails>> {

    private static final String TAG = "SubredditDetailsLoader";

    private List<SubredditDetails> results;

    private String query;

    public SubredditDetailsLoader(Context context, String query) {
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
    public List<SubredditDetails> loadInBackground() {
        try {
            URL subredditUrl = new URL(Urls.subredditSearchUrl(query).toString());

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

    class SearchParser extends JsonParser {

        private List<SubredditDetails> results = new ArrayList<SubredditDetails>();

        @Override
        public void onEntityStart(int index) {
            results.add(new SubredditDetails());
        }

        @Override
        public void onDisplayName(JsonReader reader, int index) throws IOException {
            results.get(index).displayName = reader.nextString();
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
}
