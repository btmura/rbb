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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.btmura.android.reddit.R;

public class ThumbnailLoader {

    private static final String TAG = "ThumbnailLoader";

    private static final BitmapCache BITMAP_CACHE = new BitmapCache(2 * 1024 * 1024);

    public void setThumbnail(Context context, ThingView v, String url) {
        if (!TextUtils.isEmpty(url)) {
            Bitmap b = BITMAP_CACHE.get(url);
            v.setThumbnailBitmap(b);
            if (b == null) {
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
            LoadThumbnailTask task = (LoadThumbnailTask) v.getTag();
            if (task != null) {
                task.cancel(true);
                v.setTag(null);
            }
            v.setThumbnailBitmap(null);
        }
    }

    static class BitmapCache extends LruCache<String, Bitmap> {
        BitmapCache(int size) {
            super(size);
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    }

    class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

        private final Context context;
        private final WeakReference<ThingView> ref;
        private final String url;

        LoadThumbnailTask(Context context, ThingView v, String url) {
            this.context = context.getApplicationContext();
            this.ref = new WeakReference<ThingView>(v);
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            HttpURLConnection conn = null;
            Bitmap original = null;
            try {
                URL u = new URL(url);
                conn = (HttpURLConnection) u.openConnection();
                if (isCancelled()) {
                    return null;
                }

                InputStream is = conn.getInputStream();
                if (isCancelled()) {
                    return null;
                }

                original = BitmapFactory.decodeStream(is);
                if (isCancelled()) {
                    return null;
                }

                return getThumbnailBitmap(original);

            } catch (MalformedURLException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                if (original != null) {
                    original.recycle();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }

        private Bitmap getThumbnailBitmap(Bitmap original) {
            Resources r = context.getResources();
            float density = r.getDisplayMetrics().density;
            int radius = r.getDimensionPixelSize(R.dimen.radius);
            int thumbWidth = r.getDimensionPixelSize(R.dimen.thumb_width);
            int thumbHeight = Math.round(original.getHeight() * density);

            Bitmap rounded = Bitmap.createBitmap(thumbWidth, thumbHeight, Config.ARGB_8888);
            Canvas canvas = new Canvas(rounded);
            canvas.drawARGB(0, 0, 0, 0);

            Rect src = new Rect(0, 0, original.getWidth(), original.getHeight());
            RectF dst = new RectF(0, 0, thumbWidth, thumbHeight);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawRoundRect(dst, radius, radius, paint);

            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(original, src, dst, paint);

            return rounded;
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b != null) {
                BITMAP_CACHE.put(url, b);
            }
            ThingView v = ref.get();
            if (v != null && equals(v.getTag())) {
                if (b != null) {
                    v.setThumbnailBitmap(b);
                }
                v.setTag(null);
            }
        }
    }
}
