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

import com.btmura.android.reddit.entity.Subreddit;

import android.content.Context;
import android.graphics.Canvas;
import android.text.BoringLayout;
import android.text.Layout.Alignment;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;

/**
 * {@link CustomView} for displaying subreddit names. It can show subscriber
 * count if the information is available.
 */
public class SubredditView extends CustomView {

    private String title;
    private BoringLayout.Metrics titleMetrics;
    private BoringLayout titleLayout;

    public SubredditView(Context context) {
        this(context, null);
    }

    public SubredditView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubredditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @param name of the subreddit or empty string for front page
     * @param subscribers or -1 if no subscriber info available
     */
    public void setData(String name, int subscribers) {
        this.title = Subreddit.getTitle(getContext(), name);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = 0;
        int measuredHeight = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measuredWidth = widthSize;
                break;

            case MeasureSpec.UNSPECIFIED:
                measuredWidth = getSuggestedMinimumWidth();
                break;
        }

        setTitleLayout(measuredWidth - PADDING * 2);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSize;
                break;

            case MeasureSpec.UNSPECIFIED:
                measuredHeight = getMinimumHeight();
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.translate(PADDING, PADDING);
        titleLayout.draw(c);
    }

    private void setTitleLayout(int width) {
        TextPaint titlePaint = TEXT_PAINTS[SUBREDDIT_TITLE];
        titleMetrics = BoringLayout.isBoring(title, titlePaint, titleMetrics);
        if (titleLayout == null) {
            titleLayout = BoringLayout.make(title, titlePaint, width, Alignment.ALIGN_NORMAL, 1f,
                    0f, titleMetrics, false, TruncateAt.END, width);
        } else {
            titleLayout.replaceOrMake(title, titlePaint, width, Alignment.ALIGN_NORMAL, 1f,
                    0f, titleMetrics, false, TruncateAt.END, width);
        }
    }

    private int getMinimumHeight() {
        return PADDING + titleLayout.getHeight() + PADDING;
    }
}
