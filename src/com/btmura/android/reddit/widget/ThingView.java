package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.provider.VoteProvider.Votes;

public class ThingView extends View implements OnGestureListener {

    public static final String TAG = "ThingView";
    public static final boolean DEBUG = Debug.DEBUG;

    private static float FONT_SCALE;
    private static float DENSITY;

    private static int THUMB_WIDTH;
    private static int THUMB_HEIGHT;
    private static int PADDING;
    private static int ELEMENT_PADDING;
    private static int MIN_DETAILS_WIDTH;
    private static int MAX_DETAILS_WIDTH;
    private static int MAX_SCORE_WIDTH;

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 2;
    private static final int TEXT_TITLE = 0;
    private static final int TEXT_STATUS = 1;

    private static Paint THUMB_OUTLINE_PAINT;
    private static Paint SCORE_BUBBLE_PAINT;

    public interface ThingViewListener {
        void onVoteClick(Thing thing, int vote);
    }

    private final GestureDetector detector;
    private final Rect thumbSrc = new Rect();
    private final RectF thumbDst = new RectF();

    private int bodyWidth;
    private Thing thing;
    private Bitmap bitmap;

    private Layout titleLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

    private ThingViewListener listener;

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
        VotingArrows.init(context);
    }

    private void init(Context context) {
        Resources r = context.getResources();
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            DENSITY = r.getDisplayMetrics().density;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);
            THUMB_WIDTH = r.getDimensionPixelSize(R.dimen.max_thumb_width);
            THUMB_HEIGHT = THUMB_WIDTH;
            MIN_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.min_details_width);
            MAX_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.max_details_width);
            MAX_SCORE_WIDTH = r.getDimensionPixelSize(R.dimen.max_score_width);

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

            SCORE_BUBBLE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            SCORE_BUBBLE_PAINT.setColor(r.getColor(R.color.score_bg));

            THUMB_OUTLINE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_OUTLINE_PAINT.setColor(r.getColor(android.R.color.darker_gray));
            THUMB_OUTLINE_PAINT.setStyle(Style.STROKE);
        }
    }

    public void setThingViewListener(ThingViewListener listener) {
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
        this.bitmap = bitmap;
        if (bitmap != null) {
            thumbSrc.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
            thumbDst.set(0, 0, bitmap.getWidth() * DENSITY, bitmap.getHeight() * DENSITY);
        } else {
            thumbSrc.set(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
            thumbDst.set(0, 0, THUMB_WIDTH, THUMB_HEIGHT);
        }
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

        int statusWidth = measuredWidth - PADDING * 2;
        statusWidth -= VotingArrows.ARROW_TOTAL_WIDTH + PADDING;
        titleWidth -= VotingArrows.ARROW_TOTAL_WIDTH + PADDING;
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;
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
    protected void onDraw(Canvas canvas) {
        canvas.translate(PADDING, PADDING);
        VotingArrows.draw(canvas, scoreText, scoreBounds);
        canvas.translate(0, -PADDING);

        int tdx = VotingArrows.ARROW_TOTAL_WIDTH + PADDING;
        int tdy = (minHeight - rightHeight) / 2;
        canvas.translate(tdx, tdy);
        titleLayout.draw(canvas);

        int sdy = titleLayout.getHeight() + ELEMENT_PADDING;
        canvas.translate(0, sdy);
        statusLayout.draw(canvas);
        canvas.translate(-tdx, -sdy - tdy);

        if (detailsLayout != null) {
            int x = canvas.getWidth() - PADDING - detailsLayout.getWidth();
            int y = (canvas.getHeight() - detailsLayout.getHeight()) / 2;
            canvas.translate(x, y);
            detailsLayout.draw(canvas);
            canvas.translate(-x, -y);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(TAG, "onTouchEvent");
        }
        if (detector.onTouchEvent(event)) {
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DEBUG) {
            Log.d(TAG, "onKeyDown");
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (DEBUG) {
            Log.d(TAG, "onKeyUp");
        }
        return super.onKeyUp(keyCode, event);
    }

    // OnGestureDetectorListener

    public boolean onDown(MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onDown x:" + e.getX() + " y:" + e.getY());
        }
        int right = getVoteRight();
        int bottom = getVoteBottom();
        boolean consumed = e.getX() < right && e.getY() < bottom;
        if (DEBUG && consumed) {
            Log.d(TAG, "onDown consumed event");
        }
        return consumed;
    }

    public boolean onSingleTapUp(MotionEvent e) {
        if (DEBUG) {
            Log.d(TAG, "onSingleTapUp x:" + e.getX() + " y:" + e.getY());
        }
        if (listener != null) {
            int vote = e.getY() < getVoteCenterY() ? Votes.VOTE_UP : Votes.VOTE_DOWN;
            listener.onVoteClick(thing, vote);
        }
        return true;
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

    private int getVoteRight() {
        int voteRight = PADDING * 2;
        if (thing.thumbnail != null) {
            voteRight += THUMB_WIDTH;
        } else {
            voteRight += MAX_SCORE_WIDTH;
        }
        return voteRight;
    }

    private int getVoteBottom() {
        int voteBottom = 0;
        // int voteBottom = PADDING + (ARROW_TOTAL_HEIGHT + ARROW_SPACING) * 2;
        if (thing.thumbnail != null) {
            voteBottom += THUMB_HEIGHT;
        } else {
            voteBottom += scoreBounds.height();
        }
        return voteBottom;
    }

    private int getVoteCenterY() {
        int center = 0;
        // int center = PADDING + ARROW_TOTAL_HEIGHT + ARROW_SPACING;
        if (thing.thumbnail != null) {
            center += THUMB_HEIGHT / 2;
        } else {
            center += scoreBounds.height() / 2;
        }
        return center;
    }
}
