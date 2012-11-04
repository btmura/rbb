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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.BoringLayout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.text.RelativeTime;

public class CommentView extends CustomView implements OnGestureListener {

    public static final String TAG = "CommentView";

    private static final Formatter FORMATTER = new Formatter();

    private final GestureDetector detector;
    private OnVoteListener listener;

    private boolean expanded;
    private int likes;
    private int nesting;
    private String title;
    private String thingId;
    private boolean votable;

    private CharSequence bodyText;
    private String scoreText;
    private final SpannableStringBuilder statusText = new SpannableStringBuilder();

    private StaticLayout titleLayout;
    private StaticLayout bodyLayout;
    private BoringLayout statusLayout;

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
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setData(String author,
            String body,
            long createdUtc,
            boolean expanded,
            int kind,
            int likes,
            int nesting,
            long nowTimeMs,
            int numComments,
            int score,
            String title,
            String thingId,
            boolean votable) {
        this.expanded = expanded;
        this.likes = likes;
        this.nesting = nesting;
        this.title = title;
        this.thingId = thingId;
        this.votable = votable && expanded;

        this.scoreText = VotingArrows.getScoreText(score);
        this.bodyText = FORMATTER.formatSpans(getContext(), body);
        setStatusText(author, createdUtc, expanded, kind, nowTimeMs, numComments, score);

        requestLayout();
    }

    private void setStatusText(String author, long createdUtc, boolean expanded, int kind,
            long nowTimeMs, int numComments, int score) {
        Context c = getContext();
        Resources r = getResources();

        statusText.clear();
        statusText.clearSpans();
        statusText.append(author).append("  ");

        if (createdUtc != 0) {
            statusText.append(r.getQuantityString(R.plurals.points, score, score)).append("  ");
            statusText.append(RelativeTime.format(c, nowTimeMs, createdUtc)).append("  ");
        }

        if (kind == Comments.KIND_HEADER) {
            statusText.append(r.getQuantityString(R.plurals.comments, numComments, numComments))
                    .append("  ");
        }

        if (!expanded) {
            statusText.setSpan(new StyleSpan(Typeface.ITALIC), 0, statusText.length(), 0);
        }
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

        // 2 = left + right margin
        int rightContentWidth = measuredWidth - PADDING * (2 + nesting);
        if (votable) {
            rightContentWidth -= VotingArrows.getWidth(votable) + PADDING;
            VotingArrows.measureScoreText(scoreText, scoreBounds);
        }

        titleLayout = null;
        bodyLayout = null;
        rightHeight = 0;

        if (expanded && !TextUtils.isEmpty(title)) {
            titleLayout = createTitleLayout(rightContentWidth);
            rightHeight += titleLayout.getHeight();
            rightHeight += ELEMENT_PADDING;
        }

        if (expanded && !TextUtils.isEmpty(bodyText)) {
            bodyLayout = createBodyLayout(rightContentWidth);
            rightHeight += bodyLayout.getHeight();
            rightHeight += ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty(statusText)) {
            statusLayout = createStatusLayout(rightContentWidth);
            rightHeight += statusLayout.getHeight();
        }

        int leftHeight = VotingArrows.getHeight(votable);
        minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

        bodyBounds.setEmpty();

        int rx = PADDING * (1 + nesting);
        if (votable) {
            rx += VotingArrows.getWidth(votable) + PADDING;
        }
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
        return new StaticLayout(title, TEXT_PAINTS[COMMENT_TITLE],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private StaticLayout createBodyLayout(int width) {
        return new StaticLayout(bodyText, TEXT_PAINTS[COMMENT_BODY],
                width, Alignment.ALIGN_NORMAL, 1, 0, true);
    }

    private BoringLayout createStatusLayout(int width) {
        BoringLayout.Metrics m = BoringLayout.isBoring(statusText, TEXT_PAINTS[COMMENT_STATUS]);
        return BoringLayout.make(statusText, TEXT_PAINTS[COMMENT_STATUS], width,
                Alignment.ALIGN_NORMAL, 1, 0, m, true, TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.translate(PADDING * (1 + nesting), PADDING);
        if (votable) {
            VotingArrows.draw(c, null, scoreText, scoreBounds, likes);
        }
        c.translate(0, -PADDING);

        int dx = votable ? VotingArrows.getWidth(votable) + PADDING : 0;
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

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "b: " + bodyBounds + " x: " + e.getX() + " y: " + e.getY());
            }

            if (localX > right) {
                if (BuildConfig.DEBUG) {
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
        return VotingArrows.onDown(e, getCommentLeft(), votable);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, getCommentLeft(), votable, listener, thingId);
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
