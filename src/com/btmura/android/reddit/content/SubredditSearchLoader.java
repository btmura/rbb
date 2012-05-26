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
import android.database.AbstractCursor;
import android.database.Cursor;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.data.JsonParser;
import com.btmura.android.reddit.entity.Subreddit;

public class SubredditSearchLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = "SubredditSearchLoader";

    private Cursor results;

    private URL url;

    public SubredditSearchLoader(Context context, URL url) {
        super(context);
        this.url = url;
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
    public Cursor loadInBackground() {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(stream));
            SearchParser parser = new SearchParser();
            parser.parseListingObject(reader);
            stream.close();

            connection.disconnect();

            return new SearchCursor(parser.results);

        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    static class SearchParser extends JsonParser {

        private List<Subreddit> results = new ArrayList<Subreddit>();

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

    static class SearchCursor extends AbstractCursor {

        private static final String FAKE_COLUMN_SUBSCRIBERS = "subscribers";

        private static final String[] PROJECTION = {
                Subreddits._ID, Subreddits.COLUMN_NAME, FAKE_COLUMN_SUBSCRIBERS,
        };

        private final List<Subreddit> results;

        SearchCursor(List<Subreddit> results) {
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
