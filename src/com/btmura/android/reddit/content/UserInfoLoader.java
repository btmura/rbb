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

import com.btmura.android.reddit.net.AccountInfoResult;
import com.btmura.android.reddit.net.RedditApi;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

/**
 * {@link AsyncTaskLoader} that loads a user's account info.
 */
public class UserInfoLoader extends AsyncTaskLoader<AccountInfoResult> {

    public static final String TAG = "UserInfoLoader";

    private final String user;
    private AccountInfoResult result;

    public UserInfoLoader(Context context, String user) {
        super(context.getApplicationContext());
        this.user = user;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (result != null) {
            deliverResult(result);
        } else {
            forceLoad();
        }
    }

    @Override
    public AccountInfoResult loadInBackground() {
        try {
            return RedditApi.aboutUser(user, null);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
