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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.ThemePrefs;

class Thumbnail {

    private static final int EVENT_NONE = 0;
    private static final int EVENT_CLICK = 1;

    private static int THEME;
    private static int RADIUS;
    private static int THUMB_WIDTH;
    private static int THUMB_HEIGHT;
    private static RectF THUMB_RECT;
    private static Paint THUMB_OUTLINE_PAINT;
    private static Paint THUMB_PAINT;
    private static Drawable THUMBNAIL_BITMAP;

    static void init(Context context) {
        int theme = ThemePrefs.getTheme(context);
        if (THEME != theme || THUMB_PAINT == null) {
            THEME = theme;
            Resources r = context.getResources();
            RADIUS = r.getDimensionPixelSize(R.dimen.radius);
            THUMB_WIDTH = THUMB_HEIGHT = r.getDimensionPixelSize(R.dimen.thumb_width);
            THUMB_RECT = new RectF(0, 0, THUMB_WIDTH, THUMB_HEIGHT);

            THUMB_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_PAINT.setFilterBitmap(true);
            THUMB_OUTLINE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_OUTLINE_PAINT.setStyle(Style.STROKE);
            THUMB_OUTLINE_PAINT.setColor(r.getColor(R.color.thumb_outline));

            THUMBNAIL_BITMAP = r.getDrawable(ThemePrefs.getThumbnailDrawableId(context));
            THUMBNAIL_BITMAP.setBounds(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
        }
    }

    static BitmapShader newBitmapShader(Bitmap thumb, Matrix destMatrix) {
        float scaleX = (float) THUMB_WIDTH / thumb.getWidth();
        float scaleY = (float) THUMB_HEIGHT / thumb.getHeight();
        destMatrix.setScale(scaleX, scaleY);

        BitmapShader shader = new BitmapShader(thumb, TileMode.CLAMP, TileMode.CLAMP);
        shader.setLocalMatrix(destMatrix);
        return shader;
    }

    static void setBounds(Rect bounds, int offsetLeft, int offsetTop) {
        bounds.set(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
        bounds.offsetTo(offsetLeft, offsetTop);
    }

    static void draw(Canvas c, BitmapShader thumbShader, boolean hasThumbnailUrl) {
        if (thumbShader != null) {
            THUMB_PAINT.setShader(thumbShader);
            c.drawRoundRect(THUMB_RECT, RADIUS, RADIUS, THUMB_PAINT);
        } else if (hasThumbnailUrl) {
            c.drawRoundRect(THUMB_RECT, RADIUS, RADIUS, THUMB_OUTLINE_PAINT);
        } else {
            THUMBNAIL_BITMAP.draw(c);
        }
    }

    static int getWidth() {
        return THUMB_WIDTH;
    }

    static int getHeight() {
        return THUMB_HEIGHT;
    }

    static boolean onDown(MotionEvent e, Rect bounds, boolean hasThumbnail) {
        return getEvent(e, bounds, hasThumbnail) != EVENT_NONE;
    }

    static boolean onSingleTapUp(MotionEvent e, Rect bounds, boolean hasThumbnail,
            OnVoteListener listener, View view) {
        if (listener != null) {
            int event = getEvent(e, bounds, hasThumbnail);
            switch (event) {
                case EVENT_CLICK:
                    view.playSoundEffect(SoundEffectConstants.CLICK);
                    listener.onThumbnailClick(view);
                    return true;
            }
        }
        return false;
    }

    private static int getEvent(MotionEvent e, Rect bounds, boolean hasThumbnail) {
        if (hasThumbnail && bounds.contains((int) e.getX(), (int) e.getY())) {
            return EVENT_CLICK;
        }
        return EVENT_NONE;
    }
}
