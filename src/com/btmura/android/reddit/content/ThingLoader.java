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
import java.net.URL;
import java.util.List;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.provider.NetApi;

public class ThingLoader extends AsyncTaskLoader<List<Thing>> {

    private static final String TAG = "ThingLoader";

    private final String accountName;
    private final String parentSubreddit;
    private final URL url;
    private List<Thing> things;
    private List<Thing> initThings;

    public ThingLoader(Context context, String accountName, String parentSubreddit, URL url,
            List<Thing> initThings) {
        super(context.getApplicationContext());
        this.accountName = accountName;
        this.parentSubreddit = parentSubreddit;
        this.url = url;
        this.initThings = initThings;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (things != null) {
            deliverResult(things);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(List<Thing> things) {
        this.things = things;
        super.deliverResult(things);
    }

    @Override
    public List<Thing> loadInBackground() {
        try {
            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            return NetApi.queryThings(context, url, cookie, parentSubreddit, initThings);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "loadInBackground: " + url, e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "loadInBackground: " + url, e);
        } catch (IOException e) {
            Log.e(TAG, "loadInBackground: " + url, e);
        }
        return null;
    }
}
