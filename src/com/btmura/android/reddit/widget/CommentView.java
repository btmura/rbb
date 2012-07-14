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
import android.graphics.Canvas;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.btmura.android.reddit.entity.Comment;

public class CommentView extends View {

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 2;
    private static final int TEXT_TITLE = 0;
    private static final int TEXT_BODY = 1;

    private Comment comment;

    private StaticLayout titleLayout;
    private StaticLayout bodyLayout;

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
        if (TEXT_PAINTS == null) {
            TEXT_PAINTS = new TextPaint[NUM_TEXT_PAINTS];
            for (int i = 0; i < NUM_TEXT_PAINTS; i++) {
                TEXT_PAINTS[i] = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
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
        int minHeight = 0;

        switch (comment.type) {
            case Comment.TYPE_HEADER:
                titleLayout = createTitleLayout(measuredWidth);
                minHeight += titleLayout.getHeight();
                break;

            default:
                bodyLayout = createBodyLayout(measuredWidth);
                minHeight += bodyLayout.getHeight();
                break;
        }

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

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (titleLayout != null) {
            titleLayout.draw(c);
        }
        if (bodyLayout != null) {
            bodyLayout.draw(c);
        }

    }
}
