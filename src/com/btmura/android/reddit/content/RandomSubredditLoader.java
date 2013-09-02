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

import java.io.IOException;
import java.net.HttpURLConnection;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.UriHelper;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.widget.FilterAdapter;

public class RandomSubredditLoader extends BaseAsyncTaskLoader<String> {

    private static final String TAG = "RandomSubredditLoader";

    private final String accountName;

    public RandomSubredditLoader(Context context, String accountName) {
        super(context);
        this.accountName = accountName;
    }

    @Override
    public String loadInBackground() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadInBackground");
        }

        try {
            CharSequence url = Urls.subreddit(Subreddits.NAME_RANDOM,
                    FilterAdapter.SUBREDDIT_HOT,
                    null,
                    Urls.TYPE_JSON);
            String cookie = AccountUtils.getCookie(getContext(), accountName);

            HttpURLConnection conn = RedditApi.connect(url, cookie, false, false);
            if (conn.getResponseCode() == 302) {
                String location = conn.getHeaderField("Location");
                return UriHelper.getSubreddit(Uri.parse(location));
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
