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
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.app.ThingBundle;
import com.btmura.android.reddit.text.MarkdownFormatter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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

  public static AccessTokenResult getAccessToken(Context ctx, CharSequence code)
      throws IOException {
    return getToken(ctx, code, null);
  }

  public static Bitmap getBitmap(CharSequence url) throws IOException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      conn = noAuthConnect(url);
      is = conn.getInputStream();
      return BitmapFactory.decodeStream(is);
    } finally {
      close(is, conn);
    }
  }

  public static Bitmap getCaptcha(String id) throws IOException {
    return getBitmap(Urls.captcha(id));
  }

  public static AccountInfoResult getMyInfo(Context ctx, String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    JsonReader r = null;
    try {
      conn = connect(ctx, accountName, Urls.myInfo(), false);
      r = newJsonReader(conn.getInputStream());
      return AccountInfoResult.getMyInfo(r);
    } finally {
      close(r, conn);
    }
  }

  public static ThingBundle getThingInfo(
      Context ctx,
      String accountName,
      String thingId,
      MarkdownFormatter formatter)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      conn = connect(ctx, accountName,
          Urls.thingInfo(accountName, thingId), false);
      is = conn.getInputStream();
      return ThingBundle.fromJsonReader(ctx,
          new JsonReader(new InputStreamReader(is)),
          formatter);
    } finally {
      close(is, conn);
    }
  }

  public static ArrayList<String> getMySubreddits(
      Context ctx,
      String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      conn = connect(ctx, accountName, Urls.mySubreddits(), false);
      is = new BufferedInputStream(conn.getInputStream());
      JsonReader r = new JsonReader(new InputStreamReader(is));
      SubredditParser p = new SubredditParser();
      p.parseListingObject(r);
      return p.results;
    } finally {
      close(is, conn);
    }
  }

  public static SidebarResult getSidebar(
      Context ctx,
      String accountName,
      String subreddit)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      conn = connect(ctx, accountName,
          Urls.sidebar(accountName, subreddit), false);
      is = conn.getInputStream();
      return SidebarResult.fromJson(ctx, is);
    } finally {
      close(is, conn);
    }
  }

  public static AccountInfoResult getUserInfo(
      Context ctx,
      String accountName,
      String user)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    JsonReader r = null;
    try {
      conn = connect(ctx, accountName, Urls.userInfo(accountName, user),
          false);
      r = newJsonReader(conn.getInputStream());
      return AccountInfoResult.getUserInfo(r);
    } finally {
      close(r, conn);
    }
  }

  public static void markMessagesRead(Context ctx, String accountName)
      throws IOException, AuthenticatorException, OperationCanceledException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      CharSequence url = Urls.messages(Filter.MESSAGE_INBOX, null, true);
      conn = connect(ctx, accountName, url, false);
      is = conn.getInputStream();
      while (is.read() != -1) {
        // read entire input stream
      }
    } finally {
      close(is, conn);
    }
  }

  // POST requests

  public static Result comment(
      Context ctx,
      String accountName,
      String thingId,
      String text)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.comment(),
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
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.compose(),
        Urls.composeQuery(to, subject, text, captchaId, captchaGuess));
  }

  public static Result delete(Context ctx, String accountName, String thingId)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.delete(),
        Urls.deleteQuery(thingId));
  }

  public static Result edit(
      Context ctx,
      String accountName,
      String thingId,
      String text)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.edit(),
        Urls.editQuery(thingId, text));
  }

  public static Result hide(
      Context ctx,
      String accountName,
      String thingId,
      boolean hide)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.hide(hide),
        Urls.hideQuery(thingId));
  }

  public static Result readMessage(
      Context ctx,
      String accountName,
      String thingId,
      boolean read)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.readMessage(read),
        Urls.readMessageQuery(thingId));
  }

  public static Result save(
      Context ctx,
      String accountName,
      String thingId,
      boolean save)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.save(save),
        Urls.saveQuery(thingId));
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
    return post(ctx, accountName,
        Urls.submit(),
        Urls.submitQuery(subreddit, title, text, link, captchaId,
            captchaGuess));
  }

  public static Result subscribe(
      Context ctx,
      String accountName,
      String subreddit,
      boolean subscribe)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.subscribe(),
        Urls.subscribeData(subreddit, subscribe));
  }

  public static Result vote(
      Context ctx,
      String accountName,
      String thingId,
      int vote)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return post(ctx, accountName,
        Urls.vote(),
        Urls.voteQuery(thingId, vote));
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
      AccessTokenResult atr = refreshToken(ctx, rt);
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
    setOAuthHeader(ctx, accountName, conn);
    if (doPost) {
      setPostDataHeaders(conn);
    }
    conn.connect();
    return conn;
  }

  private static AccessTokenResult refreshToken(
      Context ctx,
      CharSequence refreshToken) throws IOException {
    return getToken(ctx, null, refreshToken);
  }

  private static AccessTokenResult getToken(
      Context ctx,
      @Nullable CharSequence code,
      @Nullable CharSequence refreshToken)
      throws IOException {
    HttpURLConnection conn = null;
    JsonReader r = null;
    try {
      conn = (HttpURLConnection) Urls.newUrl(Urls.accessToken())
          .openConnection();
      conn.setInstanceFollowRedirects(false);
      setCommonHeaders(conn);
      setBasicAuthHeader(ctx, conn);
      setPostDataHeaders(conn);
      conn.connect();

      StringBuilder sb;
      if (!TextUtils.isEmpty(code)) {
        sb = new StringBuilder("grant_type=authorization_code&code=")
            .append(code)
            .append("&redirect_uri=")
            .append(Urls.OAUTH_REDIRECT_URL);
      } else {
        sb = new StringBuilder("grant_type=refresh_token&refresh_token=")
            .append(refreshToken);
      }
      writePostData(conn, sb);

      r = newJsonReader(conn.getInputStream());
      return AccessTokenResult.getAccessToken(r);
    } finally {
      close(r, conn);
    }
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

  private static void setBasicAuthHeader(Context ctx, HttpURLConnection conn) {
    try {
      String clientId = ctx.getString(R.string.key_reddit_client_id);
      String data = clientId + ":";
      String auth = Base64.encodeToString(data.getBytes(CHARSET),
          Base64.DEFAULT);
      conn.setRequestProperty("Authorization", "Basic " + auth);
    } catch (UnsupportedEncodingException e) {
      Log.wtf(TAG, e);
    }
  }

  private static void setOAuthHeader(
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

  private static void setPostDataHeaders(HttpURLConnection conn) {
    conn.setRequestProperty("Content-Type", CONTENT_TYPE);
    conn.setDoOutput(true);
  }

  private static Result post(
      Context ctx,
      String accountName,
      CharSequence url,
      CharSequence data)
      throws AuthenticatorException, OperationCanceledException, IOException {
    HttpURLConnection conn = null;
    InputStream is = null;
    try {
      conn = connect(ctx, accountName, url, true);
      writePostData(conn, data);
      is = conn.getInputStream();
      return Result.fromJson(logResponse(is));
    } finally {
      close(is, conn);
    }
  }

  private static void writePostData(HttpURLConnection conn, CharSequence data)
      throws IOException {
    OutputStream output = null;
    try {
      output = new BufferedOutputStream(conn.getOutputStream());
      output.write(data.toString().getBytes(CHARSET));
    } finally {
      close(output);
    }
  }

  protected static JsonReader newJsonReader(InputStream in) {
    return new JsonReader(new InputStreamReader(new BufferedInputStream(in)));
  }

  // TODO(btmura): make private
  static InputStream logResponse(InputStream is) throws IOException {
    if (!LOG_RESPONSES) {
      return is;
    }

    // Make a copy of the InputStream.
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    for (int read; (read = is.read(buffer)) != -1; ) {
      os.write(buffer, 0, read);
    }
    is.close();

    // Print out the response for debugging purposes.
    is = new ByteArrayInputStream(os.toByteArray());
    Scanner sc = new Scanner(is);
    while (sc.hasNextLine()) {
      Log.d(TAG, sc.nextLine());
    }
    sc.close();

    // Return a new InputStream as if nothing happened...
    return new BufferedInputStream(new ByteArrayInputStream(os.toByteArray()));
  }

  private static void close(
      @Nullable Closeable cs,
      @Nullable HttpURLConnection conn) {
    close(cs);
    if (conn != null) {
      conn.disconnect();
    }
  }

  private static void close(@Nullable Closeable cs) {
    if (cs != null) {
      try {
        cs.close();
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
      }
    }
  }

  private RedditApi() {
  }
}
