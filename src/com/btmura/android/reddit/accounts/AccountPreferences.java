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

package com.btmura.android.reddit.accounts;

import android.content.Context;
import android.content.SharedPreferences;

import com.btmura.android.reddit.database.Subreddits;

public class AccountPreferences {

    /** Name of the account preferences file. */
    private static final String PREFS = "accountPreferences";

    /** Global preference for the last selected account. */
    private static final String GLOBAL_PREF_LAST_ACCOUNT = "lastAccount";

    /** Global preference for the last selected filter by any account. */
    private static final String GLOBAL_PREF_LAST_FILTER = "lastFilter";

    /** Account preference for the last selected subreddit. */
    private static final String ACCOUNT_PREF_LAST_SUBREDDIT = "lastSubreddit";

    public static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, 0);
    }

    public static String getLastAccount(SharedPreferences prefs, String defValue) {
        return prefs.getString(GLOBAL_PREF_LAST_ACCOUNT, defValue);
    }

    public static void setLastAccount(SharedPreferences prefs, String accountName) {
        prefs.edit().putString(GLOBAL_PREF_LAST_ACCOUNT, accountName).apply();
    }

    public static int getLastFilter(SharedPreferences prefs, int defValue) {
        return prefs.getInt(GLOBAL_PREF_LAST_FILTER, defValue);
    }

    public static void setLastFilter(SharedPreferences prefs, int filter) {
        prefs.edit().putInt(GLOBAL_PREF_LAST_FILTER, filter).apply();
    }

    public static String getLastSubreddit(SharedPreferences prefs, String accountName) {
        return prefs.getString(getAccountPreferenceKey(accountName, ACCOUNT_PREF_LAST_SUBREDDIT),
                Subreddits.NAME_FRONT_PAGE);
    }

    public static void setLastSubreddit(SharedPreferences prefs, String accountName,
            String subreddit) {
        prefs.edit().putString(getAccountPreferenceKey(accountName, ACCOUNT_PREF_LAST_SUBREDDIT),
                subreddit).apply();
    }

    private static String getAccountPreferenceKey(String accountName, String prefName) {
        return accountName + "." + prefName;
    }
}
