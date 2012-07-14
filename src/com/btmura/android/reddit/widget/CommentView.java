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
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.text.BoringLayout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Comment;

public class CommentView extends View {

    private static float FONT_SCALE;

    private static int PADDING;
    private static int ELEMENT_PADDING;

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 3;
    private static final int TEXT_TITLE = 0;
    private static final int TEXT_BODY = 1;
    private static final int TEXT_STATUS = 2;

    private Comment comment;

    private StaticLayout titleLayout;
    private StaticLayout bodyLayout;
    private BoringLayout statusLayout;

    public CommentView(Context context) {
        this(context, null);
    }

    public CommentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Resources r = context.getResources();
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);

            Theme t = context.getTheme();
            int[] styles = new int[] {
                    R.style.CommentTitleText,
                    R.style.CommentBodyText,
                    R.style.CommentStatusText,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize,
                    android.R.attr.textColor,
            };

            TEXT_PAINTS = new TextPaint[NUM_TEXT_PAINTS];
            for (int i = 0; i < NUM_TEXT_PAINTS; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                TEXT_PAINTS[i] = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
                TEXT_PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * FONT_SCALE);
                TEXT_PAINTS[i].setColor(a.getColor(1, -1));
                a.recycle();
            }
        }
    }

    public void setComment(Comment comment) {
        this.comment = comment;
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

        titleLayout = null;
        bodyLayout = null;
        int minHeight = PADDING * 2;
        int contentWidth = measuredWidth - PADDING * 2;

        switch (comment.type) {
            case Comment.TYPE_HEADER:
                titleLayout = createTitleLayout(contentWidth);
                minHeight += titleLayout.getHeight();
                break;

            default:
                bodyLayout = createBodyLayout(contentWidth);
                minHeight += bodyLayout.getHeight();
                break;
        }

        statusLayout = createStatusLayout(contentWidth);
        minHeight += statusLayout.getHeight();

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getMode(heightMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSize;
                break;

            case MeasureSpec.UNSPECIFIED:
                measuredHeight = minHeight;
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private StaticLayout createTitleLayout(int width) {
        return new StaticLayout(comment.title, TEXT_PAINTS[TEXT_TITLE],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private StaticLayout createBodyLayout(int width) {
        return new StaticLayout(comment.body, TEXT_PAINTS[TEXT_BODY],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private BoringLayout createStatusLayout(int width) {
        BoringLayout.Metrics m = BoringLayout.isBoring(comment.status, TEXT_PAINTS[TEXT_STATUS]);
        return BoringLayout.make(comment.status, TEXT_PAINTS[TEXT_STATUS], width,
                Alignment.ALIGN_NORMAL, 1, 0, m, true, TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.translate(PADDING, PADDING);
        if (titleLayout != null) {
            titleLayout.draw(c);
            c.translate(0, titleLayout.getHeight());
        }
        if (bodyLayout != null) {
            bodyLayout.draw(c);
            c.translate(0, bodyLayout.getHeight());
        }
        statusLayout.draw(c);
    }
}
