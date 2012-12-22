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

package com.btmura.android.reddit.widget;

import com.btmura.android.reddit.util.BundleSupport;

import android.os.Bundle;
import android.text.TextUtils;

/**
 * {@link ThingBundle} describes a {@link Bundle} that defines a common set of
 * keys for all thing types that should be used throughout the app for setting
 * the title and copying urls.
 */
public class ThingBundle extends BundleSupport {

    /** String subreddit that this thing belongs to. */
    private static final String KEY_SUBREDDIT = "subreddit";

    /** Integer indicating what kind this thing is. */
    private static final String KEY_KIND = "kind";

    /** String for the title of this thing. */
    private static final String KEY_TITLE = "title";

    /** String for the thing id of this thing. */
    private static final String KEY_THING_ID = "thingId";

    /** String that is the parent thing id when this is a comment. */
    private static final String KEY_LINK_ID = "linkId";

    /** String for the link url of this thing. Null when just comments. */
    private static final String KEY_LINK_URL = "linkUrl";

    /** String for the comment url of this thing. */
    private static final String KEY_COMMENT_URL = "commentUrl";

    /** Boolean indicating whether this thing has comments at all. */
    private static final String KEY_NO_COMMENTS = "hasComments";

    public static String getSubreddit(Bundle bundle) {
        return getString(bundle, KEY_SUBREDDIT);
    }

    public static void putSubreddit(Bundle bundle, String subreddit) {
        bundle.putString(KEY_SUBREDDIT, subreddit);
    }

    public static int getKind(Bundle bundle) {
        return getInt(bundle, KEY_KIND);
    }

    public static void putKind(Bundle bundle, int kind) {
        bundle.putInt(KEY_KIND, kind);
    }

    public static String getTitle(Bundle bundle) {
        return getString(bundle, KEY_TITLE);
    }

    public static void putTitle(Bundle bundle, String title) {
        bundle.putString(KEY_TITLE, title);
    }

    public static String getThingId(Bundle bundle) {
        return getString(bundle, KEY_THING_ID);
    }

    public static void putThingId(Bundle bundle, String thingId) {
        bundle.putString(KEY_THING_ID, thingId);
    }

    public static String getLinkId(Bundle bundle) {
        return getString(bundle, KEY_LINK_ID);
    }

    public static void putLinkId(Bundle bundle, String linkId) {
        bundle.putString(KEY_LINK_ID, linkId);
    }

    public static boolean hasLinkUrl(Bundle bundle) {
        return !TextUtils.isEmpty(getLinkUrl(bundle));
    }

    public static CharSequence getLinkUrl(Bundle bundle) {
        return getCharSequence(bundle, KEY_LINK_URL);
    }

    public static void putLinkUrl(Bundle bundle, CharSequence url) {
        bundle.putCharSequence(KEY_LINK_URL, url);
    }

    public static boolean hasCommentUrl(Bundle bundle) {
        return !TextUtils.isEmpty(getCommentUrl(bundle));
    }

    public static CharSequence getCommentUrl(Bundle bundle) {
        return getCharSequence(bundle, KEY_COMMENT_URL);
    }

    public static void putCommentUrl(Bundle bundle, CharSequence url) {
        bundle.putCharSequence(KEY_COMMENT_URL, url);
    }

    public static boolean hasNoComments(Bundle bundle) {
        return getBoolean(bundle, KEY_NO_COMMENTS);
    }

    public static void putNoComments(Bundle bundle, boolean hasComments) {
        bundle.putBoolean(KEY_NO_COMMENTS, hasComments);
    }

    private ThingBundle() {
    }
}
