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
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.ThingIds;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class Urls2 {

  // TODO(btmura): make count a parameter

  public static final String OAUTH_REDIRECT_URL = "rbb://oauth/";

  private static final String NO_ACCOUNT = AccountUtils.NO_ACCOUNT;

  private static final int FORMAT_HTML = 0;
  private static final int FORMAT_JSON = 1;

  private static final String WWW_REDDIT_COM = "https://www.reddit.com";
  private static final String OAUTH_REDDIT_COM = "https://oauth.reddit.com";

  // Normal URLs

  private static final String CAPTCHA_URL = WWW_REDDIT_COM + "/captcha/";

  // OAuth URLs

  private static final String COMMENT_URL = OAUTH_REDDIT_COM + "/api/comment";
  private static final String COMPOSE_URL = OAUTH_REDDIT_COM + "/api/compose";
  private static final String DEL_URL = OAUTH_REDDIT_COM + "/api/del";
  private static final String EDIT_URL = OAUTH_REDDIT_COM + "/api/editusertext";
  private static final String HIDE_URL = OAUTH_REDDIT_COM + "/api/hide";
  private static final String ME_URL = OAUTH_REDDIT_COM + "/api/v1/me";
  private static final String MY_SUBREDDITS_URL =
      OAUTH_REDDIT_COM + "/subreddits/mine/subscriber?limit=1000";
  private static final String READ_MESSAGE =
      OAUTH_REDDIT_COM + "/api/read_message";
  private static final String SAVE_URL = OAUTH_REDDIT_COM + "/api/save";
  private static final String SUBMIT_URL = OAUTH_REDDIT_COM + "/api/submit/";
  private static final String SUBSCRIBE_URL =
      OAUTH_REDDIT_COM + "/api/subscribe/";
  private static final String UNHIDE_URL = OAUTH_REDDIT_COM + "/api/unhide";
  private static final String UNREAD_MESSAGE =
      OAUTH_REDDIT_COM + "/api/unread_message";
  private static final String UNSAVE_URL = OAUTH_REDDIT_COM + "/api/unsave";
  private static final String VOTE_URL = OAUTH_REDDIT_COM + "/api/vote/";

  private static final String AUTHORIZE_PATH = "/api/v1/authorize.compact";
  private static final String COMMENTS_PATH = "/comments/";
  private static final String INFO_PATH = "/api/info";
  private static final String MESSAGES_PATH = "/message";
  private static final String MESSAGE_THREAD_PATH = "/message/messages/";
  private static final String SUBREDDITS_PATH = "/subreddits";
  private static final String R_PATH = "/r/";
  private static final String U_PATH = "/u/";
  private static final String USER_PATH = "/user/";

  public static CharSequence authorize(Context ctx, CharSequence state) {
    String clientId = ctx.getString(R.string.key_reddit_client_id);
    return new StringBuilder(WWW_REDDIT_COM)
        .append(AUTHORIZE_PATH)
        .append("?client_id=").append(encode(clientId))
        .append("&response_type=code&state=").append(encode(state))
        .append("&redirect_uri=").append(encode(OAUTH_REDIRECT_URL))
        .append("&duration=permanent&scope=")
        .append(encode(
            "edit,history,identity,mysubreddits,privatemessages,read,report,submit,subscribe,vote"));
  }

  public static CharSequence myInfo() {
    return ME_URL;
  }

  public static CharSequence thingInfo(String accountName, String thingId) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(INFO_PATH);

    if (needsJsonExtension(accountName, FORMAT_JSON)) {
      sb.append(".json");
    }

    return sb.append("id=")
        .append(ThingIds.addTag(thingId, Kinds.getTag(Kinds.KIND_LINK)));
  }

  public static CharSequence mySubreddits() {
    return MY_SUBREDDITS_URL;
  }

  public static CharSequence subreddit(
      String accountName,
      String subreddit,
      int filter,
      @Nullable String more) {
    return innerSubreddit(accountName, subreddit, filter, more, FORMAT_JSON);
  }

  public static CharSequence subredditLink(String subreddit) {
    return innerSubreddit(NO_ACCOUNT, subreddit, -1, null, FORMAT_HTML);
  }

  private static CharSequence innerSubreddit(
      String accountName,
      String subreddit,
      int filter,
      @Nullable String more,
      int format) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName));

    if (!Subreddits.isFrontPage(subreddit)) {
      sb.append(R_PATH).append(encode(subreddit));
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

    if (needsJsonExtension(accountName, format)) {
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
      int numComments) {
    return innerComments(accountName, thingId, linkId, filter, numComments,
        FORMAT_JSON);
  }

  public static CharSequence commentsLink(String thingId, String linkId) {
    return innerComments(NO_ACCOUNT, thingId, linkId, -1, -1, FORMAT_HTML);
  }

  private static CharSequence innerComments(
      String accountName,
      String thingId,
      String linkId,
      int filter,
      int numComments,
      int format) {
    thingId = ThingIds.removeTag(thingId);

    boolean hasLinkId = !TextUtils.isEmpty(linkId);
    boolean hasLimit = numComments != -1;

    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(COMMENTS_PATH)
        .append(encode(hasLinkId ? ThingIds.removeTag(linkId) : thingId));

    if (needsJsonExtension(accountName, format)) {
      sb.append(".json");
    }

    if (hasLinkId || hasLimit || filter != -1) {
      sb.append("?");
    }

    if (hasLinkId) {
      sb.append("&comment=").append(encode(thingId)).append("&context=3");
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

  public static CharSequence messages(
      int filter,
      @Nullable String more,
      boolean mark) {
    StringBuilder sb = new StringBuilder(OAUTH_REDDIT_COM)
        .append(MESSAGES_PATH);

    switch (filter) {
      case Filter.MESSAGE_INBOX:
        sb.append("/inbox");
        break;

      case Filter.MESSAGE_UNREAD:
        sb.append("/unread");
        break;

      case Filter.MESSAGE_SENT:
        sb.append("/sent");
        break;

      default:
        throw new IllegalArgumentException();
    }
    if (more != null || mark) {
      sb.append("?");
    }
    if (more != null) {
      sb.append("&count=25&after=").append(encode(more));
    }
    if (mark) {
      sb.append("&mark=true");
    }
    return sb;
  }

  public static CharSequence profile(
      String accountName,
      String user,
      int filter,
      @Nullable String more) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(USER_PATH)
        .append(encode(user));
    switch (filter) {
      case Filter.PROFILE_OVERVIEW:
        sb.append("/overview");
        break;

      case Filter.PROFILE_COMMENTS:
        sb.append("/comments");
        break;

      case Filter.PROFILE_SUBMITTED:
        sb.append("/submitted");
        break;

      case Filter.PROFILE_UPVOTED:
        sb.append("/upvoted");
        break;

      case Filter.PROFILE_DOWNVOTED:
        sb.append("/downvoted");
        break;

      case Filter.PROFILE_HIDDEN:
        sb.append("/hidden");
        break;

      case Filter.PROFILE_SAVED:
        sb.append("/saved");
        break;
    }
    if (needsJsonExtension(accountName, FORMAT_JSON)) {
      sb.append(".json");
    }
    if (more != null) {
      sb.append("?count=25&after=").append(encode(more));
    }
    return sb;
  }

  public static CharSequence profileLink(String user) {
    return new StringBuilder(WWW_REDDIT_COM)
        .append(U_PATH)
        .append(encode(user));
  }

  public static CharSequence messageThread(String thingId) {
    return new StringBuilder(OAUTH_REDDIT_COM)
        .append(MESSAGE_THREAD_PATH)
        .append(encode(ThingIds.removeTag(thingId)));
  }

  public static CharSequence messageThreadLink(String thingId) {
    return new StringBuilder(WWW_REDDIT_COM)
        .append(MESSAGE_THREAD_PATH)
        .append(encode(ThingIds.removeTag(thingId)));
  }

  public static CharSequence search(
      String accountName,
      @Nullable String subreddit,
      String query,
      int filter,
      @Nullable String more) {
    return innerSearch(accountName, subreddit, false, query, filter, more);
  }

  public static CharSequence subredditSearch(
      String accountName,
      String query) {
    return innerSearch(accountName, null, true, query, -1, null);
  }

  private static CharSequence innerSearch(
      String accountName,
      @Nullable String subreddit,
      boolean subredditSearch,
      String query,
      int filter,
      @Nullable String more) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName));

    if (subredditSearch) {
      sb.append(SUBREDDITS_PATH);
    } else if (!TextUtils.isEmpty(subreddit)) {
      sb.append(R_PATH).append(encode(subreddit));
    }

    sb.append("/search");
    if (needsJsonExtension(accountName, FORMAT_JSON)) {
      sb.append(".json");
    }
    sb.append("?q=").append(encode(query));

    switch (filter) {
      case Filter.SEARCH_RELEVANCE:
        sb.append("&sort=relevance");
        break;

      case Filter.SEARCH_NEW:
        sb.append("&sort=new");
        break;

      case Filter.SEARCH_HOT:
        sb.append("&sort=hot");
        break;

      case Filter.SEARCH_TOP:
        sb.append("&sort=top");
        break;

      case Filter.SEARCH_COMMENTS:
        sb.append("&sort=comments");
        break;
    }
    if (more != null) {
      sb.append("&count=25&after=").append(encode(more));
    }
    if (!TextUtils.isEmpty(subreddit)) {
      sb.append("&restrict_sr=on");
    }
    return sb;
  }

  public static CharSequence sidebar(
      String accountName,
      String subreddit) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(R_PATH)
        .append(encode(subreddit))
        .append("/about");
    if (needsJsonExtension(accountName, FORMAT_JSON)) {
      sb.append(".json");
    }
    return sb;
  }

  public static CharSequence userInfo(String accountName, String user) {
    StringBuilder sb = new StringBuilder(getBaseUrl(accountName))
        .append(USER_PATH)
        .append(encode(user))
        .append("/about");
    if (needsJsonExtension(accountName, FORMAT_JSON)) {
      sb.append(".json");
    }
    return sb;
  }

  // POST requests

  public static CharSequence comment() {
    return COMMENT_URL;
  }

  public static CharSequence commentQuery(String thingId, String text) {
    return thingTextQuery(thingId, text);
  }

  public static CharSequence compose() {
    return COMPOSE_URL;
  }

  public static CharSequence composeQuery(
      String to,
      String subject,
      String text,
      String captchaId,
      String captchaGuess) {
    StringBuilder sb = new StringBuilder("api_type=json");
    if (!TextUtils.isEmpty(captchaGuess)) {
      sb.append("&captcha=").append(encode(captchaGuess));
    }
    if (!TextUtils.isEmpty(captchaId)) {
      sb.append("&iden=").append(encode(captchaId));
    }
    return sb
        .append("&subject=").append(encode(subject))
        .append("&text=").append(encode(text))
        .append("&to=").append(encode(to));
  }

  public static CharSequence delete() {
    return DEL_URL;
  }

  public static CharSequence deleteQuery(String thingId) {
    return thingQuery(thingId);
  }

  public static CharSequence edit() {
    return EDIT_URL;
  }

  public static CharSequence editQuery(String thingId, String text) {
    return thingTextQuery(thingId, text);
  }

  public static CharSequence hide(boolean hide) {
    return hide ? HIDE_URL : UNHIDE_URL;
  }

  public static CharSequence hideQuery(String thingId) {
    return thingQuery(thingId);
  }

  public static CharSequence readMessage(boolean read) {
    return read ? READ_MESSAGE : UNREAD_MESSAGE;
  }

  public static CharSequence readMessageQuery(String thingId) {
    return thingQuery(thingId);
  }

  public static CharSequence save(boolean save) {
    return save ? SAVE_URL : UNSAVE_URL;
  }

  public static CharSequence saveQuery(String thingId) {
    return thingQuery(thingId);
  }

  public static CharSequence submit() {
    return SUBMIT_URL;
  }

  public static CharSequence submitQuery(
      String subreddit,
      String title,
      String text,
      boolean link,
      String captchaId,
      String captchaGuess) {
    StringBuilder sb = new StringBuilder("api_type=json");
    if (!TextUtils.isEmpty(captchaGuess)) {
      sb.append("&captcha=").append(encode(captchaGuess));
    }
    if (!TextUtils.isEmpty(captchaId)) {
      sb.append("&iden=").append(encode(captchaId));
    }
    return sb.append("&kind=").append(link ? "link" : "self")
        .append("&sr=").append(encode(subreddit))
        .append(link ? "&url=" : "&text=").append(encode(text))
        .append("&title=").append(encode(title));
  }

  public static CharSequence subscribe() {
    return SUBSCRIBE_URL;
  }

  public static CharSequence subscribeData(
      String subreddit,
      boolean subscribe) {
    return new StringBuilder()
        .append("action=").append(subscribe ? "sub" : "unsub")
        .append("&sr_name=").append(encode(subreddit));
  }

  public static CharSequence vote() {
    return VOTE_URL;
  }

  public static CharSequence voteQuery(String thingId, int vote) {
    return new StringBuilder()
        .append("id=").append(thingId)
        .append("&dir=").append(Integer.toString(vote));
  }

  // Other links

  public static CharSequence captcha(String id) {
    return new StringBuilder(CAPTCHA_URL).append(id).append(".png");
  }

  public static CharSequence permaLink(String perma, String thingId) {
    StringBuilder sb = new StringBuilder(WWW_REDDIT_COM).append(perma);
    if (!TextUtils.isEmpty(thingId)) {
      sb.append(ThingIds.removeTag(thingId));
    }
    return sb;
  }

  private static String getBaseUrl(String accountName) {
    return isOAuth(accountName) ? OAUTH_REDDIT_COM : WWW_REDDIT_COM;
  }

  private static boolean needsJsonExtension(String accountName, int format) {
    return !isOAuth(accountName) && format == FORMAT_JSON;
  }

  private static boolean isOAuth(String accountName) {
    return AccountUtils.isAccount(accountName);
  }

  private static CharSequence thingQuery(String thingId) {
    return new StringBuilder("id=").append(encode(thingId));
  }

  private static CharSequence thingTextQuery(String thingId, String text) {
    return new StringBuilder()
        .append("thing_id=").append(encode(thingId))
        .append("&text=").append(encode(text));
  }


  public static URL newUrl(CharSequence url) {
    try {
      return new URL(url.toString());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static String encode(CharSequence param) {
    try {
      return URLEncoder.encode(param.toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
