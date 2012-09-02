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
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.btmura.android.reddit.R;

/**
 * {@link View} that performs shared initialization of dimensions and paints
 * common to most custom views.
 */
abstract class CustomView extends View {

    static float FONT_SCALE = -1;
    static int PADDING;
    static int ELEMENT_PADDING;
    static int MIN_DETAILS_WIDTH;
    static int MAX_DETAILS_WIDTH;

    static final int NUM_TEXT_PAINTS = 7;
    static final int SUBREDDIT_TITLE = 0;
    static final int SUBREDDIT_STATUS = 1;
    static final int THING_TITLE = 2;
    static final int THING_STATUS = 3;
    static final int COMMENT_TITLE = 4;
    static final int COMMENT_BODY = 5;
    static final int COMMENT_STATUS = 6;
    static final TextPaint[] TEXT_PAINTS = new TextPaint[NUM_TEXT_PAINTS];

    CustomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private static void init(Context context) {
        Resources r = context.getResources();

        // Reinitialize everything when the font scale changes via Settings.
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);
            MIN_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.min_details_width);
            MAX_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.max_details_width);

            // We only need these when things change so don't make them static.
            int[] styles = new int[] {
                    R.style.SubredditTitleText,
                    R.style.SubredditStatusText,
                    R.style.ThingTitleText,
                    R.style.ThingStatusText,
                    R.style.CommentTitleText,
                    R.style.CommentBodyText,
                    R.style.CommentStatusText,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize,
                    android.R.attr.textColor,
                    android.R.attr.textColorLink,
            };

            Theme t = context.getTheme();
            for (int i = 0; i < NUM_TEXT_PAINTS; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                TEXT_PAINTS[i] = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                TEXT_PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * fontScale);
                TEXT_PAINTS[i].setColor(a.getColor(1, -1));
                TEXT_PAINTS[i].linkColor = a.getColor(2, -1);
                a.recycle();
            }
        }
    }
}
