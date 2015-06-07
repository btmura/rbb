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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.text.MarkdownFormatter;

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

public class RedditApi {

  private static final String TAG = "RedditApi2";

  static final String CHARSET = "UTF-8";
  static final String CONTENT_TYPE =
      "application/x-www-form-urlencoded;charset=" + CHARSET;
  static final String USER_AGENT = "falling for reddit v3.4 by /u/btmura";

  private static final boolean LOG_RESPONSES = BuildConfig.DEBUG;

  private static final int HTTP_UNAUTHORIZED = 401;

  // GET requests

  public static Bitmap getBitmap(CharSequence url) throws IOException {
    HttpURLConnection conn = null;
    InputStream in = null;
    try {
      conn = noAuthConnect(url);
      in = new BufferedInputStream(conn.getInputStream());
      return BitmapFactory.decodeStream(in);
    } finally {
      close(in, conn);
    }
  }

  public static Bitmap getCaptcha(String id) throws IOException {
    return getBitmap(Urls.captcha(id));
  }

  public static AccountInfoResult getMyInfo(Context ctx, String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName, Urls.myInfo(), false);
      return AccountInfoResult.fromMyInfoJson(conn.getInputStream());
    } finally {
      close(conn);
    }
  }

  public static ThingBundle getThingInfo(
      Context ctx,
      String accountName,
      String thingId,
      MarkdownFormatter formatter)
      throws IOException, AuthenticatorException, OperationCanceledException {
    HttpURLConnection conn = null;
    try {
      conn = connect(ctx, accountName,
          Urls.thingInfo(accountName, thingId), false);
      return ThingBundle.fromJsonReader(ctx,
          new JsonReader(new InputStreamReader(conn.getInputStream())),
          formatter);
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
      conn = connect(ctx, accountName, Urls.mySubreddits(), false);
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
      CharSequence url = Urls.sidebar(accountName, subreddit);
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
      CharSequence url = Urls.userInfo(accountName, user);
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
      CharSequence url = Urls.messages(Filter.MESSAGE_INBOX, null, true);
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
    return post(ctx, accountName, Urls.comment(),
        Urls.commentQuery(thingId, text));
  }

  public static Result compose(
      Context ctx,
      String accountName,
      String to,
      String subject,
      String text,
      String captchaId,
      String captchaGuess)
      throws IOException, AuthenticatorException, OperationCanceledException {
    return post(ctx, accountName, Urls.compose(),
        Urls.composeQuery(to, subject, text, captchaId, captchaGuess));
  }

  public static Result delete(Context ctx, String accountName, String thingId)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.delete(), Urls.deleteQuery(thingId));
  }

  public static Result edit(
      Context ctx,
      String accountName,
      String thingId,
      String text)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.edit(), Urls.editQuery(thingId, text));
  }

  public static Result hide(
      Context ctx,
      String accountName,
      String thingId,
      boolean hide)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.hide(hide), Urls.hideQuery(thingId));
  }

  public static Result readMessage(
      Context ctx,
      String accountName,
      String thingId,
      boolean read)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.readMessage(read),
        Urls.readMessageQuery(thingId));
  }

  public static Result save(
      Context ctx,
      String accountName,
      String thingId,
      boolean save)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.save(save), Urls.saveQuery(thingId));
  }

  public static Result submit(
      Context ctx,
      String accountName,
      String subreddit,
      String title,
      String text,
      boolean link,
      String captchaId,
      String captchaGuess)
      throws IOException, AuthenticatorException, OperationCanceledException {
    return post(ctx, accountName, Urls.submit(),
        Urls.submitQuery(subreddit, title, text, link, captchaId,
            captchaGuess));
  }

  public static Result subscribe(
      Context ctx,
      String accountName,
      String subreddit,
      boolean subscribe)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.subscribe(),
        Urls.subscribeData(subreddit, subscribe));
  }

  public static Result vote(
      Context ctx,
      String accountName,
      String thingId,
      int vote)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName, Urls.vote(), Urls.voteQuery(thingId, vote));
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
      return Result.fromJson(logResponse(conn.getInputStream()));
    } finally {
      close(conn);
    }
  }

  public static HttpURLConnection connect(
      Context ctx,
      String accountName,
      CharSequence url,
      boolean doPost)
      throws IOException, AuthenticatorException, OperationCanceledException {
    HttpURLConnection conn = innerConnect(ctx, accountName, url, doPost);

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

      conn = innerConnect(ctx, accountName, url, doPost);
    }

    return conn;
  }

  private static HttpURLConnection innerConnect(
      Context ctx,
      String accountName,
      CharSequence url,
      boolean doPost)
      throws AuthenticatorException, IOException, OperationCanceledException {
    HttpURLConnection conn =
        (HttpURLConnection) Urls.newUrl(url).openConnection();
    conn.setInstanceFollowRedirects(false);
    setCommonHeaders(conn);
    setAuthorizationHeader(ctx, accountName, conn);
    if (doPost) {
      setFormDataHeaders(conn);
    }
    conn.connect();
    return conn;
  }

  private static HttpURLConnection noAuthConnect(CharSequence url)
      throws IOException {
    HttpURLConnection conn =
        (HttpURLConnection) Urls.newUrl(url).openConnection();
    conn.setInstanceFollowRedirects(false);
    setCommonHeaders(conn);
    conn.connect();
    return conn;
  }

  private static void setCommonHeaders(HttpURLConnection conn) {
    conn.setRequestProperty("Accept-Charset", CHARSET);
    conn.setRequestProperty("User-Agent", USER_AGENT);
  }

  private static void setAuthorizationHeader(
      Context ctx,
      String accountName,
      HttpURLConnection conn)
      throws AuthenticatorException, OperationCanceledException, IOException {
    if (AccountUtils.isAccount(accountName)) {
      String at = AccountUtils.getAccessToken(ctx, accountName);
      // TODO(btmura): handle empty access token
      conn.setRequestProperty("Authorization", "bearer " + at);
    }
  }

  private static void setFormDataHeaders(HttpURLConnection conn) {
    conn.setRequestProperty("Content-Type", CONTENT_TYPE);
    conn.setDoOutput(true);
  }

  private static void writeFormData(HttpURLConnection conn, CharSequence data)
      throws IOException {
    OutputStream output = null;
    try {
      output = new BufferedOutputStream(conn.getOutputStream());
      output.write(data.toString().getBytes(CHARSET));
    } finally {
      if (output != null) {
        output.close();
      }
    }
  }

  // TODO(btmura): make private
  static InputStream logResponse(InputStream in) throws IOException {
    if (!LOG_RESPONSES) {
      return in;
    }

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

  private RedditApi() {
  }
}
