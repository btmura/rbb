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

public interface BaseThingColumns {

    // The following columns allow us to show pending saves to the user before
    // we have had a chance to sync them back to the server.

    /** String author name of the saved item. */
    public static final String COLUMN_AUTHOR = Things.COLUMN_AUTHOR;

    /** Long UTC creation time of the saved item. */
    public static final String COLUMN_CREATED_UTC = Things.COLUMN_CREATED_UTC;

    /** String domain of the thing. */
    public static final String COLUMN_DOMAIN = Things.COLUMN_DOMAIN;

    /** Integer number of downvotes. */
    public static final String COLUMN_DOWNS = Things.COLUMN_DOWNS;

    /** Integer either -1, 0, 1 to represent if the user liked it. */
    public static final String COLUMN_LIKES = Things.COLUMN_LIKES;

    /** Integer indicating whether hidden. 0 is false. 1 is true. */
    public static final String COLUMN_HIDDEN = Things.COLUMN_HIDDEN;

    /** Integer number of comments. */
    public static final String COLUMN_NUM_COMMENTS = Things.COLUMN_NUM_COMMENTS;

    /** Boolean indicating whether this is for over 18 folks. */
    public static final String COLUMN_OVER_18 = Things.COLUMN_OVER_18;

    /** String URL on reddit of the thing. */
    public static final String COLUMN_PERMA_LINK = Things.COLUMN_PERMA_LINK;

    /** Column indicating whether this thing has been saved. */
    public static final String COLUMN_SAVED = "saved";

    /** Boolean indicating whether this is a self post or link. */
    public static final String COLUMN_SELF = "self";

    /** Integer score of the thing. */
    public static final String COLUMN_SCORE = Things.COLUMN_SCORE;

    /** String subreddit name of the thing. */
    public static final String COLUMN_SUBREDDIT = Things.COLUMN_SUBREDDIT;

    /** String title of this thing. */
    public static final String COLUMN_TITLE = "title";

    /** String URL of the thumbnail. */
    public static final String COLUMN_THUMBNAIL_URL = Things.COLUMN_THUMBNAIL_URL;

    /** Integer amount of upvotes for this thing. */
    public static final String COLUMN_UPS = "ups";

    /** String URL of the thing. */
    public static final String COLUMN_URL = Things.COLUMN_URL;

    static final String CREATE_THING_COLUMNS = ""
            // The following columns are for storing enough information so
            // we can show the user we're going to save their item.
            + COLUMN_AUTHOR + " TEXT,"
            + COLUMN_CREATED_UTC + " INTEGER DEFAULT 0,"
            + COLUMN_DOMAIN + " TEXT,"
            + COLUMN_DOWNS + " INTEGER DEFAULT 0,"
            + COLUMN_LIKES + " INTEGER DEFAULT 0, "
            + COLUMN_NUM_COMMENTS + " INTEGER DEFAULT 0,"
            + COLUMN_OVER_18 + " INTEGER DEFAULT 0,"
            + COLUMN_PERMA_LINK + " TEXT,"
            + COLUMN_SCORE + " INTEGER DEFAULT 0,"
            + COLUMN_SELF + " INTEGER DEFAULT 0, "
            + COLUMN_SUBREDDIT + " TEXT,"
            + COLUMN_TITLE + " TEXT,"
            + COLUMN_THUMBNAIL_URL + " TEXT,"
            + COLUMN_UPS + " INTEGER DEFAULT 0,"
            + COLUMN_URL + " TEXT";

    static final String CREATE_THING_COLUMNS_V2 = CREATE_THING_COLUMNS + ","
            + COLUMN_HIDDEN + " INTEGER DEFAULT 0,"
            + COLUMN_SAVED + " INTEGER DEFAULT 0";
}
