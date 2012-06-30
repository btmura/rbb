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
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.JsonReader;

import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Thing;

public class NetApi {

    static ArrayList<String> querySubreddits(String cookie) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = Urls.subredditListUrl();
            conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn, cookie);
            conn.connect();

            in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            return parser.results;

        } finally {
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static ArrayList<Thing> queryThings(Context context, URL url, String cookie,
            String parentSubreddit, List<Thing> initThings) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn, cookie);
            conn.connect();

            in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            ThingParser parser = new ThingParser(context, parentSubreddit, initThings);
            parser.parseListingObject(reader);
            return parser.things;

        } finally {
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static void subscribe(String cookie, String modhash, String subreddit, boolean subscribe)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = Urls.subscribeUrl();
            conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn, cookie);
            setFormDataHeaders(conn);

            conn.connect();
            writeFormData(conn, Urls.subscribeQuery(modhash, subreddit, subscribe));
            in = conn.getInputStream();

        } finally {
            if (in != null) {
                in.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void setCommonHeaders(HttpURLConnection conn, String cookie) {
        conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
        conn.setRequestProperty("User-Agent", Urls.USER_AGENT);
        if (!TextUtils.isEmpty(cookie)) {
            conn.setRequestProperty("Cookie", Urls.loginCookie(cookie));
        }
    }

    private static void setFormDataHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", Urls.CONTENT_TYPE);
        conn.setDoOutput(true);
    }

    private static void writeFormData(HttpURLConnection conn, String data) throws IOException {
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
}
