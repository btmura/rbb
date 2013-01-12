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

import com.btmura.android.reddit.content.CaptchaLoader.CaptchaResult;
import com.btmura.android.reddit.net.RedditApi;

public class CaptchaLoader extends AsyncTaskLoader<CaptchaResult> {

    public static final String TAG = "CaptchaLoader";

    public static class CaptchaResult {
        public String iden;
        public Bitmap captchaBitmap;
    }

    private final String captchaId;
    private CaptchaResult result;

    public CaptchaLoader(Context context, String captchaId) {
        super(context);
        this.captchaId = captchaId;
    }

    @Override
    public CaptchaResult loadInBackground() {
        try {
            CaptchaResult captchaResult = new CaptchaResult();
            captchaResult.iden = captchaId;
            captchaResult.captchaBitmap = RedditApi.getCaptcha(captchaId);
            return captchaResult;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void deliverResult(CaptchaResult newResult) {
        if (isReset()) {
            newResult.captchaBitmap.recycle();
            return;
        }

        CaptchaResult oldResult = result;
        result = newResult;

        if (isStarted()) {
            super.deliverResult(newResult);
        }

        if (oldResult != null && oldResult != newResult && !oldResult.captchaBitmap.isRecycled()) {
            oldResult.captchaBitmap.recycle();
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
    public void onCanceled(CaptchaResult result) {
        if (result != null && !result.captchaBitmap.isRecycled()) {
            result.captchaBitmap.recycle();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (result != null && !result.captchaBitmap.isRecycled()) {
            result.captchaBitmap.recycle();
        }
        result = null;
    }
}
