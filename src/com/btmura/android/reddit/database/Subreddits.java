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

import android.provider.BaseColumns;


public class Subreddits implements BaseColumns, SyncColumns {
    public static final String TABLE_NAME = "subreddits";

    public static final String COLUMN_ACCOUNT = "account";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_STATE = "state";
    public static final String COLUMN_EXPIRATION = "expiration";

    public static final String SORT_BY_NAME = Subreddits.COLUMN_NAME + " COLLATE NOCASE ASC";

    public static final String NAME_FRONT_PAGE = "";
    public static final String ACCOUNT_NONE = "";
}