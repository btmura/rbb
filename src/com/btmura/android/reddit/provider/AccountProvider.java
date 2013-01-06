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

package com.btmura.android.reddit.provider;

import android.content.UriMatcher;
import android.net.Uri;

import com.btmura.android.reddit.database.Accounts;

public class AccountProvider extends BaseProvider {

    public static final String TAG = "AccountProvider";

    public static final String AUTHORITY = "com.btmura.android.reddit.provider.accounts";

    static final String PATH_ACCOUNTS = "accounts";

    static final String BASE_AUTHORITY_URI = "content://" + AUTHORITY + "/";
    public static final Uri ACCOUNTS_URI = Uri.parse(BASE_AUTHORITY_URI + PATH_ACCOUNTS);
    public static final Uri ACCOUNTS_NOTIFY_URI = makeNotifyUri(ACCOUNTS_URI);

    private static final UriMatcher MATCHER = new UriMatcher(0);
    private static final int MATCH_ACCOUNTS = 0;
    static {
        MATCHER.addURI(AUTHORITY, PATH_ACCOUNTS, MATCH_ACCOUNTS);
    }

    public AccountProvider() {
        super(TAG);
    }

    @Override
    protected String getTable(Uri uri) {
        return Accounts.TABLE_NAME;
    }
}
