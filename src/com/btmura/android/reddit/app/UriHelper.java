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

package com.btmura.android.reddit.app;

import java.util.List;

import com.btmura.android.reddit.widget.ThingBundle;

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

class UriHelper {

    // URI constants and matcher for handling intents with data.
    private static final String AUTHORITY = "www.reddit.com";
    private static final String AUTHORITY2 = "reddit.com";
    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDIT = 1;
    private static final int MATCH_COMMENTS = 2;
    private static final int MATCH_USER = 3;
    static {
        // http://www.reddit.com/r/rbb
        MATCHER.addURI(AUTHORITY, "r/*", MATCH_SUBREDDIT);
        MATCHER.addURI(AUTHORITY2, "r/*", MATCH_SUBREDDIT);

        // http://www.reddit.com/r/rbb/comments/12zl0q/
        MATCHER.addURI(AUTHORITY, "r/*/comments/*", MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY2, "r/*/comments/*", MATCH_COMMENTS);

        // http://www.reddit.com/r/rbb/comments/12zl0q/test_1
        MATCHER.addURI(AUTHORITY, "r/*/comments/*/*", MATCH_COMMENTS);
        MATCHER.addURI(AUTHORITY2, "r/*/comments/*/*", MATCH_COMMENTS);

        // http://www.reddit.com/u/btmura
        MATCHER.addURI(AUTHORITY, "u/*", MATCH_USER);
        MATCHER.addURI(AUTHORITY2, "u/*", MATCH_USER);
    }

    public static String getSubreddit(Uri data) {
        switch (MATCHER.match(data)) {
            case MATCH_SUBREDDIT:
                return data.getLastPathSegment();

            case MATCH_COMMENTS:
                return data.getPathSegments().get(1);

            default:
                return null;
        }
    }

    public static Bundle getThingBundle(Uri data) {
        switch (MATCHER.match(data)) {
            case MATCH_COMMENTS:
                List<String> segments = data.getPathSegments();
                Bundle b = new Bundle(2);
                ThingBundle.putSubreddit(b, segments.get(1));
                ThingBundle.putThingId(b, segments.get(3));
                return b;

            default:
                return null;
        }
    }

    public static String getUser(Uri data) {
        switch (MATCHER.match(data)) {
            case MATCH_USER:
                return data.getLastPathSegment();

            default:
                return null;
        }
    }
}
