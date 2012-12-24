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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.text.TextUtils;

import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.widget.FilterAdapter;

public class Urls {

    /** Type for getting a HTML response. */
    public static final int TYPE_HTML = 0;

    /** Type for getting a JSON response. */
    public static final int TYPE_JSON = 1;

    public static final String BASE_URL = "http://www.reddit.com";
    public static final String BASE_SSL_URL = "https://ssl.reddit.com";

    private static final String API_COMMENTS_URL = BASE_URL + "/api/comment";
    private static final String API_COMPOSE_URL = BASE_URL + "/api/compose";
    private static final String API_DELETE_URL = BASE_URL + "/api/del";
    private static final String API_LOGIN_URL = BASE_SSL_URL + "/api/login/";
    private static final String API_ME_URL = BASE_URL + "/api/me";
    private static final String API_NEW_CAPTCHA_URL = BASE_URL + "/api/new_captcha";
    private static final String API_SAVE_URL = BASE_URL + "/api/save";
    private static final String API_SUBMIT_URL = BASE_URL + "/api/submit/";
    private static final String API_SUBSCRIBE_URL = BASE_URL + "/api/subscribe/";
    private static final String API_UNSAVE_URL = BASE_URL + "/api/unsave";
    private static final String API_VOTE_URL = BASE_URL + "/api/vote/";

    private static final String BASE_CAPTCHA_URL = BASE_URL + "/captcha/";
    private static final String BASE_COMMENTS_URL = BASE_URL + "/comments/";
    private static final String BASE_MESSAGE_URL = BASE_URL + "/message/";
    private static final String BASE_MESSAGE_THREAD_URL = BASE_URL + "/message/messages/";
    private static final String BASE_SEARCH_URL = BASE_URL + "/search.json?q=";
    private static final String BASE_SUBREDDIT_LIST_URL = BASE_URL + "/reddits/mine/.json";
    private static final String BASE_SUBREDDIT_SEARCH_URL = BASE_URL + "/reddits/search.json?q=";
    private static final String BASE_SUBREDDIT_URL = BASE_URL + "/r/";
    private static final String BASE_USER_URL = BASE_URL + "/user/";

    public static URL newUrl(CharSequence url) {
        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static CharSequence comments() {
        return API_COMMENTS_URL;
    }

    public static String commentsQuery(String thingId, String text, String modhash) {
        StringBuilder b = new StringBuilder();
        b.append("thing_id=").append(encode(thingId));
        b.append("&text=").append(encode(text));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b.toString();
    }

    public static CharSequence captcha(String id) {
        return new StringBuilder(BASE_CAPTCHA_URL).append(id).append(".png");
    }

    public static CharSequence commentListing(String id, String linkId, int apiType) {
        id = removeTag(id);
        StringBuilder b = new StringBuilder(BASE_COMMENTS_URL);
        if (!TextUtils.isEmpty(linkId)) {
            b.append(removeTag(linkId));
        } else {
            b.append(id);
        }
        if (apiType == TYPE_JSON) {
            b.append(".json");
        }
        if (!TextUtils.isEmpty(linkId)) {
            b.append("?comment=").append(id).append("&context=3");
        }
        return b;
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
        StringBuilder b = new StringBuilder();
        b.append("id=").append(encode(thingId));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b;
    }

    public static CharSequence loginCookie(String cookie) {
        StringBuilder b = new StringBuilder();
        b.append("reddit_session=").append(encode(cookie));
        return b;
    }

    public static CharSequence login(String userName) {
        return new StringBuilder(API_LOGIN_URL).append(encode(userName));
    }

    public static CharSequence loginQuery(String userName, String password) {
        StringBuilder b = new StringBuilder();
        b.append("user=").append(encode(userName));
        b.append("&passwd=").append(encode(password));
        b.append("&api_type=json");
        return b;
    }

    public static CharSequence me() {
        return new StringBuilder(API_ME_URL).append(".json");
    }

    public static CharSequence messageThread(String thingId, int apiType) {
        StringBuilder b = new StringBuilder(BASE_MESSAGE_THREAD_URL);
        b.append(removeTag(thingId));
        if (apiType == TYPE_JSON) {
            b.append(".json");
        }
        return b;
    }

    public static CharSequence message(int filter, String more) {
        StringBuilder b = new StringBuilder(BASE_MESSAGE_URL);
        switch (filter) {
            case FilterAdapter.MESSAGE_INBOX:
                b.append("inbox");
                break;

            case FilterAdapter.MESSAGE_SENT:
                b.append("sent");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }
        b.append("/.json");
        if (more != null) {
            b.append("?count=25&after=").append(encode(more));
        }
        return b;
    }

    public static CharSequence newCaptcha() {
        return API_NEW_CAPTCHA_URL;
    }

    public static CharSequence newCaptchaQuery() {
        return "api_type=json";
    }

    public static CharSequence saveQuery(String thingId, String modhash) {
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
            b.append(removeTag(thingId));
        }
        return b;
    }

    public static CharSequence save(boolean save) {
        return save ? API_SAVE_URL : API_UNSAVE_URL;
    }

    public static CharSequence search(String query, String more) {
        return newSearchUrl(BASE_SEARCH_URL, query, more);
    }

    public static CharSequence sidebar(String name) {
        return new StringBuilder(BASE_SUBREDDIT_URL).append(name).append("/about.json");
    }

    public static CharSequence submit() {
        return API_SUBMIT_URL;
    }

    public static CharSequence submitQuery(String subreddit, String title, String text,
            boolean link, String captchaId, String captchaGuess, String modhash) {
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

    public static CharSequence subreddit(String subreddit, int filter, String more) {
        StringBuilder b = new StringBuilder(BASE_URL);

        // Trim off /r/ if it is there. Only when random provides us with the
        // redirect subreddit location. Otherwise, this prefix is not attached.
        if (subreddit.startsWith("/r/")) {
            subreddit = subreddit.substring(3);
        }

        if (!Subreddits.isFrontPage(subreddit)) {
            b.append("/r/").append(encode(subreddit));
        }

        switch (filter) {
            case FilterAdapter.SUBREDDIT_HOT:
                break;

            case FilterAdapter.SUBREDDIT_NEW:
                b.append("/new");
                break;

            case FilterAdapter.SUBREDDIT_CONTROVERSIAL:
                b.append("/controversial");
                break;

            case FilterAdapter.SUBREDDIT_TOP:
                b.append("/top");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }

        b.append("/.json");

        boolean hasSort = filter == FilterAdapter.SUBREDDIT_NEW;
        if (hasSort) {
            b.append("?sort=new");
        }
        if (more != null) {
            b.append(hasSort ? "&" : "?").append("count=25&after=").append(encode(more));
        }
        return b;
    }

    public static CharSequence subredditList(int limit) {
        StringBuilder b = new StringBuilder(BASE_SUBREDDIT_LIST_URL);
        if (limit != -1) {
            b.append("?limit=").append(limit);
        }
        return b;
    }

    public static CharSequence subredditSearch(String query, String more) {
        return newSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, more);
    }

    public static CharSequence subscribe() {
        return API_SUBSCRIBE_URL;
    }

    public static CharSequence user(String user, int filter, String more) {
        StringBuilder b = new StringBuilder(BASE_USER_URL).append(user);
        switch (filter) {
            case FilterAdapter.PROFILE_OVERVIEW:
                break;

            case FilterAdapter.PROFILE_COMMENTS:
                b.append("/comments");
                break;

            case FilterAdapter.PROFILE_SUBMITTED:
                b.append("/submitted");
                break;

            case FilterAdapter.PROFILE_SAVED:
                b.append("/saved");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }
        b.append("/.json");
        if (more != null) {
            b.append("?count=25&after=").append(encode(more));
        }
        return b;
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

    private static CharSequence newSearchUrl(String base, String query, String more) {
        StringBuilder b = new StringBuilder(base).append(encode(query));
        if (more != null) {
            b.append("&count=25&after=").append(encode(more));
        }
        return b;
    }

    private static String encode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String removeTag(String id) {
        int sepIndex = id.indexOf('_');
        if (sepIndex != -1) {
            return id.substring(sepIndex + 1);
        }
        return id;
    }
}
