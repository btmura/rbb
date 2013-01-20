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

import com.btmura.android.reddit.widget.FilterAdapter;
import com.btmura.android.reddit.widget.ThingBundle;

import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

class UriHelper {

    private static final String[] AUTHORITIES = {
            "www.reddit.com",
            "reddit.com",
    };

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_SUBREDDIT = 1;
    private static final int MATCH_SUBREDDIT_HOT = 2;
    private static final int MATCH_SUBREDDIT_NEW = 3;
    private static final int MATCH_SUBREDDIT_CONTROVERSIAL = 4;
    private static final int MATCH_SUBREDDIT_TOP = 5;
    private static final int MATCH_COMMENTS = 6;
    private static final int MATCH_USER = 7;
    private static final int MATCH_USER_OVERVIEW = 8;
    private static final int MATCH_USER_COMMENTS = 9;
    private static final int MATCH_USER_SUBMITTED = 10;
    private static final int MATCH_USER_SAVED = 11;

    static {
        for (int i = 0; i < AUTHORITIES.length; i++) {
            // http://www.reddit.com/r/rbb
            MATCHER.addURI(AUTHORITIES[i], "r/*", MATCH_SUBREDDIT);

            // Various filters of subreddits.
            MATCHER.addURI(AUTHORITIES[i], "r/*/hot", MATCH_SUBREDDIT_HOT);
            MATCHER.addURI(AUTHORITIES[i], "r/*/new", MATCH_SUBREDDIT_NEW);
            MATCHER.addURI(AUTHORITIES[i], "r/*/controversial", MATCH_SUBREDDIT_CONTROVERSIAL);
            MATCHER.addURI(AUTHORITIES[i], "r/*/top", MATCH_SUBREDDIT_TOP);

            // http://www.reddit.com/r/rbb/comments/12zl0q/
            MATCHER.addURI(AUTHORITIES[i], "r/*/comments/*", MATCH_COMMENTS);

            // http://www.reddit.com/r/rbb/comments/12zl0q/test_1
            MATCHER.addURI(AUTHORITIES[i], "r/*/comments/*/*", MATCH_COMMENTS);

            // http://www.reddit.com/u/btmura
            MATCHER.addURI(AUTHORITIES[i], "u/*", MATCH_USER);

            // Various filters of users.
            MATCHER.addURI(AUTHORITIES[i], "u/*/overview", MATCH_USER_OVERVIEW);
            MATCHER.addURI(AUTHORITIES[i], "u/*/comments", MATCH_USER_COMMENTS);
            MATCHER.addURI(AUTHORITIES[i], "u/*/submitted", MATCH_USER_SUBMITTED);
            MATCHER.addURI(AUTHORITIES[i], "u/*/saved", MATCH_USER_SAVED);
        }
    }

    public static String getSubreddit(Uri data) {
        switch (MATCHER.match(data)) {
            case MATCH_SUBREDDIT:
            case MATCH_SUBREDDIT_HOT:
            case MATCH_SUBREDDIT_NEW:
            case MATCH_SUBREDDIT_CONTROVERSIAL:
            case MATCH_SUBREDDIT_TOP:
            case MATCH_COMMENTS:
                return data.getPathSegments().get(1);

            default:
                return null;
        }
    }

    public static int getSubredditFilter(Uri data) {
        switch (MATCHER.match(data)) {
            case MATCH_SUBREDDIT_HOT:
                return FilterAdapter.SUBREDDIT_HOT;

            case MATCH_SUBREDDIT_NEW:
                return FilterAdapter.SUBREDDIT_NEW;

            case MATCH_SUBREDDIT_CONTROVERSIAL:
                return FilterAdapter.SUBREDDIT_CONTROVERSIAL;

            case MATCH_SUBREDDIT_TOP:
                return FilterAdapter.SUBREDDIT_TOP;

            default:
                return -1;
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
        if (data != null) {
            switch (MATCHER.match(data)) {
                case MATCH_USER:
                case MATCH_USER_OVERVIEW:
                case MATCH_USER_COMMENTS:
                case MATCH_USER_SUBMITTED:
                case MATCH_USER_SAVED:
                    return data.getPathSegments().get(1);
            }
        }
        return null;
    }

    public static int getUserFilter(Uri data) {
        if (data != null) {
            switch (MATCHER.match(data)) {
                case MATCH_USER_OVERVIEW:
                    return FilterAdapter.PROFILE_OVERVIEW;

                case MATCH_USER_COMMENTS:
                    return FilterAdapter.PROFILE_COMMENTS;

                case MATCH_USER_SUBMITTED:
                    return FilterAdapter.PROFILE_SUBMITTED;

                case MATCH_USER_SAVED:
                    return FilterAdapter.PROFILE_SAVED;
            }
        }
        return -1;
    }
}
