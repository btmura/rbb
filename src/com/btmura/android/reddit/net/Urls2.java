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

import android.support.annotation.Nullable;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.database.Subreddits;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class Urls2 {

  public static final int TYPE_HTML = 0;
  public static final int TYPE_JSON = 1;

  private static final String BASE_URL = "https://www.reddit.com";
  private static final String BASE_OAUTH_URL = "https://oauth.reddit.com";

  public static CharSequence subreddit(
      String accountName,
      String subreddit,
      int filter,
      @Nullable String more,
      int apiType) {
    StringBuilder sb = new StringBuilder();

    if (!AccountUtils.isAccount(accountName)) {
      sb.append(BASE_URL);
    } else {
      sb.append(BASE_OAUTH_URL);
    }

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

    if (!AccountUtils.isAccount(accountName) && apiType == TYPE_JSON) {
      sb.append(".json");
    }

    if (more != null) {
      sb.append("?count=25&after=").append(encode(more));
    }

    return sb;
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
