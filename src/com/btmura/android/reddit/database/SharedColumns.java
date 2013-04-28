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

public class SharedColumns {

    /** String account name. */
    public static final String COLUMN_ACCOUNT = "account";

    public static final String COLUMN_LOCAL_HIDDEN = "localHidden";

    public static final String COLUMN_SAVE = "save";

    /** Long session id referring to the primary key in the sessions table. */
    public static final String COLUMN_SESSION_ID = "sessionId";

    /** Full string name of some thing with the kind tag attached. */
    public static final String COLUMN_THING_ID = "thingId";

    public static final String COLUMN_VOTE = "vote";

    public static final String SELECT_BY_ACCOUNT = COLUMN_ACCOUNT + "=?";

    public static final String SELECT_BY_ID = BaseColumns._ID + "=?";

    public static final String SELECT_BY_SESSION_ID = COLUMN_SESSION_ID + "=?";

    public static final String SELECT_BY_THING_ID = COLUMN_THING_ID + "=?";

    public static final String SORT_BY_ID = BaseColumns._ID + " ASC";
}
