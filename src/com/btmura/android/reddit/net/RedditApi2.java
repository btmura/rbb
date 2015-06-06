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

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

// TODO(btmura): remove RedditApi and replace with this class
public class RedditApi2 {

  // TODO(btmura): add RedditApiException type

  private static final String TAG = "RedditApi2";
  private static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int HTTP_UNAUTHORIZED = 401;

  // GET requests

  public static AccountInfoResult getMyInfo(Context ctx, String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName, Urls2.myInfo(), false);
      return AccountInfoResult.fromMyInfoJson(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static ArrayList<String> getMySubreddits(
      Context ctx,
      String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
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
      String subreddit)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    try {
      CharSequence url = Urls2.sidebar(accountName, subreddit);
      conn = connect(ctx, accountName, url, false);
      return SidebarResult.fromJson(ctx, conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static AccountInfoResult getUserInfo(
      Context ctx,
      String accountName,
      String user)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    try {
      CharSequence url = Urls2.userInfo(accountName, user);
      conn = connect(ctx, accountName, url, false);
      return AccountInfoResult.fromUserInfoJson(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static void markMessagesRead(Context ctx, String accountName)
      throws IOException, AuthenticatorException, OperationCanceledException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      CharSequence url = Urls2.messages(Filter.MESSAGE_INBOX, null, true);
      conn = connect(ctx, accountName, url, false);
      in = conn.getInputStream();
      in.read();
    } finally {
      close(in, conn);
    }
  }

  // POST requests

  public static Result comment(
      Context ctx,
      String accountName,
      String thingId,
      String text)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.comment(),
        Urls2.commentQuery(thingId, text));
  }

  public static Result hide(
      Context ctx,
      String accountName,
      String thingId,
      boolean hide)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.hide(hide), Urls2.hideQuery(thingId));
  }

  public static Result readMessage(
      Context ctx,
      String accountName,
      String thingId,
      boolean read)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.readMessage(read),
        Urls2.readMessageQuery(thingId));
  }

  public static Result save(
      Context ctx,
      String accountName,
      String thingId,
      boolean save)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.save(save), Urls2.saveQuery(thingId));
  }

  public static Result subscribe(
      Context ctx,
      String accountName,
      String subreddit,
      boolean subscribe)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.subscribe(),
        Urls2.subscribeData(subreddit, subscribe));
  }

  public static Result vote(
      Context ctx,
      String accountName,
      String thingId,
      int vote)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls2.vote(), Urls2.voteQuery(thingId, vote));
  }

  private static Result post(
      Context ctx,
      String accountName,
      CharSequence url,
      CharSequence data)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName, url, true);
      writeFormData(conn, data);
      return Result.fromJson(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static HttpURLConnection connect(
      Context ctx,
      String accountName,
      CharSequence url,
      boolean post)
      throws IOException, AuthenticatorException, OperationCanceledException {
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
      boolean post)
      throws AuthenticatorException, IOException, OperationCanceledException {
    HttpURLConnection conn =
        (HttpURLConnection) Urls2.newUrl(url).openConnection();
    conn.setInstanceFollowRedirects(false);
    setCommonHeaders(ctx, accountName, conn);
    if (post) {
      setFormDataHeaders(conn);
    }
    conn.connect();
    return conn;
  }

  private static void setCommonHeaders(
      Context ctx,
      String accountName,
      HttpURLConnection conn)
      throws AuthenticatorException, OperationCanceledException, IOException {
    conn.setRequestProperty("Accept-Charset", RedditApi.CHARSET);
    conn.setRequestProperty("User-Agent", RedditApi.USER_AGENT);
    if (AccountUtils.isAccount(accountName)) {
      String at = AccountUtils.getAccessToken(ctx, accountName);
      // TODO(btmura): handle empty access token
      conn.setRequestProperty("Authorization", "bearer " + at);
    }
  }

  private static void setFormDataHeaders(HttpURLConnection conn) {
    conn.setRequestProperty("Content-Type", RedditApi.CONTENT_TYPE);
    conn.setDoOutput(true);
  }

  private static void writeFormData(HttpURLConnection conn, CharSequence data)
      throws IOException {
    OutputStream output = null;
    try {
      output = new BufferedOutputStream(conn.getOutputStream());
      output.write(data.toString().getBytes(RedditApi.CHARSET));
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

  private static void close(InputStream in, HttpURLConnection conn)
      throws IOException {
    if (in != null) {
      in.close();
    }
    close(conn);
  }

  private static void close(HttpURLConnection conn) throws IOException {
    if (conn != null) {
      conn.disconnect();
    }
  }

  private RedditApi2() {
  }
}
