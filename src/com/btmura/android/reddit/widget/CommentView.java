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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.BoringLayout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.data.Formatter;

public class CommentView extends View implements OnGestureListener {

    public static final String TAG = "CommentView";
    public static final boolean DEBUG = Debug.DEBUG;

    private static float FONT_SCALE;

    private static int PADDING;
    private static int ELEMENT_PADDING;

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 3;
    private static final int TEXT_TITLE = 0;
    private static final int TEXT_BODY = 1;
    private static final int TEXT_STATUS = 2;

    private static final Formatter FORMATTER = new Formatter();

    private final GestureDetector detector;
    private OnVoteListener listener;

    private String body;
    private int downs;
    private int likes;
    private int nesting;
    private String title;
    private String thingId;
    private int ups;

    private CharSequence bodyText;

    private StaticLayout titleLayout;
    private StaticLayout bodyLayout;
    private BoringLayout statusLayout;

    private String scoreText;
    private final Rect scoreBounds = new Rect();
    private int rightHeight;
    private int minHeight;

    private final RectF bodyBounds = new RectF();

    public CommentView(Context context) {
        this(context, null);
    }

    public CommentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        detector = new GestureDetector(context, this);
        init(context);
    }

    private void init(Context context) {
        VotingArrows.init(context);
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
                    android.R.attr.textColorLink,
            };

            TEXT_PAINTS = new TextPaint[NUM_TEXT_PAINTS];
            for (int i = 0; i < NUM_TEXT_PAINTS; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                TEXT_PAINTS[i] = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
                TEXT_PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * FONT_SCALE);
                TEXT_PAINTS[i].setColor(a.getColor(1, -1));
                TEXT_PAINTS[i].linkColor = a.getColor(2, -1);
                a.recycle();
            }
        }
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setData(String body, int downs, int likes, int nesting, String title,
            String thingId, int ups) {
        this.body = body;
        this.downs = downs;
        this.likes = likes;
        this.nesting = nesting;
        this.title = title;
        this.thingId = thingId;
        this.ups = ups;
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

        scoreText = VotingArrows.getScoreText(ups - downs + likes);
        VotingArrows.measureScoreText(scoreText, scoreBounds);

        bodyText = FORMATTER.formatSpans(getContext(), body);

        int rightContentWidth = measuredWidth
                - PADDING - VotingArrows.getWidth() - PADDING
                - PADDING * nesting - PADDING;

        titleLayout = null;
        bodyLayout = null;
        rightHeight = 0;

        if (!TextUtils.isEmpty(title)) {
            titleLayout = createTitleLayout(rightContentWidth);
            rightHeight += titleLayout.getHeight();
            rightHeight += ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty(body)) {
            bodyLayout = createBodyLayout(rightContentWidth);
            rightHeight += bodyLayout.getHeight();
            rightHeight += ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty("status")) {
            statusLayout = createStatusLayout(rightContentWidth);
            rightHeight += statusLayout.getHeight();
        }

        int leftHeight = VotingArrows.getHeight();
        minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

        bodyBounds.setEmpty();

        int rx = PADDING * (1 + nesting) + VotingArrows.getWidth() + PADDING;
        if (bodyLayout != null) {
            bodyBounds.left = rx;
            rx += bodyLayout.getWidth();
            bodyBounds.right = rx;
        }

        int ry = (minHeight - rightHeight) / 2;
        if (titleLayout != null) {
            ry += titleLayout.getHeight() + ELEMENT_PADDING;
        }
        if (bodyLayout != null) {
            bodyBounds.top = ry;
            ry += bodyLayout.getHeight();
            bodyBounds.bottom = ry;
            ry += ELEMENT_PADDING;
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
        return new StaticLayout(title, TEXT_PAINTS[TEXT_TITLE],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private StaticLayout createBodyLayout(int width) {
        return new StaticLayout(bodyText, TEXT_PAINTS[TEXT_BODY],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private BoringLayout createStatusLayout(int width) {
        BoringLayout.Metrics m = BoringLayout.isBoring("status", TEXT_PAINTS[TEXT_STATUS]);
        return BoringLayout.make("status", TEXT_PAINTS[TEXT_STATUS], width,
                Alignment.ALIGN_NORMAL, 1, 0, m, true, TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.translate(PADDING * (1 + nesting), PADDING);
        VotingArrows.draw(c, null, false, scoreText, scoreBounds, likes);
        c.translate(0, -PADDING);

        int dx = VotingArrows.getWidth() + PADDING;
        int dy = (minHeight - rightHeight) / 2;
        c.translate(dx, dy);

        if (titleLayout != null) {
            titleLayout.draw(c);
            c.translate(0, titleLayout.getHeight() + ELEMENT_PADDING);
        }

        if (bodyLayout != null) {
            bodyLayout.draw(c);
            c.translate(0, bodyLayout.getHeight() + ELEMENT_PADDING);
        }

        statusLayout.draw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e) || onBodyTouchEvent(e) || super.onTouchEvent(e);
    }

    private boolean onBodyTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                && bodyText instanceof Spannable
                && bodyBounds.contains(e.getX(), e.getY())) {
            float localX = e.getX() - bodyBounds.left;
            float localY = e.getY() - bodyBounds.top;

            int line = bodyLayout.getLineForVertical(Math.round(localY));
            int offset = bodyLayout.getOffsetForHorizontal(line, localX);
            float right = bodyBounds.left + bodyLayout.getLineRight(line);

            if (DEBUG) {
                Log.d(TAG, "b: " + bodyBounds + " x: " + e.getX() + " y: " + e.getY());
            }

            if (localX > right) {
                if (DEBUG) {
                    Log.d(TAG, "lx: " + localX + " r: " + right);
                }
                return false;
            }

            Spannable bodySpan = (Spannable) bodyText;
            ClickableSpan[] spans = bodySpan.getSpans(offset, offset,
                    ClickableSpan.class);
            if (spans != null && spans.length > 0) {
                if (action == MotionEvent.ACTION_UP) {
                    spans[0].onClick(this);
                }
                return true;
            }
        }
        return false;
    }

    public boolean onDown(MotionEvent e) {
        return VotingArrows.onDown(e, getCommentLeft());
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, getCommentLeft(), listener, thingId);
    }

    private float getCommentLeft() {
        return nesting * PADDING;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    public void onShowPress(MotionEvent e) {
    }
}
