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

import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AccountSubredditService extends IntentService {

    public static final String TAG = "AccountSubredditService";

    public static final String EXTRA_COOKIE = "c";
    public static final String EXTRA_MODHASH = "m";

    public static final String EXTRA_SUBSCRIBE = "ss";
    public static final String EXTRA_SUBREDDIT = "sr";

    public AccountSubredditService() {
        super("AccountSubredditServiceWorker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        handleSubscribe(intent);
    }

    private void handleSubscribe(Intent intent) {
        String cookie = intent.getStringExtra(EXTRA_COOKIE);
        String modhash = intent.getStringExtra(EXTRA_MODHASH);
        String subreddit = intent.getStringExtra(EXTRA_SUBREDDIT);
        boolean subscribe = intent.getBooleanExtra(EXTRA_SUBSCRIBE, false);
        try {
            NetApi.subscribe(cookie, modhash, subreddit, subscribe);
        } catch (IOException e) {
            Log.e(TAG, "subscribe", e);
        }
    }
}
