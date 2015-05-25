/*
 * Copyright (C) 2015 Brian Muramatsu
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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

// TODO(btmura): remove RedditApi and replace with this class
public class RedditApi2 {

    private static final String TAG = "RedditApi2";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final int HTTP_UNAUTHORIZED = 401;

    public static HttpURLConnection connect(
            Context ctx,
            String accountName,
            CharSequence url)
            throws IOException, AuthenticatorException, OperationCanceledException {
        HttpURLConnection conn = innerConnect(ctx, accountName, url);

        if (AccountUtils.isAccount(accountName) && conn.getResponseCode() == HTTP_UNAUTHORIZED) {
            conn.disconnect();

            // TODO(btmura): put refresh token code in separate method
            String rt = AccountUtils.getRefreshToken(ctx, accountName);
            // TODO(btmura): handle empty refresh token
            AccessTokenResult atr = AccessTokenResult.refreshAccessToken(ctx, rt);
            // TODO(btmura): validate access token result
            AccountUtils.setAccessToken(ctx, accountName, atr.accessToken);

            conn = innerConnect(ctx, accountName, url);
        }

        return conn;
    }


    private static HttpURLConnection innerConnect(
            Context ctx,
            String accountName,
            CharSequence url)
            throws IOException, AuthenticatorException, OperationCanceledException {
        HttpURLConnection conn = (HttpURLConnection) Urls2.newUrl(url).openConnection();
        conn.setRequestProperty("Accept-Charset", RedditApi.CHARSET);
        conn.setRequestProperty("User-Agent", RedditApi.USER_AGENT);
        if (AccountUtils.isAccount(accountName)) {
            String at = AccountUtils.getAccessToken(ctx, accountName);
            // TODO(btmura): handle empty access token
            conn.setRequestProperty("Authorization", "bearer " + at);
        }
        conn.connect();
        if (DEBUG) {
            Log.d(TAG, "response code: " + conn.getResponseCode());
        }
        return conn;
    }


    private RedditApi2() {
    }
}
