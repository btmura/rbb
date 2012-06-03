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

package com.btmura.android.reddit.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class HttpUtils {

    public static final String TAG = "HttpUtils";

    private static final String CHARSET_VALUE = "UTF-8";
    private static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded;charset="
            + CHARSET_VALUE;

    public static InputStream post(URL url, String cookie, String data) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept-Charset", CHARSET_VALUE);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE_VALUE);
            conn.setRequestProperty("Cookie", cookie);
            conn.setDoOutput(true);
            conn.connect();
            writeData(conn, data);
            return conn.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "post", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    private static void writeData(HttpURLConnection conn, String data) throws IOException {
        OutputStream output = null;
        try {
            output = conn.getOutputStream();
            output.write(data.getBytes(CHARSET_VALUE));
            output.close();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}
