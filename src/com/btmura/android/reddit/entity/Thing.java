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

package com.btmura.android.reddit.entity;

import android.os.Bundle;
import android.text.TextUtils;

import com.btmura.android.reddit.database.Things;

public class Thing {

    public static String getId(String name) {
        int sepIndex = name.indexOf('_');
        return name.substring(sepIndex + 1);
    }

    public static String getSubreddit(Bundle thingBundle) {
        if (thingBundle != null) {
            return thingBundle.getString(Things.COLUMN_SUBREDDIT);
        } else {
            return null;
        }
    }

    public static String getDomain(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_DOMAIN);
    }

    public static int getLikes(Bundle thingBundle) {
        return thingBundle.getInt(Things.COLUMN_LIKES);
    }

    public static String getPermaLink(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_PERMA_LINK);
    }

    public static int getScore(Bundle thingBundle) {
        return thingBundle.getInt(Things.COLUMN_SCORE);
    }

    public static boolean isSelf(Bundle thingBundle) {
        return thingBundle.getBoolean(Things.COLUMN_SELF);
    }

    public static String getUrl(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_URL);
    }

    public static CharSequence getTitle(Bundle thingBundle) {
        return thingBundle.getCharSequence(Things.COLUMN_TITLE);
    }

    public static String getThumbnail(Bundle thingBundle) {
        return thingBundle.getString(Things.COLUMN_THUMBNAIL_URL);
    }

    public static boolean hasThumbnail(Bundle thingBundle) {
        return !TextUtils.isEmpty(getThumbnail(thingBundle));
    }
}
