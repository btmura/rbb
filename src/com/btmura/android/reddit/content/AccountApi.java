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

package com.btmura.android.reddit.content;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.net.HttpUtils;

public class AccountApi {

    public static final String TAG = "AccountApi";

    public static void addSubreddit(String cookie, String modhash, String subreddit) {
        try {
            InputStream input = HttpUtils.post(Urls.subscribeUrl(), Urls.loginCookie(cookie),
                    Urls.subscribeQuery(modhash, subreddit));
            input.close();
        } catch (IOException e) {
            Log.e(TAG, "addSubreddit", e);
        }
    }
}
