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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
import com.btmura.android.reddit.util.JsonParser;

public class RedditApi {

    public static final String TAG = "RedditApi";

    private static final String CHARSET = "UTF-8";
    private static final String CONTENT_TYPE =
            "application/x-www-form-urlencoded;charset=" + CHARSET;
    static final String USER_AGENT = "reddit by brian v2.0 by /u/btmura";

    private static final boolean LOG_RESPONSES = BuildConfig.DEBUG && !true;

    public static class Result {

        /** Error for missing or incorrect captcha guess. */
        private static final String ERROR_BAD_CAPTCHA = "BAD_CAPTCHA";

        /** Error for too much user activity. */
        private static final String ERROR_RATELIMIT = "RATELIMIT";

        public double rateLimit;

        /**
         * Example:
         * [[BAD_CAPTCHA, care to try these again?, captcha],
         *  [RATELIMIT, you are doing that too much. try again in 6 minutes., ratelimit]]
         */
        public String[][] errors;

        /** Example: D5GggaXa0GWshObkjzzEPzzrK8zpQfeB */
        public String captcha;

        /** Captcha id returned when asking for a new captcha. */
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

        public boolean hasErrors() {
            return !Array.isEmpty(errors);
        }

        public boolean hasRateLimitError() {
            return hasError(ERROR_RATELIMIT);
        }

        public boolean hasBadCaptchaError() {
            return hasError(ERROR_BAD_CAPTCHA);
        }

        private boolean hasError(String errorCode) {
            if (!Array.isEmpty(errors)) {
                for (int i = 0; i < errors.length; i++) {
                    if (errorCode.equals(errors[i][0])) {
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

    /**
     * {@link AccountResult} is the result of calling the
     * {@link RedditApi#me(String)} method.
     */
    public static class AccountResult extends JsonParser {

        /** Amount of link karma. */
        public int linkKarma;

        /** Amount of comment karma. */
        public int commentKarma;

        /** True if the account has mail. False otherwise. */
        public boolean hasMail;

        /** Return a new {@link AccountResult} from a {@link JsonReader}. */
        public static AccountResult fromJsonReader(JsonReader reader) throws IOException {
            AccountResult result = new AccountResult();
            result.parseEntity(reader);
            return result;
        }

        private AccountResult() {
            // Use the fromJsonReader method.
        }

        @Override
        public void onLinkKarma(JsonReader reader, int index) throws IOException {
            linkKarma = reader.nextInt();
        }

        @Override
        public void onCommentKarma(JsonReader reader, int index) throws IOException {
            commentKarma = reader.nextInt();
        }

        @Override
        public void onHasMail(JsonReader reader, int index) throws IOException {
            hasMail = reader.nextBoolean();
        }
    }

    public static Result comment(String thingId, String text, String cookie, String modhash)
            throws IOException {
        return postData(Urls.comments(), Urls.commentsQuery(thingId, text, modhash), cookie);
    }

    public static Result edit(String thingId, String text, String cookie, String modhash)
            throws IOException {
        return postData(Urls.edit(), Urls.editQuery(thingId, text, modhash), cookie);
    }

    public static Result delete(String thingId, String cookie, String modhash) throws IOException {
        return postData(Urls.delete(), Urls.deleteQuery(thingId, modhash), cookie);
    }

    public static Bitmap getCaptcha(String id) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.captcha(id), null, true, false);
            in = conn.getInputStream();
            return BitmapFactory.decodeStream(in);
        } finally {
            close(in, conn);
        }
    }

    public static Result newCaptcha() throws IOException {
        return postData(Urls.newCaptcha(), Urls.newCaptchaQuery(), null);
    }

    public static SidebarResult getSidebar(Context context, String subreddit, String cookie)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.sidebar(subreddit), cookie, true, false);
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
            conn = connect(Urls.subredditList(1000), cookie, true, false);
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
            CharSequence url = Urls.login(login);
            conn = (HttpsURLConnection) Urls.newUrl(url).openConnection();
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

    public static AccountResult me(String cookie) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(Urls.me(), cookie, true, false);
            in = new BufferedInputStream(conn.getInputStream());
            return AccountResult.fromJsonReader(new JsonReader(new InputStreamReader(in)));
        } finally {
            close(in, conn);
        }
    }

    public static Result readMessage(String thingId, boolean read, String cookie, String modhash)
            throws IOException {
        if (read) {
            return postData(Urls.readMessage(), Urls.readMessageQuery(thingId, modhash), cookie);
        } else {
            return postData(Urls.unreadMessage(), Urls.unreadMessageQuery(thingId, modhash),
                    cookie);
        }
    }

    public static Result save(String thingId, boolean save, String cookie, String modhash)
            throws IOException {
        return postData(Urls.save(save), Urls.saveQuery(thingId, modhash), cookie);
    }

    public static Result subscribe(String subreddit, boolean subscribe, String cookie,
            String modhash) throws IOException {
        return postData(Urls.subscribe(), Urls.subscribeQuery(subreddit, subscribe, modhash),
                cookie);
    }

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

    public static Result vote(Context context, String thingId, int vote, String cookie,
            String modhash) throws IOException {
        return postData(Urls.vote(), Urls.voteQuery(thingId, vote, modhash), cookie);
    }

    private static Result postData(CharSequence url, CharSequence data, String cookie)
            throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = connect(url, cookie, true, true);
            conn.connect();
            writeFormData(conn, data);
            in = conn.getInputStream();
            if (LOG_RESPONSES) {
                in = logResponse(in);
            }
            return ResponseParser.parseResponse(in);
        } finally {
            close(in, conn);
        }
    }

    public static HttpURLConnection connect(CharSequence url, String cookie,
            boolean followRedirects, boolean doOutput) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) Urls.newUrl(url).openConnection();
        conn.setInstanceFollowRedirects(followRedirects);
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
            output = conn.getOutputStream();
            output.write(data.toString().getBytes(CHARSET));
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
