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
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

// TODO(btmura): remove RedditApi and replace with this class
public class RedditApi2 {

  // TODO(btmura): add RedditApiException type

  private static final String TAG = "RedditApi2";
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int HTTP_UNAUTHORIZED = 401;

  public static HttpURLConnection connect(
      Context ctx,
      String accountName,
      CharSequence url)
      throws IOException, AuthenticatorException, OperationCanceledException {
    HttpURLConnection conn = innerConnect(ctx, accountName, url);

    // TODO(btmura): check whether error is scope problem or not
    if (AccountUtils.isAccount(accountName)
        && conn.getResponseCode() == HTTP_UNAUTHORIZED) {
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
    HttpURLConnection conn =
        (HttpURLConnection) Urls2.newUrl(url).openConnection();
    conn.setInstanceFollowRedirects(false);
    conn.setRequestProperty("Accept-Charset", RedditApi.CHARSET);
    conn.setRequestProperty("User-Agent", RedditApi.USER_AGENT);
    if (AccountUtils.isAccount(accountName)) {
      String at = AccountUtils.getAccessToken(ctx, accountName);
      // TODO(btmura): handle empty access token
      conn.setRequestProperty("Authorization", "bearer " + at);
    }
    conn.connect();
    if (DEBUG) {
      Log.d(TAG, "url: " + url + " response code: " + conn.getResponseCode());
    }
    return conn;
  }

  public static ArrayList<String> getMySubreddits(
      Context ctx,
      String accountName)
      throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = connect(ctx, accountName, Urls2.mySubreddits());
      in = new BufferedInputStream(conn.getInputStream());
      JsonReader r = new JsonReader(new InputStreamReader(in));
      SubredditParser p = new SubredditParser();
      p.parseListingObject(r);
      return p.results;
    } finally {
      close(in, conn);
    }
  }

  public static AccountInfoResult getUserInfo(
      Context ctx,
      String accountName,
      String user) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      CharSequence url = Urls2.userInfo(accountName, user, Urls2.TYPE_JSON);
      conn = connect(ctx, accountName, url);
      in = new BufferedInputStream(conn.getInputStream());
      return AccountInfoResult.fromJsonReader(
          new JsonReader(new InputStreamReader(in)));
    } finally {
      close(in, conn);
    }
  }

  public static void markMessagesRead(Context ctx, String accountName) throws
      IOException,
      AuthenticatorException,
      OperationCanceledException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      CharSequence url = Urls2.messages(accountName, Filter.MESSAGE_UNREAD,
          null, true, Urls.TYPE_HTML);
      conn = connect(ctx, accountName, url);
      in = new BufferedInputStream(conn.getInputStream());
      in.read();
    } finally {
      close(in, conn);
    }
  }

  private static void close(InputStream in, HttpURLConnection conn) throws
      IOException {
    if (in != null) {
      in.close();
    }
    if (conn != null) {
      conn.disconnect();
    }
  }

  private RedditApi2() {
  }
}
