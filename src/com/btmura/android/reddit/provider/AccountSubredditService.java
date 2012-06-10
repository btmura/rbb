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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.btmura.android.reddit.data.Urls;

public class AccountSubredditService extends IntentService {

    public static final String TAG = "AccountSubredditService";

    public static final String EXTRA_COOKIE = "c";
    public static final String EXTRA_MODHASH = "m";

    public static final String EXTRA_ACTION = "a";
    public static final String EXTRA_SUBREDDIT = "s";

    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;

    static Intent getAddSubredditIntent(Context context, String subreddit) {
        Intent intent = new Intent(context, AccountSubredditService.class);
        intent.putExtra(EXTRA_ACTION, ACTION_ADD);
        intent.putExtra(EXTRA_SUBREDDIT, subreddit);
        return intent;
    }

    public static Intent getRemoveSubredditIntent(Context context, String subreddit) {
        Intent intent = new Intent(context, AccountSubredditService.class);
        intent.putExtra(EXTRA_ACTION, ACTION_REMOVE);
        intent.putExtra(EXTRA_SUBREDDIT, subreddit);
        return intent;
    }

    public AccountSubredditService() {
        super("AccountSubredditServiceWorker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int action = intent.getIntExtra(EXTRA_ACTION, -1);
        switch (action) {
            case ACTION_ADD:
                handleAddSubreddit(intent);
                break;

            case ACTION_REMOVE:
                handleRemoveSubreddit(intent);
                break;

            default:
                throw new IllegalArgumentException();
        }

    }

    private void handleAddSubreddit(Intent intent) {
        subscribe(intent, true);
    }

    private void handleRemoveSubreddit(Intent intent) {
        subscribe(intent, false);
    }

    private void subscribe(Intent intent, boolean subscribe) {
        String cookie = intent.getStringExtra(EXTRA_COOKIE);
        String modhash = intent.getStringExtra(EXTRA_MODHASH);
        String subreddit = intent.getStringExtra(EXTRA_SUBREDDIT);

        URL url = Urls.subscribeUrl();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            setCommonHeaders(conn, cookie);
            setFormDataHeaders(conn);

            conn.connect();
            writeFormData(conn, Urls.subscribeQuery(modhash, subreddit, subscribe));

            conn.getInputStream().close();
            conn.disconnect();

        } catch (IOException e) {
            Log.e(TAG, "subscribe", e);
        }
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
