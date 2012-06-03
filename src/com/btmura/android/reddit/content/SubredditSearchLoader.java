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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.util.JsonReader;
import android.util.Log;

public class SubredditSearchLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = "SubredditSearchLoader";

    private Cursor results;

    private final URL url;

    public SubredditSearchLoader(Context context, URL url) {
        super(context.getApplicationContext());
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
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            stream.close();

            connection.disconnect();

            return new SubredditCursor(parser.results);

        } catch (MalformedURLException e) {
            Log.e(TAG, "loadInBackground", e);
        } catch (IOException e) {
            Log.e(TAG, "loadInBackground", e);
        }

        return null;
    }
}
