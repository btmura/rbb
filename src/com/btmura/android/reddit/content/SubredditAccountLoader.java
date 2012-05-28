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
import java.net.URL;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.data.Urls;

public class SubredditAccountLoader extends AsyncTaskLoader<Cursor> {

    public static final String TAG = "SubredditAccountLoader";

    private final String cookie;

    private Cursor results;

    public SubredditAccountLoader(Context context, String cookie) {
        super(context);
        this.cookie = cookie;
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
            URL url = Urls.subredditListUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
            conn.setRequestProperty("Cookie", Urls.loginCookie(cookie));
            conn.connect();
            
            InputStream in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            
            in.close();
            
            conn.disconnect();
            
            return new SubredditCursor(parser.results);
            
        } catch (IOException e) {
            Log.e(TAG, "loadInBackground", e);
        }

        return null;
    }
}
