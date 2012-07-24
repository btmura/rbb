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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;

import com.btmura.android.reddit.R;

class Thumbnail {

    private static int RADIUS;
    private static int THUMB_WIDTH;
    private static int THUMB_HEIGHT;
    private static RectF THUMB_OUTLINE_RECT;
    private static Paint THUMB_OUTLINE_PAINT;
    private static Paint THUMB_PAINT;

    static void init(Context context) {
        if (THUMB_PAINT == null) {
            Resources r = context.getResources();
            RADIUS = r.getDimensionPixelSize(R.dimen.radius);
            THUMB_WIDTH = THUMB_HEIGHT = r.getDimensionPixelSize(R.dimen.thumb_width);
            THUMB_OUTLINE_RECT = new RectF(0, 0, THUMB_WIDTH, THUMB_HEIGHT);

            THUMB_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_OUTLINE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_OUTLINE_PAINT.setStyle(Style.STROKE);
            THUMB_OUTLINE_PAINT.setColor(r.getColor(R.color.thumb_outline));
        }
    }

    static void draw(Canvas c, Bitmap thumb) {
        if (thumb != null) {
            int tdy = (THUMB_HEIGHT - thumb.getHeight()) / 2;
            c.drawBitmap(thumb, 0, tdy, THUMB_PAINT);
        } else {
            c.drawRoundRect(THUMB_OUTLINE_RECT, RADIUS, RADIUS, THUMB_OUTLINE_PAINT);
        }
    }

    static int getWidth() {
        return THUMB_WIDTH;
    }

    static int getHeight() {
        return THUMB_HEIGHT;
    }
}
