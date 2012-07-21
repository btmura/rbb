package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Thing;

public class ThingView extends View implements OnGestureListener {

    public static final String TAG = "ThingView";
    public static final boolean DEBUG = Debug.DEBUG;

    private static float FONT_SCALE;

    private static int PADDING;
    private static int ELEMENT_PADDING;
    private static int MIN_DETAILS_WIDTH;
    private static int MAX_DETAILS_WIDTH;

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 2;
    private static final int TEXT_TITLE = 0;
    private static final int TEXT_STATUS = 1;

    private final GestureDetector detector;
    private OnVoteListener listener;

    private int bodyWidth;
    private Thing thing;
    private Bitmap thumb;

    private Layout titleLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

    private String scoreText;
    private Rect scoreBounds = new Rect();
    private int rightHeight;
    private int minHeight;

    public ThingView(Context context) {
        this(context, null);
    }

    public ThingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        detector = new GestureDetector(context, this);
        init(context);
    }

    private void init(Context context) {
        VotingArrows.init(context);
        Thumbnail.init(context);
        Resources r = context.getResources();
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);
            MIN_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.min_details_width);
            MAX_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.max_details_width);

            Theme t = context.getTheme();
            int[] styles = new int[] {
                    R.style.ThingTitleText,
                    R.style.ThingStatusText,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize,
                    android.R.attr.textColor,
            };

            TEXT_PAINTS = new TextPaint[NUM_TEXT_PAINTS];
            for (int i = 0; i < NUM_TEXT_PAINTS; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                TEXT_PAINTS[i] = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                TEXT_PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * FONT_SCALE);
                TEXT_PAINTS[i].setColor(a.getColor(1, -1));
                a.recycle();
            }
        }
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setBodyWidth(int bodyWidth) {
        if (this.bodyWidth != bodyWidth) {
            this.bodyWidth = bodyWidth;
            requestLayout();
        }
    }

    public void setThing(Thing thing) {
        if (this.thing != thing) {
            this.thing = thing;
            requestLayout();
        }
    }

    public void setThumbnail(Bitmap bitmap) {
        this.thumb = bitmap;
        invalidate();
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

        scoreText = VotingArrows.getScoreText(thing.score);
        VotingArrows.measureScoreText(scoreText, scoreBounds);

        int titleWidth;
        int detailsWidth;
        CharSequence detailsText;

        if (bodyWidth > 0) {
            titleWidth = Math.min(measuredWidth, bodyWidth) - PADDING * 2;
            int remainingWidth = measuredWidth - bodyWidth - PADDING * 2;
            if (remainingWidth > MAX_DETAILS_WIDTH) {
                detailsWidth = MAX_DETAILS_WIDTH;
                detailsText = thing.details;
            } else if (remainingWidth > MIN_DETAILS_WIDTH) {
                detailsWidth = MIN_DETAILS_WIDTH;
                detailsText = thing.domain;
            } else {
                detailsWidth = 0;
                detailsText = "";
            }
        } else {
            titleWidth = measuredWidth - PADDING * 2;
            detailsWidth = 0;
            detailsText = "";
        }

        int width = VotingArrows.getWidth();
        if (thing.thumbnail != null) {
            width += PADDING + Thumbnail.getWidth();
        }
        width += PADDING;
        titleWidth -= width;
        int statusWidth = measuredWidth - PADDING * 2;
        statusWidth -= width;
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;titleWidth -= width;
        }

        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        detailsWidth = Math.max(0, detailsWidth);

        titleLayout = makeTitleLayout(titleWidth);
        statusLayout = makeLayout(TEXT_STATUS, thing.status, statusWidth, Alignment.ALIGN_NORMAL);
        if (detailsWidth > 0) {
            detailsLayout = makeLayout(TEXT_STATUS, detailsText, detailsWidth,
                    Alignment.ALIGN_OPPOSITE);
        } else {
            detailsLayout = null;
        }

        int leftHeight = VotingArrows.getHeight();
        rightHeight = titleLayout.getHeight() + ELEMENT_PADDING + statusLayout.getHeight();
        minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
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

    private Layout makeTitleLayout(int width) {
        return new StaticLayout(thing.title, TEXT_PAINTS[TEXT_TITLE], width,
                Alignment.ALIGN_NORMAL, 1f, 0f, true);
    }

    private static Layout makeLayout(int paint, CharSequence text, int width, Alignment alignment) {
        BoringLayout.Metrics m = BoringLayout.isBoring(text, TEXT_PAINTS[paint]);
        return BoringLayout.make(text, TEXT_PAINTS[paint], width, alignment, 1f, 0f, m, true,
                TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas c) {
        boolean hasThumb = thing.thumbnail != null;
        c.translate(PADDING, PADDING);
        VotingArrows.draw(c, thumb, hasThumb, scoreText, scoreBounds, thing.likes);
        c.translate(VotingArrows.getWidth() + PADDING, 0);

        if (hasThumb) {
            Thumbnail.draw(c, thumb, hasThumb);
            c.translate(Thumbnail.getWidth() + PADDING, 0);
        }

        int tdy = (minHeight - rightHeight) / 2;
        c.translate(0, -PADDING + tdy);
        titleLayout.draw(c);

        int sdy = titleLayout.getHeight() + ELEMENT_PADDING;
        c.translate(0, sdy);
        statusLayout.draw(c);
        c.translate(0, -sdy - tdy);

        if (detailsLayout != null) {
            int dx = c.getWidth() - PADDING - detailsLayout.getWidth();
            int dy = (c.getHeight() - detailsLayout.getHeight()) / 2;
            c.translate(dx, dy);
            detailsLayout.draw(c);
            c.translate(-dx, -dy);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector.onTouchEvent(event)) {
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    public boolean onDown(MotionEvent e) {
        return VotingArrows.onDown(e, thing.thumbnail != null, 0);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, thing.thumbnail != null, 0, listener, thing);
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
