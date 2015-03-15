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

/**
 * {@link Kinds} contains definitions of column names and values for tables that have a kind column
 * and some methods to handle parsing them JSON.
 */
public final class Kinds {

    /** Name of the kind column. Refer to this in table classes for clarity. */
    public static final String COLUMN_KIND = "kind";

    public static final int KIND_MORE = 0;
    public static final int KIND_COMMENT = 1;
    public static final int KIND_ACCOUNT = 2;
    public static final int KIND_LINK = 3;
    public static final int KIND_MESSAGE = 4;
    public static final int KIND_SUBREDDIT = 5;

    /** Converts a kind string to an integer constant. */
    public static int parseKind(String kind) {
        if ("t1".equals(kind)) {
            return KIND_COMMENT;
        } else if ("t2".equals(kind)) {
            return KIND_ACCOUNT;
        } else if ("t3".equals(kind)) {
            return KIND_LINK;
        } else if ("t4".equals(kind)) {
            return KIND_MESSAGE;
        } else if ("t5".equals(kind)) {
            return KIND_SUBREDDIT;
        } else if ("more".equals(kind)) {
            return KIND_MORE;
        } else {
            throw new IllegalArgumentException("kind: " + kind);
        }
    }

    public static String getTag(int kind) {
        switch (kind) {
            case KIND_COMMENT:
                return "t1";

            case KIND_ACCOUNT:
                return "t2";

            case KIND_LINK:
                return "t3";

            case KIND_MESSAGE:
                return "t4";

            case KIND_SUBREDDIT:
                return "t5";

            default:
                throw new IllegalArgumentException("kind: " + kind);
        }
    }
}
