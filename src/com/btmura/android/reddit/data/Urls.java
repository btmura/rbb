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

package com.btmura.android.reddit.data;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.widget.FilterAdapter;

public class Urls {

    public static final String BASE_URL = "http://www.reddit.com";
    public static final String BASE_SSL_URL = "https://ssl.reddit.com";

    private static final String API_COMMENTS_URL = BASE_URL + "/api/comment";
    private static final String BASE_CAPTCHA_URL = BASE_URL + "/captcha/";
    private static final String BASE_COMMENTS_URL = BASE_URL + "/comments/";
    private static final String BASE_LOGIN_URL = BASE_SSL_URL + "/api/login/";
    private static final String BASE_SEARCH_URL = BASE_URL + "/search.json?q=";
    private static final String BASE_SUBMIT_URL = BASE_URL + "/api/submit/";
    private static final String BASE_SUBREDDIT_LIST_URL = BASE_URL + "/reddits/mine/.json";
    private static final String BASE_SUBREDDIT_SEARCH_URL = BASE_URL + "/reddits/search.json?q=";
    private static final String BASE_SUBREDDIT_URL = BASE_URL + "/r/";
    private static final String BASE_SUBSCRIBE_URL = BASE_URL + "/api/subscribe/";
    private static final String BASE_VOTE_URL = BASE_URL + "/api/vote/";

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

    public static URL commentsUrl(String id) {
        return newUrl(resetBuilder().append(BASE_COMMENTS_URL).append(id).append(".json"));
    }

    public static String loginCookie(String cookie) {
        StringBuilder b = resetBuilder();
        b.append("reddit_session=").append(encode(cookie));
        return b.toString();
    }

    public static URL loginUrl(String userName) {
        return newUrl(resetBuilder().append(BASE_LOGIN_URL).append(encode(userName)));
    }

    public static String loginQuery(String userName, String password) {
        StringBuilder b = resetBuilder();
        b.append("user=").append(encode(userName));
        b.append("&passwd=").append(encode(password));
        b.append("&api_type=json");
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

    public static URL permaUrl(Thing thing) {
        return newUrl(resetBuilder().append(BASE_URL).append(thing.permaLink));
    }

    public static URL searchUrl(String query, String more) {
        return newSearchUrl(BASE_SEARCH_URL, query, more);
    }

    public static URL sidebarUrl(String name) {
        return newUrl(resetBuilder().append(BASE_SUBREDDIT_URL).append(name).append("/about.json"));
    }

    public static URL submitUrl() {
        return newUrl(BASE_SUBMIT_URL);
    }

    public static String submitTextQuery(String modhash, String subreddit, String title,
            String text, String captchaId, String captchaGuess) {
        StringBuilder b = resetBuilder();
        b.append("kind=self");
        b.append("&uh=").append(encode(modhash));
        b.append("&sr=").append(encode(subreddit));
        b.append("&title=").append(encode(title));
        b.append("&text=").append(encode(text));
        if (captchaId != null) {
            b.append("&iden=").append(encode(captchaId));
        }
        if (captchaGuess != null) {
            b.append("&captcha=").append(encode(captchaGuess));
        }
        b.append("&api_type=json");
        return b.toString();
    }

    public static URL subredditUrl(Subreddit sr, int filter, String more) {
        StringBuilder b = resetBuilder().append(BASE_URL);

        if (!sr.isFrontPage()) {
            b.append("/r/").append(encode(sr.name));
        }

        switch (filter) {
            case FilterAdapter.FILTER_HOT:
                break;

            case FilterAdapter.FILTER_NEW:
                b.append("/new");
                break;

            case FilterAdapter.FILTER_CONTROVERSIAL:
                b.append("/controversial");
                break;

            case FilterAdapter.FILTER_TOP:
                b.append("/top");
                break;

            default:
                throw new IllegalArgumentException(Integer.toString(filter));
        }

        b.append("/.json");

        boolean hasSort = filter == FilterAdapter.FILTER_NEW;
        if (hasSort) {
            b.append("?sort=new");
        }
        if (more != null) {
            b.append(hasSort ? "&" : "?").append("count=25&after=").append(encode(more));
        }
        return newUrl(b);
    }

    public static URL subredditListUrl() {
        return newUrl(BASE_SUBREDDIT_LIST_URL);
    }

    public static URL subredditSearchUrl(String query, String more) {
        return newSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, more);
    }

    public static URL subscribeUrl() {
        return newUrl(BASE_SUBSCRIBE_URL);
    }

    public static URL voteUrl() {
        return newUrl(BASE_VOTE_URL);
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
}
