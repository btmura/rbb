/*
 * Copyright (C) 2013 Brian Muramatsu
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

import java.util.List;

import android.content.UriMatcher;
import android.net.Uri;
import android.text.TextUtils;

public class YouTubeUrls {

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_YOUTUBE = 1;
    private static final int MATCH_YOUTU_BE = 2;

    static {
        MATCHER.addURI("youtube.com", "watch", MATCH_YOUTUBE);
        MATCHER.addURI("www.youtube.com", "watch", MATCH_YOUTUBE);
        MATCHER.addURI("youtu.be", "*", MATCH_YOUTU_BE);
        MATCHER.addURI("www.youtu.be", "*", MATCH_YOUTU_BE);
    }

    public static boolean isYouTubeVideoUrl(String url) {
        return !TextUtils.isEmpty(getVideoId(url));
    }

    public static String getVideoId(String url) {
        Uri uri = Uri.parse(url);
        switch (MATCHER.match(uri)) {
            case MATCH_YOUTUBE:
                return uri.getQueryParameter("v");

            case MATCH_YOUTU_BE:
                List<String> segments = uri.getPathSegments();
                return segments.size() == 1 ? segments.get(0) : null;

            default:
                return null;
        }
    }
}
