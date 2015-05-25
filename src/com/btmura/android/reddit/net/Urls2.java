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

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.ThingIds;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class Urls2 {

  public static final String OAUTH_REDIRECT_URL = "rbb://oauth/";

  public static final int TYPE_HTML = 0;
  public static final int TYPE_JSON = 1;

  private static final String BASE_URL = "https://www.reddit.com";
  private static final String BASE_OAUTH_URL = "https://oauth.reddit.com";

  private static final String AUTHORIZE_PATH = "/api/v1/authorize";
  private static final String COMMENTS_PATH = "/comments/";
  private static final String MY_SUBREDDITS_PATH =
      "/subreddits/mine/subscriber";

  public static CharSequence authorize(Context ctx, CharSequence state) {
    String clientId = ctx.getString(R.string.key_reddit_client_id);
    return new StringBuilder(BASE_URL)
        .append(AUTHORIZE_PATH)
        .append("?client_id=").append(clientId)
        .append("&response_type=code&state=").append(state)
        .append("&redirect_uri=").append(OAUTH_REDIRECT_URL)
        .append("&duration=permanent&scope=")
        .append(encode("mysubreddits,read"));
  }

  public static CharSequence mySubreddits() {
    return new StringBuilder(BASE_OAUTH_URL)
        .append(MY_SUBREDDITS_PATH)
        .append("?limit=").append(100);
  }

  // TODO(btmura): rename to subredditThings
  public static CharSequence subreddit(
      String accountName,
      String subreddit,
      int filter,
      @Nullable String more,
      int apiType) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName));

    if (!Subreddits.isFrontPage(subreddit)) {
      sb.append("/r/").append(encode(subreddit));
    }

    if (!Subreddits.isRandom(subreddit)) {
      switch (filter) {
        case Filter.SUBREDDIT_CONTROVERSIAL:
          sb.append("/controversial");
          break;

        case Filter.SUBREDDIT_HOT:
          sb.append("/hot");
          break;

        case Filter.SUBREDDIT_NEW:
          sb.append("/new");
          break;

        case Filter.SUBREDDIT_RISING:
          sb.append("/rising");
          break;

        case Filter.SUBREDDIT_TOP:
          sb.append("/top");
          break;
      }
    }

    if (needsJsonExtension(accountName, apiType)) {
      sb.append(".json");
    }

    if (more != null) {
      sb.append("?count=25&after=").append(encode(more));
    }

    return sb;
  }

  public static CharSequence comments(
      String accountName,
      String thingId,
      String linkId,
      int filter,
      int numComments,
      int apiType) {
    thingId = ThingIds.removeTag(thingId);

    boolean hasLinkId = !TextUtils.isEmpty(linkId);
    boolean hasLimit = numComments != -1;

    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(COMMENTS_PATH)
        .append(hasLinkId ? ThingIds.removeTag(linkId) : thingId);

    if (needsJsonExtension(accountName, apiType)) {
      sb.append(".json");
    }

    if (hasLinkId || hasLimit || filter != -1) {
      sb.append("?");
    }

    if (hasLinkId) {
      sb.append("&comment=").append(thingId).append("&context=3");
    } else if (filter != -1) {
      switch (filter) {
        case Filter.COMMENTS_BEST:
          sb.append("&sort=confidence");
          break;

        case Filter.COMMENTS_CONTROVERSIAL:
          sb.append("&sort=controversial");
          break;

        case Filter.COMMENTS_HOT:
          sb.append("&sort=hot");
          break;

        case Filter.COMMENTS_NEW:
          sb.append("&sort=new");
          break;

        case Filter.COMMENTS_OLD:
          sb.append("&sort=old");
          break;

        case Filter.COMMENTS_TOP:
          sb.append("&sort=top");
          break;

        default:
          break;
      }
    }
    if (hasLimit) {
      sb.append("&limit=").append(numComments);
    }
    return sb;
  }

  private static String getBaseUrl(String accountName) {
    return isOAuth(accountName) ? BASE_OAUTH_URL : BASE_URL;
  }

  private static boolean needsJsonExtension(String accountName, int apiType) {
    return !isOAuth(accountName) && apiType == TYPE_JSON;
  }

  private static boolean isOAuth(String accountName) {
    return AccountUtils.isAccount(accountName);
  }

  public static URL newUrl(CharSequence url) {
    try {
      return new URL(url.toString());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static String encode(String param) {
    try {
      return URLEncoder.encode(param, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
