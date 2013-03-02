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

import com.btmura.android.reddit.database.Subreddits;

public class AccountPrefs extends Prefs {

    /** Global preference for the last selected account. */
    private static final String GLOBAL_LAST_ACCOUNT = "lastAccount";

    /** Global preference for the last selected filter by any account. */
    private static final String GLOBAL_LAST_SUBREDDIT_FILTER = "lastSubredditFilter";

    /** Account preference for the last selected subreddit. */
    private static final String ACCOUNT_LAST_SUBREDDIT = "lastSubreddit";

    public static String getLastAccount(Context context, String defValue) {
        return getPrefsInstance(context).getString(GLOBAL_LAST_ACCOUNT, defValue);
    }

    public static void setLastAccount(Context context, String accountName) {
        getPrefsInstance(context).edit().putString(GLOBAL_LAST_ACCOUNT, accountName).apply();
    }

    public static int getLastSubredditFilter(Context context, int defValue) {
        return getPrefsInstance(context).getInt(GLOBAL_LAST_SUBREDDIT_FILTER, defValue);
    }

    public static void setLastSubredditFilter(Context context, int filter) {
        getPrefsInstance(context).edit().putInt(GLOBAL_LAST_SUBREDDIT_FILTER, filter).apply();
    }

    public static String getLastSubreddit(Context context, String accountName) {
        String key = getAccountPreferenceKey(accountName, ACCOUNT_LAST_SUBREDDIT);
        return getPrefsInstance(context).getString(key, Subreddits.NAME_FRONT_PAGE);
    }

    public static void setLastSubreddit(Context context, String accountName, String subreddit) {
        String key = getAccountPreferenceKey(accountName, ACCOUNT_LAST_SUBREDDIT);
        getPrefsInstance(context).edit().putString(key, subreddit).apply();
    }

    private static String getAccountPreferenceKey(String accountName, String prefName) {
        return accountName + "." + prefName;
    }
}
