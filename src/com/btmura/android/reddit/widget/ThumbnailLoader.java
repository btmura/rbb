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

package com.btmura.android.reddit.widget;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;

public class ThumbnailLoader {

    private static final String TAG = "ThumbnailLoader";

    private static final boolean DEBUG = BuildConfig.DEBUG && !true;

    private static final int LOCK_WAIT_TIME_MS = 250;

    private static final AtomicInteger taskIdCounter = DEBUG ? new AtomicInteger() : null;

    private static volatile boolean locked = false;

    public static void lock(boolean lock) {
        locked = lock;
    }

    public void setThumbnail(Context context, ThingView v, String url) {
        if (!TextUtils.isEmpty(url)) {
            Bitmap b = BitmapCache.getInstance(context).get(url);
            v.setThumbnailBitmap(b, false);
            if (b != null) {
                clearLoadThumbnailTask(v);
            } else {
                LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
                if (task == null || !url.equals(task.url)) {
                    if (task != null) {
                        task.cancel(true);
                    }
                    task = new LoadThumbnailTask(context, v, url);
                    v.setTag(task);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        } else {
            v.setThumbnailBitmap(null, false);
            clearLoadThumbnailTask(v);
        }
    }

    private void clearLoadThumbnailTask(ThingView v) {
        LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
        if (task != null) {
            task.cancel(true);
            v.setTag(null);
        }
    }

    class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

        private final Context context;
        private final int taskId;
        private final WeakReference<ThingView> ref;
        private final String url;

        LoadThumbnailTask(Context context, ThingView v, String url) {
            this.context = context.getApplicationContext();
            this.taskId = taskIdCounter != null ? taskIdCounter.getAndIncrement() : -1;
            this.ref = new WeakReference<ThingView>(v);
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            while (locked) {
                if (DEBUG) {
                    Log.d(TAG, taskId + ": locked");
                }
                SystemClock.sleep(LOCK_WAIT_TIME_MS);
                if (isCancelled()) {
                    if (DEBUG) {
                        Log.d(TAG, taskId + ": cancelled(1)");
                    }
                    return null;
                }
            }

            HttpURLConnection conn = null;
            try {
                URL u = new URL(url);
                conn = (HttpURLConnection) u.openConnection();
                if (isCancelled()) {
                    if (DEBUG) {
                        Log.d(TAG, taskId + ": cancelled(2)");
                    }
                    return null;
                }

                InputStream is = null;
                try {
                    is = conn.getInputStream();
                    if (isCancelled()) {
                        if (DEBUG) {
                            Log.d(TAG, taskId + ": cancelled(3)");
                        }
                        return null;
                    }

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inScaled = true;
                    options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
                    options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
                    return BitmapFactory.decodeStream(is, null, options);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }

            } catch (MalformedURLException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b != null) {
                BitmapCache.getInstance(context).put(url, b);
            }
            ThingView v = ref.get();
            if (v != null && equals(v.getTag())) {
                if (b != null) {
                    v.setThumbnailBitmap(b, true);
                }
                v.setTag(null);
            }
        }
    }

    static class BitmapCache extends LruCache<String, Bitmap> {

        private static BitmapCache BITMAP_CACHE;

        static BitmapCache getInstance(Context context) {
            if (BITMAP_CACHE == null) {
                int cacheSize = context.getResources().getInteger(R.integer.bitmap_cache_size);
                BITMAP_CACHE = new BitmapCache(cacheSize);
            }
            return BITMAP_CACHE;
        }

        BitmapCache(int size) {
            super(size);
        }
    }

}
