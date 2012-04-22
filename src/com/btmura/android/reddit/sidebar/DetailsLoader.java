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

package com.btmura.android.reddit.sidebar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.Formatter;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Details;

class DetailsLoader extends AsyncTaskLoader<Details> {

    private static final String TAG = "DetailsLoader";

    private Details results;

    private String subreddit;

    public DetailsLoader(Context context, String subreddit) {
        super(context);
        this.subreddit = subreddit;
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
    public Details loadInBackground() {
        try {
            URL subredditUrl = new URL(Urls.sidebarUrl(subreddit).toString());

            HttpURLConnection connection = (HttpURLConnection) subredditUrl.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            DetailsParser parser = new DetailsParser();
            parser.parseEntity(reader);
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

    class DetailsParser extends JsonParser {

        private Details results;

        @Override
        public void onEntityStart(int index) {
            results = new Details();
        }

        @Override
        public void onDisplayName(JsonReader reader, int index) throws IOException {
            results.displayName = reader.nextString();
        }

        @Override
        public void onTitle(JsonReader reader, int index) throws IOException {
            results.title = Formatter.formatTitle(getContext(), readTrimmedString(reader, ""));
        }

        @Override
        public void onDescription(JsonReader reader, int index) throws IOException {
            results.description = readTrimmedString(reader, "");
        }
    }
}
