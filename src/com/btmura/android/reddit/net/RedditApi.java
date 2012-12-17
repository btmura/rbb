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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.util.Array;

public class RedditApi {

    public static final String TAG = "RedditApi";

    private static final String CHARSET = "UTF-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset="
            + CHARSET;
    private static final String USER_AGENT = "reddit by brian (rbb) for Android by /u/btmura";

    public static class Result {
        public double rateLimit;

        /** Example: [["BAD_CAPTCHA", "care to try these again?", "captcha"]] */
        public String[][] errors;

        /** Example: D5GggaXa0GWshObkjzzEPzzrK8zpQfeB */
        public String captcha;

        public String iden;

        /** Example: http://www.reddit.com/r/rbb/comments/w5mhh/test/ */
        public String url;

        /** Example: t3_w5mhh */
        public String name;

        public CharSequence getErrorMessage(Context context) {
            if (Array.isEmpty(errors)) {
                return "";
            }
            StringBuilder b = new StringBuilder();

            // Append a newline if there are multiple errors.
            if (errors.length > 1) {
                b.append("\n");
            }

            for (int i = 0; i < errors.length; i++) {
                b.append(context.getString(R.string.reddit_error_line,
                        errors[i][0], errors[i][1]));
                if (i + 1 < errors.length) {
                    b.append("\n");
                }
            }

            return context.getString(R.string.reddit_error, b);
        }

        public boolean shouldRetry() {
            return hasRateLimitError();
        }

        private boolean hasRateLimitError() {
            if (!Array.isEmpty(errors)) {
                for (int i = 0; i < errors.length; i++) {
                    if ("RATE_LIMIT".equals(errors[i][0])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void logAnyErrors(String tag) {
            if (!Array.isEmpty(errors)) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < errors.length; i++) {
                    line.delete(0, line.length());
                    for (int j = 0; j < errors[i].length; j++) {
                        line.append(errors[i][j]);
                        if (j + 1 < errors[i].length) {
                            line.append(" ");
                        }
                    }
                    Log.d(tag, line.toString());
                }
            }
        }
    }

    // TODO: Merge with Result.
    public static class LoginResult {
        public String cookie;
        public String modhash;
        public String error;
    }

    // TODO: Merge with Result.
    public static class SidebarResult {
        public String subreddit;
        public CharSequence title;
        public int subscribers;
        public CharSequence description;
    }

    public static Result comment(String thingId, String text, String cookie, String modhash)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.commentsApiUrl(), cookie, true);
            writeFormData(conn, Urls.commentsApiQuery(thingId, text, modhash));
            in = conn.getInputStream();
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static Result delete(String thingId, String cookie, String modhash) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.deleteApiUrl(), cookie, true);
            writeFormData(conn, Urls.deleteApiQuery(thingId, modhash));
            in = conn.getInputStream();
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static Bitmap getCaptcha(String id) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.captchaUrl(id), null, false);
            in = conn.getInputStream();
            return BitmapFactory.decodeStream(in);
        } finally {
            close(in, conn);
        }
    }

    public static Result newCaptcha() throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.newCaptchaUrl(), null, true);
            writeFormData(conn, Urls.newCaptchaQuery());
            in = conn.getInputStream();
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static SidebarResult getSidebar(Context context, String subreddit, String cookie)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = Urls.sidebarUrl(subreddit);
            conn = connect(url, cookie, false);

            in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SidebarParser parser = new SidebarParser(context);
            parser.parseEntity(reader);
            return parser.results;
        } finally {
            close(in, conn);
        }
    }

    public static ArrayList<String> getSubreddits(String cookie) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.subredditListUrl(1000), cookie, false);

            in = conn.getInputStream();
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            SubredditParser parser = new SubredditParser();
            parser.parseListingObject(reader);
            return parser.results;
        } finally {
            close(in, conn);
        }
    }

    public static LoginResult login(Context context, String login, String password)
            throws IOException {
        HttpsURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = Urls.loginUrl(login);
            conn = (HttpsURLConnection) url.openConnection();
            setCommonHeaders(conn, null);
            setFormDataHeaders(conn);
            conn.connect();

            writeFormData(conn, Urls.loginQuery(login, password));
            in = conn.getInputStream();
            return LoginParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static void subscribe(String cookie, String modhash, String subreddit,
            boolean subscribe) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.subscribeUrl(), cookie, true);
            conn.connect();

            writeFormData(conn, Urls.subscribeQuery(modhash, subreddit, subscribe));
            in = conn.getInputStream();
            if (BuildConfig.DEBUG) {
                logResponse(in);
            }
        } finally {
            close(in, conn);
        }
    }

    public static Result compose(String to, String subject, String text, String captchaId,
            String captchaGuess, String cookie, String modhash) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.composeUrl(), cookie, true);
            writeFormData(conn, Urls.composeQuery(to, subject, text,
                    captchaId, captchaGuess, modhash));
            in = conn.getInputStream();
            return ResponseParser.parseResponse(logResponse(in));
        } finally {
            close(in, conn);
        }
    }

    public static Result submit(String subreddit, String title, String text, boolean link,
            String captchaId, String captchaGuess, String cookie, String modhash)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.submitUrl(), cookie, true);
            writeFormData(conn, Urls.submitQuery(modhash, subreddit, title, text, link,
                    captchaId, captchaGuess));
            in = conn.getInputStream();
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static Result vote(Context context, String name, int vote, String cookie,
            String modhash) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.voteUrl(), cookie, true);
            writeFormData(conn, Urls.voteQuery(modhash, name, vote));
            in = conn.getInputStream();
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static HttpURLConnection connect(URL url, String cookie, boolean doOutput)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
            conn.setRequestProperty("Cookie", Urls.loginCookie(cookie));
        }
    }

    private static void setFormDataHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setDoOutput(true);
    }

    private static void writeFormData(HttpURLConnection conn, String data) throws IOException {
        OutputStream output = null;
        try {
            output = conn.getOutputStream();
            output.write(data.getBytes(CHARSET));
            output.close();
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    /**
     * Logs entire response and returns a fresh InputStream as if nothing
     * happened. Make sure to delete all usages of this method, since it is only
     * for debugging.
     */
    private static InputStream logResponse(InputStream in) throws IOException {
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
        return new ByteArrayInputStream(out.toByteArray());
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
