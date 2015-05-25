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

import com.btmura.android.reddit.app.Filter;
import com.btmura.android.reddit.database.Subreddits;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Static methods that create Reddit API URLs. */
public class ApiUrls {

    private static final String BASE_URL = "https://oauth.reddit.com";

    public static CharSequence subreddit(String subreddit, int filter, String more) {
        StringBuilder b = new StringBuilder(BASE_URL);

        if (!Subreddits.isFrontPage(subreddit)) {
            b.append("/r/").append(encode(subreddit));
        }

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

        if (more != null) {
            b.append("?count=25&after=").append(encode(more));
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
