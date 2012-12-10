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

// TODO: Fix this class to be thread safe.
public class Urls {

    public static final String BASE_URL = "http://www.reddit.com";
    public static final String BASE_SSL_URL = "https://ssl.reddit.com";

    private static final String API_COMMENTS_URL = BASE_URL + "/api/comment";
    private static final String API_DELETE_URL = BASE_URL + "/api/del";
    private static final String API_LOGIN_URL = BASE_SSL_URL + "/api/login/";
    private static final String API_NEW_CAPTCHA_URL = BASE_URL + "/api/new_captcha";
    private static final String API_SUBMIT_URL = BASE_URL + "/api/submit/";
    private static final String API_SUBSCRIBE_URL = BASE_URL + "/api/subscribe/";
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

    private static final StringBuilder S = new StringBuilder(BASE_URL.length() * 3);

    public static URL commentsApiUrl() {
        return newUrl(API_COMMENTS_URL);
    }

    public static String commentsApiQuery(String thingId, String text, String modhash) {
        StringBuilder b = resetBuilder();
        b.append("thing_id=").append(encode(thingId));
        b.append("&text=").append(encode(text));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b.toString();
    }

    public static URL captchaUrl(String id) {
        return newUrl(resetBuilder().append(BASE_CAPTCHA_URL).append(id).append(".png"));
    }

    public static URL commentsUrl(String id, String linkId, boolean json) {
        id = removeTag(id);
        StringBuilder b = resetBuilder().append(BASE_COMMENTS_URL);
        if (!TextUtils.isEmpty(linkId)) {
            b.append(removeTag(linkId));
        } else {
            b.append(id);
        }
        if (json) {
            b.append(".json");
        }
        if (!TextUtils.isEmpty(linkId)) {
            b.append("?comment=").append(id).append("&context=3");
        }
        return newUrl(b);
    }

    public static URL deleteApiUrl() {
        return newUrl(API_DELETE_URL);
    }

    public static String deleteApiQuery(String thingId, String modhash) {
        StringBuilder b = resetBuilder();
        b.append("id=").append(encode(thingId));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b.toString();
    }

    public static String loginCookie(String cookie) {
        StringBuilder b = resetBuilder();
        b.append("reddit_session=").append(encode(cookie));
        return b.toString();
    }

    public static URL loginUrl(String userName) {
        return newUrl(resetBuilder().append(API_LOGIN_URL).append(encode(userName)));
    }

    public static String loginQuery(String userName, String password) {
        StringBuilder b = resetBuilder();
        b.append("user=").append(encode(userName));
        b.append("&passwd=").append(encode(password));
        b.append("&api_type=json");
        return b.toString();
    }

    public static URL messageThreadUrl(String thingId) {
        return newUrl(resetBuilder().append(BASE_MESSAGE_THREAD_URL)
                .append(removeTag(thingId))
                .append(".json"));
    }

    public static URL messageUrl(int filter, String more) {
        StringBuilder b = resetBuilder().append(BASE_MESSAGE_URL);
        switch (filter) {
            case FilterAdapter.MESSAGE_INBOX:
                b.append("inbox");
                break;

            case FilterAdapter.MESSAGE_UNREAD:
                b.append("unread");
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
        return newUrl(b);
    }

    public static URL newCaptchaUrl() {
        return newUrl(API_NEW_CAPTCHA_URL);
    }

    public static String newCaptchaQuery() {
        StringBuilder b = resetBuilder();
        b.append("api_type=json");
        return b.toString();
    }

    public static String subscribeQuery(String modhash, String subreddit, boolean subscribe) {
        StringBuilder b = resetBuilder();
        b.append("action=").append(subscribe ? "sub" : "unsub");
        b.append("&uh=").append(encode(modhash));
        b.append("&sr_name=").append(encode(subreddit));
        b.append("&api_type=json");
        return b.toString();
    }

    public static URL permaUrl(String permaLink, String thingId) {
        StringBuilder b = resetBuilder().append(BASE_URL).append(permaLink);
        if (!TextUtils.isEmpty(thingId)) {
            b.append(removeTag(thingId));
        }
        return newUrl(b);
    }

    public static URL searchUrl(String query, String more) {
        return newSearchUrl(BASE_SEARCH_URL, query, more);
    }

    public static URL sidebarUrl(String name) {
        return newUrl(resetBuilder().append(BASE_SUBREDDIT_URL).append(name).append("/about.json"));
    }

    public static URL submitUrl() {
        return newUrl(API_SUBMIT_URL);
    }

    public static String submitQuery(String modhash, String subreddit, String title, String text,
            boolean link, String captchaId, String captchaGuess) {
        StringBuilder b = resetBuilder();
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
        return b.toString();
    }

    public static URL subredditUrl(String subreddit, int filter, String more) {
        StringBuilder b = resetBuilder().append(BASE_URL);

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
        return newUrl(b);
    }

    public static URL subredditListUrl(int limit) {
        StringBuilder b = resetBuilder().append(BASE_SUBREDDIT_LIST_URL);
        if (limit != -1) {
            b.append("?limit=").append(limit);
        }
        return newUrl(b);
    }

    public static URL subredditSearchUrl(String query, String more) {
        return newSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, more);
    }

    public static URL subscribeUrl() {
        return newUrl(API_SUBSCRIBE_URL);
    }

    public static URL userUrl(String user, int filter, String more) {
        StringBuilder b = resetBuilder().append(BASE_USER_URL).append(user);
        switch (filter) {
            case FilterAdapter.PROFILE_OVERVIEW:
                break;

            case FilterAdapter.PROFILE_COMMENTS:
                b.append("/comments");
                break;

            case FilterAdapter.PROFILE_SUBMITTED:
                b.append("/submitted");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }
        b.append("/.json");
        if (more != null) {
            b.append("?count=25&after=").append(encode(more));
        }
        return newUrl(b);
    }

    public static URL voteUrl() {
        return newUrl(API_VOTE_URL);
    }

    public static String voteQuery(String modhash, String id, int vote) {
        StringBuilder b = resetBuilder();
        b.append("id=").append(id);
        b.append("&dir=").append(encode(Integer.toString(vote)));
        b.append("&uh=").append(encode(modhash));
        b.append("&api_type=json");
        return b.toString();
    }

    private static URL newSearchUrl(String base, String query, String more) {
        StringBuilder b = resetBuilder().append(base).append(encode(query));
        if (more != null) {
            b.append("&count=25&after=").append(encode(more));
        }
        return newUrl(b);
    }

    private static URL newUrl(CharSequence url) {
        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static StringBuilder resetBuilder() {
        return S.delete(0, S.length());
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
