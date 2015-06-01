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
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Scanner;

// TODO(btmura): remove RedditApi and replace with this class
public class RedditApi2 {

  // TODO(btmura): add RedditApiException type

  private static final String TAG = "RedditApi2";
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int HTTP_UNAUTHORIZED = 401;

  public static HttpURLConnection connect(
      Context ctx,
      String accountName,
      CharSequence url,
      boolean post) throws
      IOException,
      AuthenticatorException,
      OperationCanceledException {
    HttpURLConnection conn = innerConnect(ctx, accountName, url, post);

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

      conn = innerConnect(ctx, accountName, url, post);
    }

    return conn;
  }

  private static HttpURLConnection innerConnect(
      Context ctx,
      String accountName,
      CharSequence url,
      boolean post) throws
      AuthenticatorException,
      IOException,
      OperationCanceledException {
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

  public static AccountInfoResult getMyInfo(
      Context ctx,
      String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName, Urls2.myInfo(), false);
      return AccountInfoResult.getMyInfo(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static ArrayList<String> getMySubreddits(
      Context ctx,
      String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = connect(ctx, accountName, Urls2.mySubreddits(), false);
      in = new BufferedInputStream(conn.getInputStream());
      JsonReader r = new JsonReader(new InputStreamReader(in));
      SubredditParser p = new SubredditParser();
      p.parseListingObject(r);
      return p.results;
    } finally {
      close(in, conn);
    }
  }

  public static SidebarResult getSidebar(
      Context ctx,
      String accountName,
      String subreddit) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      CharSequence url = Urls2.sidebar(accountName, subreddit);
      conn = connect(ctx, accountName, url, false);
      in = new BufferedInputStream(conn.getInputStream());
      return SidebarResult
          .fromJsonReader(ctx, new JsonReader(new InputStreamReader(in)));
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
    try {
      conn = connect(ctx, accountName, Urls2.userInfo(accountName, user), false);
      return AccountInfoResult.getUserInfo(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static void markMessagesRead(Context ctx, String accountName) throws
      IOException,
      AuthenticatorException,
      OperationCanceledException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      CharSequence url = Urls2.messages(Filter.MESSAGE_INBOX, null, true);
      conn = connect(ctx, accountName, url, false);
      in = new BufferedInputStream(conn.getInputStream());
      in.read();
    } finally {
      close(in, conn);
    }
  }

  public static Result subscribe(
      Context ctx,
      String accountName,
      String subreddit,
      boolean subscribe) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName, Urls2.subscribe(), true);
      writeFormData(conn, Urls2.subscribeData(subreddit, subscribe));
      return Result.fromInputStream(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  private static void writeFormData(HttpURLConnection conn, CharSequence data) throws IOException {
    OutputStream output = null;
    try {
      output = new BufferedOutputStream(conn.getOutputStream());
      output.write(data.toString().getBytes(RedditApi.CHARSET));
      output.close();
    } finally {
      if (output != null) {
        output.close();
      }
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

  private static void close(HttpURLConnection conn) throws IOException {
    if (conn != null) {
      conn.disconnect();
      ;
    }
  }

  private RedditApi2() {
  }

  /**
   * Logs entire response and returns a fresh InputStream as if nothing
   * happened. Make sure to delete all usages of this method, since it is only
   * for debugging.
   */
  static InputStream logResponse(InputStream in) throws IOException {
    // Make a copy of the InputStream.
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    for (int read = 0; (read = in.read(buffer)) != -1; ) {
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

}
