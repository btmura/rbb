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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.provider.NetApi;

public class CaptchaLoader extends AsyncTaskLoader<Bitmap> {

    public static final String TAG = "CaptchaLoader";
    public static final boolean DEBUG = Debug.DEBUG;

    private final String captchaId;

    private Bitmap result;

    public CaptchaLoader(Context context, String captchaId) {
        super(context);
        this.captchaId = captchaId;
    }

    @Override
    public Bitmap loadInBackground() {
        if (DEBUG) {
            Log.d(TAG, "loadInBackground");
        }
        try {
            return NetApi.captcha(captchaId);
        } catch (IOException e) {
            Log.e(TAG, "loadInBackground", e);
            return null;
        }
    }

    @Override
    public void deliverResult(Bitmap newResult) {
        if (isReset()) {
            newResult.recycle();
            return;
        }

        Bitmap oldResult = result;
        result = newResult;

        if (isStarted()) {
            super.deliverResult(newResult);
        }

        if (oldResult != null && oldResult != newResult && !oldResult.isRecycled()) {
            oldResult.recycle();
        }
    }

    @Override
    protected void onStartLoading() {
        if (result != null) {
            deliverResult(result);
        }
        if (takeContentChanged() || result == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(Bitmap result) {
        if (result != null && !result.isRecycled()) {
            result.recycle();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (result != null && !result.isRecycled()) {
            result.recycle();
        }
        result = null;
    }
}
