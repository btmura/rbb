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

package com.btmura.android.reddit.content;

import android.content.Context;
import android.support.v4.content.CursorLoader;

import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.util.Array;

public class AccountSubredditListLoader extends CursorLoader {

    private static final String[] PROJECTION = {
            Subreddits._ID,
            Subreddits.COLUMN_NAME
    };

    public AccountSubredditListLoader(Context context, String accountName) {
        super(context, SubredditProvider.SUBREDDITS_URI, PROJECTION,
                Subreddits.SELECT_BY_ACCOUNT_NOT_DELETED, Array.of(accountName),
                Subreddits.SORT_BY_NAME);
    }
}
