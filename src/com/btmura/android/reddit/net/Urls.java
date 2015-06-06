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

import android.text.TextUtils;

import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.util.ThingIds;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Urls {

  public static final String BASE_URL = "https://www.reddit.com";
  private static final String BASE_SSL_URL = "https://ssl.reddit.com";

  public static final String API_ACCESS_TOKEN_URL = BASE_SSL_URL + "/api/v1/access_token";

  private static final String API_COMPOSE_URL = BASE_URL + "/api/compose";
  private static final String API_EDIT_URL = BASE_URL + "/api/editusertext";
  private static final String API_INFO_URL = BASE_URL + "/api/info";
  private static final String API_SUBMIT_URL = BASE_URL + "/api/submit/";

  private static final String BASE_CAPTCHA_URL = BASE_URL + "/captcha/";

  public static CharSequence captcha(String id) {
    return new StringBuilder(BASE_CAPTCHA_URL).append(id).append(".png");
  }

  public static CharSequence edit() {
    return API_EDIT_URL;
  }

  public static CharSequence editQuery(
      String thingId,
      String text,
      String modhash) {
    return thingTextQuery(thingId, text, modhash);
  }

  private static CharSequence thingTextQuery(
      String thingId,
      String text,
      String modhash) {
    return new StringBuilder()
        .append("thing_id=").append(encode(thingId))
        .append("&text=").append(encode(text))
        .append("&uh=").append(encode(modhash))
        .append("&api_type=json");
  }

  public static CharSequence compose() {
    return API_COMPOSE_URL;
  }

  public static String composeQuery(
      String to, String subject, String text, String captchaId,
      String captchaGuess, String modhash) {
    StringBuilder b = new StringBuilder();
    b.append("to=").append(encode(to));
    b.append("&subject=").append(encode(subject));
    b.append("&text=").append(encode(text));
    if (!TextUtils.isEmpty(captchaId)) {
      b.append("&iden=").append(encode(captchaId));
    }
    if (!TextUtils.isEmpty(captchaGuess)) {
      b.append("&captcha=").append(encode(captchaGuess));
    }
    b.append("&uh=").append(encode(modhash));
    b.append("&api_type=json");
    return b.toString();
  }

  public static CharSequence info(String thingId) {
    return new StringBuilder(API_INFO_URL)
        .append(".json?id=")
        .append(ThingIds.addTag(thingId, Kinds.getTag(Kinds.KIND_LINK)));
  }

  public static CharSequence loginCookie(String cookie) {
    StringBuilder b = new StringBuilder();
    b.append("reddit_session=").append(encode(cookie));
    return b;
  }

  public static CharSequence perma(String permaLink, String thingId) {
    StringBuilder b = new StringBuilder(BASE_URL).append(permaLink);
    if (!TextUtils.isEmpty(thingId)) {
      b.append(ThingIds.removeTag(thingId));
    }
    return b;
  }

  public static CharSequence submit() {
    return API_SUBMIT_URL;
  }

  public static CharSequence submitQuery(
      String subreddit,
      String title,
      String text,
      boolean link,
      String captchaId,
      String captchaGuess,
      String modhash) {
    StringBuilder b = new StringBuilder();
    b.append(link ? "kind=link" : "kind=self");
    b.append("&uh=").append(encode(modhash));
    b.append("&sr=").append(encode(subreddit));
    b.append("&title=").append(encode(title));
    b.append(link ? "&url=" : "&text=").append(encode(text));
    if (!TextUtils.isEmpty(captchaId)) {
      b.append("&iden=").append(encode(captchaId));
    }
    if (!TextUtils.isEmpty(captchaGuess)) {
      b.append("&captcha=").append(encode(captchaGuess));
    }
    b.append("&api_type=json");
    return b;
  }

  public static String encode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
