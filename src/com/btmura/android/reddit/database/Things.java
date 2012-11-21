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

package com.btmura.android.reddit.database;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class Things implements BaseColumns {
    public static final String TABLE_NAME = "things";

    /** Account for joining with the votes table. */
    public static final String COLUMN_ACCOUNT = Votes.COLUMN_ACCOUNT;

    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_CREATED_UTC = "createdUtc";
    public static final String COLUMN_DOMAIN = "domain";
    public static final String COLUMN_DOWNS = "downs";
    public static final String COLUMN_KIND = "kind";
    public static final String COLUMN_LIKES = "likes";
    public static final String COLUMN_LINK_ID = "linkId";
    public static final String COLUMN_LINK_TITLE = "linkTitle";
    public static final String COLUMN_NUM_COMMENTS = "numComments";
    public static final String COLUMN_OVER_18 = "over18";
    public static final String COLUMN_PERMA_LINK = "permaLink";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_SESSION_ID = "sessionId";
    public static final String COLUMN_SESSION_TIMESTAMP = "sessionTimestamp";
    public static final String COLUMN_SELF = "self";
    public static final String COLUMN_SUBREDDIT = "subreddit";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_THING_ID = Votes.COLUMN_THING_ID;
    public static final String COLUMN_THUMBNAIL_URL = "thumbnailUrl";
    public static final String COLUMN_UPS = "ups";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_VOTE = Votes.COLUMN_VOTE;

    public static final int KIND_MORE = 0;
    public static final int KIND_COMMENT = 1;
    public static final int KIND_ACCOUNT = 2;
    public static final int KIND_LINK = 3;
    public static final int KIND_MESSAGE = 4;
    public static final int KIND_SUBREDDIT = 5;

    public static final String SELECT_BY_SESSION_ID = COLUMN_SESSION_ID + " = ?";

    // TODO: Do we need an index for sessionId and more?
    public static final String SELECT_BY_SESSION_ID_AND_MORE =
            SELECT_BY_SESSION_ID + " AND " + COLUMN_KIND + " = " + KIND_MORE;

    public static final String SELECT_BEFORE_TIMESTAMP =
            COLUMN_SESSION_TIMESTAMP + " < ?";

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COLUMN_ACCOUNT + " TEXT NOT NULL, "
                + COLUMN_AUTHOR + " TEXT, "
                + COLUMN_BODY + " TEXT, "
                + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0, "
                + COLUMN_DOMAIN + " TEXT, "
                + COLUMN_DOWNS + " INTEGER DEFAULT 0, "
                + COLUMN_KIND + " INTEGER NOT NULL, "
                + COLUMN_LIKES + " INTEGER DEFAULT 0, "
                + COLUMN_LINK_ID + " TEXT, "
                + COLUMN_LINK_TITLE + " TEXT, "
                + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0, "
                + COLUMN_OVER_18 + " INTEGER DEFAULT 0, "
                + COLUMN_PERMA_LINK + " TEXT, "
                + COLUMN_SCORE + " INTEGER DEFAULT 0, "
                + COLUMN_SELF + " INTEGER DEFAULT 0, "
                + COLUMN_SESSION_TIMESTAMP + " INTEGER NOT NULL, "
                + COLUMN_SESSION_ID + " TEXT NOT NULL, "
                + COLUMN_SUBREDDIT + " TEXT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_THING_ID + " TEXT NOT NULL, "
                + COLUMN_THUMBNAIL_URL + " TEXT, "
                + COLUMN_UPS + " INTEGER DEFAULT 0, "
                + COLUMN_URL + " TEXT)");
    }

    public static String getBody(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_BODY);
    }

    public static String getDomain(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_DOMAIN);
    }

    public static boolean isKind(Bundle thingBundle, int kind) {
        return getKind(thingBundle) == kind;
    }

    public static int getKind(Bundle thingBundle) {
        return getInt(thingBundle, COLUMN_KIND);
    }

    public static int getLikes(Bundle thingBundle) {
        return getInt(thingBundle, COLUMN_LIKES);
    }

    public static String getLinkId(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_LINK_ID);
    }

    public static String getPermaLink(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_PERMA_LINK);
    }

    public static int getScore(Bundle thingBundle) {
        return getInt(thingBundle, COLUMN_SCORE);
    }

    public static boolean isSelf(Bundle thingBundle) {
        return getBoolean(thingBundle, COLUMN_SELF);
    }

    public static String getSubreddit(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_SUBREDDIT);
    }

    public static String getUrl(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_URL);
    }

    public static CharSequence getTitle(Bundle thingBundle) {
        return getCharSequence(thingBundle, COLUMN_TITLE);
    }

    public static String getThingId(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_THING_ID);
    }

    public static String getThumbnail(Bundle thingBundle) {
        return getString(thingBundle, COLUMN_THUMBNAIL_URL);
    }

    public static boolean hasThumbnail(Bundle thingBundle) {
        return !TextUtils.isEmpty(getThumbnail(thingBundle));
    }

    static boolean getBoolean(Bundle bundle, String columnName) {
        return bundle != null ? bundle.getBoolean(columnName) : null;
    }

    static CharSequence getCharSequence(Bundle bundle, String columnName) {
        return bundle != null ? bundle.getCharSequence(columnName) : null;
    }

    static int getInt(Bundle bundle, String columnName) {
        return bundle != null ? bundle.getInt(columnName) : null;
    }

    static String getString(Bundle bundle, String columnName) {
        return bundle != null ? bundle.getString(columnName) : null;
    }

    public static int parseKind(String kind) {
        if ("t1".equals(kind)) {
            return Things.KIND_COMMENT;
        } else if ("t2".equals(kind)) {
            return Things.KIND_ACCOUNT;
        } else if ("t3".equals(kind)) {
            return Things.KIND_LINK;
        } else if ("t4".equals(kind)) {
            return Things.KIND_MESSAGE;
        } else if ("t5".equals(kind)) {
            return Things.KIND_SUBREDDIT;
        } else {
            throw new IllegalArgumentException("kind: " + kind);
        }
    }
}
