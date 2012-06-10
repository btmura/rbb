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

import android.util.JsonReader;

import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Subreddit;

public class NetApi {

    static ArrayList<Subreddit> query(String cookie) throws IOException {
        URL url = Urls.subredditListUrl();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setCommonHeaders(conn, cookie);
        conn.connect();

        InputStream in = conn.getInputStream();
        JsonReader reader = new JsonReader(new InputStreamReader(in));
        SubredditParser parser = new SubredditParser();
        parser.parseListingObject(reader);
        in.close();
        conn.disconnect();

        return parser.results;
    }

    static void subscribe(String cookie, String modhash, String subreddit, boolean subscribe)
            throws IOException {
        URL url = Urls.subscribeUrl();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        setCommonHeaders(conn, cookie);
        setFormDataHeaders(conn);

        conn.connect();
        writeFormData(conn, Urls.subscribeQuery(modhash, subreddit, subscribe));

        conn.getInputStream().close();
        conn.disconnect();
    }

    private static void setCommonHeaders(HttpURLConnection conn, String cookie) {
        conn.setRequestProperty("Accept-Charset", Urls.CHARSET);
        conn.setRequestProperty("User-Agent", Urls.USER_AGENT);
        conn.setRequestProperty("Cookie", Urls.loginCookie(cookie));
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
