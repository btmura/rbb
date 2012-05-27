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

    private static final String BASE_URL = "http://www.reddit.com";
    private static final String BASE_SSL_URL = "https://ssl.reddit.com";

    private static final String BASE_COMMENTS_URL = BASE_URL + "/comments/";
    private static final String BASE_LOGIN_URL = BASE_SSL_URL + "/api/login/";
    private static final String BASE_SEARCH_URL = BASE_URL + "/search.json?q=";
    private static final String BASE_SUBREDDIT_URL = BASE_URL + "/r/";
    private static final String BASE_SUBREDDIT_SEARCH_URL = BASE_URL + "/reddits/search.json?q=";

    private static final StringBuilder S = new StringBuilder(BASE_URL.length() * 3);

    public static URL commentsUrl(String id) {
        return newUrl(resetBuilder().append(BASE_COMMENTS_URL).append(id).append(".json"));
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

    public static URL permaUrl(Thing thing) {
        return newUrl(resetBuilder().append(BASE_URL).append(thing.permaLink));
    }

    public static URL sidebarUrl(String name) {
        return newUrl(resetBuilder().append(BASE_SUBREDDIT_URL).append(name).append("/about.json"));
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

    public static URL searchUrl(String query, String more) {
        return newSearchUrl(BASE_SEARCH_URL, query, more);
    }

    public static URL subredditSearchUrl(String query, String more) {
        return newSearchUrl(BASE_SUBREDDIT_SEARCH_URL, query, more);
    }

    private static URL newSearchUrl(String base, String query, String more) {
        StringBuilder b = resetBuilder().append(base).append(encode(query));
        if (more != null) {
            b.append("&count=25&after=").append(encode(more));
        }
        return newUrl(b);
    }

    private static URL newUrl(StringBuilder builder) {
        try {
            return new URL(builder.toString());
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
