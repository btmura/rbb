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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Scanner;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;

public class RedditApi {

    public static final String TAG = "RedditApi";

    static final String CHARSET = "UTF-8";
    static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=" + CHARSET;
    static final String USER_AGENT = "falling for reddit v3.4 by /u/btmura";

    private static final boolean LOG_RESPONSES = BuildConfig.DEBUG && !true;

    public static Result compose(String to, String subject, String text, String captchaId,
            String captchaGuess, String cookie, String modhash) throws IOException {
        return postData(Urls.compose(), Urls.composeQuery(to, subject, text,
                captchaId, captchaGuess, modhash), cookie);
    }

    public static Result submit(String subreddit, String title, String text, boolean link,
            String captchaId, String captchaGuess, String cookie, String modhash)
            throws IOException {
        return postData(Urls.submit(), Urls.submitQuery(subreddit, title, text, link, captchaId,
                captchaGuess, modhash), cookie);
    }

    private static Result postData(CharSequence url, CharSequence data, String cookie)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(url, cookie, true);
            writeFormData(conn, data);
            in = new BufferedInputStream(conn.getInputStream());
            if (LOG_RESPONSES) {
                in = logResponse(in);
            }
            return Result.fromJsonReader(new JsonReader(new InputStreamReader(in)));
        } finally {
            close(in, conn);
        }
    }

    public static HttpURLConnection connect(CharSequence url, String cookie,
                                            boolean doOutput) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) Urls2.newUrl(url).openConnection();
        conn.setInstanceFollowRedirects(false);
        setCommonHeaders(conn, cookie);
        if (doOutput) {
            setFormDataHeaders(conn);
        }
        conn.connect();
        return conn;
    }

    private static void setCommonHeaders(HttpURLConnection conn, String cookie) {
        conn.setRequestProperty("Accept-Charset", CHARSET);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (!TextUtils.isEmpty(cookie)) {
            conn.setRequestProperty("Cookie", Urls.loginCookie(cookie).toString());
        }
    }

    private static void setFormDataHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setDoOutput(true);
    }

    private static void writeFormData(HttpURLConnection conn, CharSequence data) throws IOException {
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(conn.getOutputStream());
            output.write(data.toString().getBytes(CHARSET));
            output.close();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Logs entire response and returns a fresh InputStream as if nothing happened. Make sure to
     * delete all usages of this method, since it is only for debugging.
     */
    static InputStream logResponse(InputStream in) throws IOException {
        // Make a copy of the InputStream.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int read = 0; (read = in.read(buffer)) != -1;) {
            out.write(buffer, 0, read);
        }
        in.close();

        // Print out the response for debugging purposes.
        in = new ByteArrayInputStream(out.toByteArray());
        Scanner sc = new Scanner(in);
        while (sc.hasNextLine()) {
            Log.d(TAG, sc.nextLine());
        }
        sc.close();

        // Return a new InputStream as if nothing happened...
        return new BufferedInputStream(new ByteArrayInputStream(out.toByteArray()));
    }

    private static void close(InputStream in, HttpURLConnection conn) throws IOException {
        if (in != null) {
            in.close();
        }
        if (conn != null) {
            conn.disconnect();
        }
    }
}
