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

import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.util.ThingIds;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Urls {

    /** Type for getting a HTML response. */
    public static final int TYPE_HTML = 0;

    /** Type for getting a JSON response. */
    public static final int TYPE_JSON = 1;

    public static final String BASE_URL = "https://www.reddit.com";
    private static final String BASE_SSL_URL = "https://ssl.reddit.com";

    public static final String API_ACCESS_TOKEN_URL = BASE_SSL_URL + "/api/v1/access_token";

    private static final String API_COMMENTS_URL = BASE_URL + "/api/comment";
    private static final String API_COMPOSE_URL = BASE_URL + "/api/compose";
    private static final String API_DELETE_URL = BASE_URL + "/api/del";
    private static final String API_EDIT_URL = BASE_URL + "/api/editusertext";
    private static final String API_HIDE_URL = BASE_URL + "/api/hide";
    private static final String API_INFO_URL = BASE_URL + "/api/info";
    private static final String API_LOGIN_URL = BASE_SSL_URL + "/api/login/";
    private static final String API_ME_URL = BASE_URL + "/api/me";
    private static final String API_READ_MESSAGE = BASE_URL + "/api/read_message";
    private static final String API_SAVE_URL = BASE_URL + "/api/save";
    private static final String API_SUBMIT_URL = BASE_URL + "/api/submit/";
    private static final String API_SUBSCRIBE_URL = BASE_URL + "/api/subscribe/";
    private static final String API_UNHIDE_URL = BASE_URL + "/api/unhide";
    private static final String API_UNREAD_MESSAGE = BASE_URL + "/api/unread_message";
    private static final String API_UNSAVE_URL = BASE_URL + "/api/unsave";
    private static final String API_VOTE_URL = BASE_URL + "/api/vote/";

    private static final String BASE_CAPTCHA_URL = BASE_URL + "/captcha/";
    private static final String BASE_SEARCH_QUERY = "/search.json?q=";
    private static final String BASE_SEARCH_URL = BASE_URL + BASE_SEARCH_QUERY;
    private static final String BASE_SUBREDDIT_SEARCH_URL = BASE_URL + "/reddits/search.json?q=";
    private static final String BASE_SUBREDDIT_URL = BASE_URL + "/r/";
    private static final String BASE_USER_JSON_URL = BASE_URL + "/user/";

    public static CharSequence aboutMe() {
        return new StringBuilder(API_ME_URL).append(".json");
    }

    public static CharSequence aboutUser(String user) {
        return new StringBuilder(BASE_USER_JSON_URL).append(user).append("/about.json");
    }

    public static CharSequence captcha(String id) {
        return new StringBuilder(BASE_CAPTCHA_URL).append(id).append(".png");
    }

    public static CharSequence comments() {
        return API_COMMENTS_URL;
    }

    public static CharSequence commentsQuery(String thingId, String text, String modhash) {
        return thingTextQuery(thingId, text, modhash);
    }

    public static CharSequence edit() {
        return API_EDIT_URL;
    }

    public static CharSequence editQuery(String thingId, String text, String modhash) {
        return thingTextQuery(thingId, text, modhash);
    }

    private static CharSequence thingTextQuery(String thingId, String text, String modhash) {
        return new StringBuilder()
                .append("thing_id=").append(encode(thingId))
                .append("&text=").append(encode(text))
                .append("&uh=").append(encode(modhash))
                .append("&api_type=json");
    }

    public static CharSequence compose() {
        return API_COMPOSE_URL;
    }

    public static String composeQuery(String to, String subject, String text, String captchaId,
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

    public static CharSequence delete() {
        return API_DELETE_URL;
    }

    public static CharSequence deleteQuery(String thingId, String modhash) {
        return thingQuery(thingId, modhash);
    }

    public static CharSequence hide(boolean hide) {
        return hide ? API_HIDE_URL : API_UNHIDE_URL;
    }

    public static CharSequence hideQuery(String thingId, String modhash) {
        return thingQuery(thingId, modhash);
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

    public static CharSequence readMessage() {
        return API_READ_MESSAGE;
    }

    public static CharSequence readMessageQuery(String thingId, String modhash) {
        return thingQuery(thingId, modhash);
    }

    public static CharSequence saveQuery(String thingId, String modhash) {
        return thingQuery(thingId, modhash);
    }

    private static CharSequence thingQuery(String thingId, String modhash) {
        StringBuilder b = new StringBuilder();
        b.append("id=").append(encode(thingId));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b;
    }

    public static CharSequence subscribeQuery(String subreddit, boolean subscribe,
                                              String modhash) {
        StringBuilder b = new StringBuilder();
        b.append("action=").append(subscribe ? "sub" : "unsub");
        b.append("&uh=").append(encode(modhash));
        b.append("&sr_name=").append(encode(subreddit));
        b.append("&api_type=json");
        return b;
    }

    public static CharSequence perma(String permaLink, String thingId) {
        StringBuilder b = new StringBuilder(BASE_URL).append(permaLink);
        if (!TextUtils.isEmpty(thingId)) {
            b.append(ThingIds.removeTag(thingId));
        }
        return b;
    }

    public static CharSequence save(boolean save) {
        return save ? API_SAVE_URL : API_UNSAVE_URL;
    }

    public static CharSequence search(String subreddit, String query, int filter, String more) {
        if (!TextUtils.isEmpty(subreddit)) {
            StringBuilder b = new StringBuilder(BASE_SUBREDDIT_URL);
            b.append(encode(subreddit));
            b.append(BASE_SEARCH_QUERY);
            return newSearchUrl(b, query, filter, more, true);
        } else {
            return newSearchUrl(BASE_SEARCH_URL, query, filter, more, false);
        }
    }

    public static CharSequence sidebar(String name) {
        return new StringBuilder(BASE_SUBREDDIT_URL).append(encode(name)).append("/about.json");
    }

    public static CharSequence submit() {
        return API_SUBMIT_URL;
    }

    public static CharSequence submitQuery(String subreddit,
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

    public static CharSequence subreddit(String subreddit, int filter, int apiType) {
        return subredditMore(subreddit, filter, null, apiType);
    }

    public static CharSequence subredditMore(String subreddit,
                                             int filter,
                                             String more,
                                             int apiType) {
        StringBuilder b = new StringBuilder(BASE_URL);

        if (!Subreddits.isFrontPage(subreddit)) {
            b.append("/r/").append(encode(subreddit));
        }

        // Only add the filter for non random subreddits.
        if (!Subreddits.isRandom(subreddit)) {
            switch (filter) {
                case Filter.SUBREDDIT_CONTROVERSIAL:
                    b.append("/controversial");
                    break;

                case Filter.SUBREDDIT_HOT:
                    b.append("/hot");
                    break;

                case Filter.SUBREDDIT_NEW:
                    b.append("/new");
                    break;

                case Filter.SUBREDDIT_RISING:
                    b.append("/rising");
                    break;

                case Filter.SUBREDDIT_TOP:
                    b.append("/top");
                    break;
            }
        }

        if (apiType == TYPE_JSON) {
            b.append("/.json");
        }

        if (more != null) {
            b.append("?count=25&after=").append(encode(more));
        }
        return b;
    }

    public static CharSequence subredditSearch(String query, String more) {
        return newSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, -1, more, false);
    }

    public static CharSequence subscribe() {
        return API_SUBSCRIBE_URL;
    }

    public static CharSequence unreadMessage() {
        return API_UNREAD_MESSAGE;
    }

    public static CharSequence unreadMessageQuery(String thingId, String modhash) {
        return thingQuery(thingId, modhash);
    }

    public static CharSequence vote() {
        return API_VOTE_URL;
    }

    public static CharSequence voteQuery(String thingId, int vote, String modhash) {
        StringBuilder b = new StringBuilder();
        b.append("id=").append(thingId);
        b.append("&dir=").append(encode(Integer.toString(vote)));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b;
    }

    private static CharSequence newSearchUrl(CharSequence base, String query, int filter,
                                             String more, boolean restrict) {
        StringBuilder b = new StringBuilder(base).append(encode(query));
        switch (filter) {
            case Filter.SEARCH_RELEVANCE:
                b.append("&sort=relevance");
                break;

            case Filter.SEARCH_NEW:
                b.append("&sort=new");
                break;

            case Filter.SEARCH_HOT:
                b.append("&sort=hot");
                break;

            case Filter.SEARCH_TOP:
                b.append("&sort=top");
                break;

            case Filter.SEARCH_COMMENTS:
                b.append("&sort=comments");
                break;

            default:
                break;
        }
        if (more != null) {
            b.append("&count=25&after=").append(encode(more));
        }
        if (restrict) {
            b.append("&restrict_sr=on");
        }
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
