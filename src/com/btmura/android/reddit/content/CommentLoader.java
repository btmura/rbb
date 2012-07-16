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
import com.btmura.android.reddit.entity.Comment;
import com.btmura.android.reddit.provider.NetApi;

public class CommentLoader extends AsyncTaskLoader<List<Comment>> {

    public static final String TAG = "CommentLoader";

    private final String accountName;
    private final URL url;
    private List<Comment> comments;

    public CommentLoader(Context context, String accountName, URL url) {
        super(context.getApplicationContext());
        this.accountName = accountName;
        this.url = url;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (comments != null) {
            deliverResult(comments);
        } else {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(List<Comment> comments) {
        this.comments = comments;
        super.deliverResult(comments);
    }

    @Override
    public List<Comment> loadInBackground() {
        try {
            Context context = getContext();
            String cookie = AccountUtils.getCookie(context, accountName);
            return NetApi.queryComments(getContext(), url, cookie);
        } catch (IOException e) {
            Log.e(TAG, "loadInBackground", e);
        } catch (OperationCanceledException e) {
            Log.e(TAG, "loadInBackground", e);
        } catch (AuthenticatorException e) {
            Log.e(TAG, "loadInBackground", e);
        }
        return null;
    }
}
