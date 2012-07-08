package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
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
    private static int SCORE_BUBBLE_PADDING;
    private static int MIN_DETAILS_WIDTH;
    private static int MAX_DETAILS_WIDTH;
    private static int MAX_SCORE_WIDTH;
    private static int ROUNDED_RADIUS;

    private static TextPaint[] TEXT_PAINTS;
    private static final int NUM_TEXT_PAINTS = 5;
    private static final int TITLE = 0;
    private static final int STATUS = 1;
    private static final int NEUTRAL_SCORE = 2;
    private static final int UP_SCORE = 3;
    private static final int DOWN_SCORE = 4;

    private static Paint[] VOTE_PAINTS;
    private static final int NUM_VOTE_PAINTS = 3;
    private static final int NEUTRAL = 0;
    private static final int UP = 1;
    private static final int DOWN = 2;

    private static int ARROW_TOTAL_WIDTH;
    private static int ARROW_TOTAL_HEIGHT;
    private static int ARROW_SPACING;

    private static Paint THUMB_OUTLINE_PAINT;
    private static Paint SCORE_BUBBLE_PAINT;

    private static Path UPVOTE_PATH;
    private static Path DOWNVOTE_PATH;

    public interface OnThingViewClickListener {
        void onVoteArrowClick(int vote);
    }

    private final GestureDetector detector;
    private final Rect thumbSrc = new Rect();
    private final RectF thumbDst = new RectF();
    private final RectF bubbleRect = new RectF();

    private int bodyWidth;
    private Thing thing;
    private Bitmap bitmap;

    private Layout scoreLayout;
    private Layout titleLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

    private OnThingViewClickListener listener;

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
        Resources r = context.getResources();
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            DENSITY = r.getDisplayMetrics().density;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);
            SCORE_BUBBLE_PADDING = r.getDimensionPixelSize(R.dimen.score_bubble_padding);
            THUMB_WIDTH = r.getDimensionPixelSize(R.dimen.max_thumb_width);
            THUMB_HEIGHT = THUMB_WIDTH;
            MIN_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.min_details_width);
            MAX_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.max_details_width);
            MAX_SCORE_WIDTH = r.getDimensionPixelSize(R.dimen.max_score_width);
            ROUNDED_RADIUS = r.getDimensionPixelSize(R.dimen.rounded_radius);

            Theme t = context.getTheme();
            int[] styles = new int[] {
                    R.style.ThingTitleText,
                    R.style.ThingStatusText,
                    R.style.ThingNeutralScoreText,
                    R.style.ThingUpScoreText,
                    R.style.ThingDownScoreText,
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

            int[] colorResIds = new int[] {
                    R.color.score_bg,
                    android.R.color.holo_orange_dark,
                    android.R.color.holo_blue_dark,
            };
            VOTE_PAINTS = new Paint[NUM_VOTE_PAINTS];
            for (int i = 0; i < NUM_VOTE_PAINTS; i++) {
                VOTE_PAINTS[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                VOTE_PAINTS[i].setColor(r.getColor(colorResIds[i]));
            }

            ARROW_TOTAL_WIDTH = r.getDimensionPixelSize(R.dimen.arrow_total_width);
            ARROW_TOTAL_HEIGHT = r.getDimensionPixelSize(R.dimen.arrow_total_height);
            ARROW_SPACING = r.getDimensionPixelSize(R.dimen.arrow_spacing);

            int arrowHeadHeight = r.getDimensionPixelSize(R.dimen.arrow_head_height);
            int arrowStemWidth = r.getDimensionPixelSize(R.dimen.arrow_stem_width);
            int arrowStemIndent = (ARROW_TOTAL_WIDTH - arrowStemWidth) / 2;

            UPVOTE_PATH = new Path();
            UPVOTE_PATH.moveTo(ARROW_TOTAL_WIDTH / 2, 0);
            UPVOTE_PATH.lineTo(ARROW_TOTAL_WIDTH, arrowHeadHeight);
            UPVOTE_PATH.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, arrowHeadHeight);
            UPVOTE_PATH.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, ARROW_TOTAL_HEIGHT);
            UPVOTE_PATH.lineTo(arrowStemIndent, ARROW_TOTAL_HEIGHT);
            UPVOTE_PATH.lineTo(arrowStemIndent, arrowHeadHeight);
            UPVOTE_PATH.lineTo(0, arrowHeadHeight);
            UPVOTE_PATH.close();

            Matrix flip = new Matrix();
            flip.setRotate(180, ARROW_TOTAL_WIDTH / 2, ARROW_TOTAL_HEIGHT / 2);

            DOWNVOTE_PATH = new Path();
            DOWNVOTE_PATH.set(UPVOTE_PATH);
            DOWNVOTE_PATH.transform(flip);
            DOWNVOTE_PATH.close();

            SCORE_BUBBLE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            SCORE_BUBBLE_PAINT.setColor(r.getColor(R.color.score_bg));

            THUMB_OUTLINE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
            THUMB_OUTLINE_PAINT.setColor(r.getColor(android.R.color.darker_gray));
            THUMB_OUTLINE_PAINT.setStyle(Style.STROKE);
        }
    }

    public void setOnThingViewClickListener(OnThingViewClickListener listener) {
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
        if (this.bitmap != bitmap) {
            this.bitmap = bitmap;
            if (bitmap != null) {
                this.thumbSrc.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                this.thumbDst.set(0, 0, bitmap.getWidth() * DENSITY, bitmap.getHeight() * DENSITY);
            }
            invalidate();
        }
    }

    public void removeThumbnail() {
        if (bitmap != null) {
            bitmap = null;
            requestLayout();
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

        int scoreWidth;
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
        if (thing.thumbnail != null) {
            titleWidth -= THUMB_WIDTH + PADDING;
            statusWidth -= THUMB_WIDTH + PADDING;
            scoreWidth = THUMB_WIDTH;
        } else {
            titleWidth -= MAX_SCORE_WIDTH + PADDING;
            statusWidth -= MAX_SCORE_WIDTH + PADDING;
            scoreWidth = MAX_SCORE_WIDTH;
        }
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;
        }

        scoreWidth = Math.max(0, scoreWidth);
        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        detailsWidth = Math.max(0, detailsWidth);

        int paint = NEUTRAL_SCORE;
        if (thing.likes == Votes.VOTE_UP) {
            paint = UP_SCORE;
        } else if (thing.likes == Votes.VOTE_DOWN) {
            paint = DOWN_SCORE;
        }
        scoreLayout = makeLayout(paint, Integer.toString(thing.score), scoreWidth,
                Alignment.ALIGN_CENTER);
        titleLayout = makeTitleLayout(titleWidth);
        statusLayout = makeLayout(STATUS, thing.status, statusWidth, Alignment.ALIGN_NORMAL);

        if (detailsWidth > 0) {
            detailsLayout = makeLayout(STATUS, detailsText, detailsWidth, Alignment.ALIGN_OPPOSITE);
        } else {
            detailsLayout = null;
        }

        int leftHeight = (ARROW_TOTAL_HEIGHT + ARROW_SPACING) * 2;
        if (thing.thumbnail != null) {
            leftHeight += THUMB_HEIGHT;
        } else {
            leftHeight += scoreLayout.getHeight();
        }
        int rightHeight = titleLayout.getHeight() + ELEMENT_PADDING + statusLayout.getHeight();
        int minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

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
        return new StaticLayout(thing.title, TEXT_PAINTS[TITLE], width, Alignment.ALIGN_NORMAL, 1f,
                0f,
                true);
    }

    private static Layout makeLayout(int paint, CharSequence text, int width, Alignment alignment) {
        BoringLayout.Metrics m = BoringLayout.isBoring(text, TEXT_PAINTS[paint]);
        return BoringLayout.make(text, TEXT_PAINTS[paint], width, alignment, 1f, 0f, m, true,
                TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(PADDING, PADDING);

        int arrowX = -ARROW_TOTAL_WIDTH / 2;
        boolean hasThumbnail = thing.thumbnail != null;
        if (hasThumbnail) {
            arrowX += THUMB_WIDTH / 2;
        } else {
            arrowX += MAX_SCORE_WIDTH / 2;
        }

        Paint upPaint = VOTE_PAINTS[NEUTRAL];
        Paint downPaint = VOTE_PAINTS[NEUTRAL];
        if (thing.likes == Votes.VOTE_UP) {
            upPaint = VOTE_PAINTS[UP];
        } else if (thing.likes == Votes.VOTE_DOWN) {
            downPaint = VOTE_PAINTS[DOWN];
        }

        canvas.translate(arrowX, 0);
        canvas.drawPath(UPVOTE_PATH, upPaint);

        canvas.translate(-arrowX, ARROW_TOTAL_HEIGHT + ARROW_SPACING);
        if (hasThumbnail) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, thumbSrc, thumbDst, THUMB_OUTLINE_PAINT);
            } else {
                canvas.drawRoundRect(thumbDst, ROUNDED_RADIUS, ROUNDED_RADIUS, THUMB_OUTLINE_PAINT);
            }

            int bubbleLoc = THUMB_HEIGHT - scoreLayout.getHeight() - SCORE_BUBBLE_PADDING;
            canvas.translate(0, bubbleLoc);
            bubbleRect.set(scoreLayout.getLineLeft(0) - SCORE_BUBBLE_PADDING,
                    scoreLayout.getLineTop(0),
                    scoreLayout.getLineRight(0) + SCORE_BUBBLE_PADDING,
                    scoreLayout.getLineBottom(0));
            canvas.drawRoundRect(bubbleRect, ROUNDED_RADIUS, ROUNDED_RADIUS,
                    SCORE_BUBBLE_PAINT);
            scoreLayout.draw(canvas);
            canvas.translate(0, -bubbleLoc);

            canvas.translate(0, THUMB_HEIGHT);
        } else {
            scoreLayout.draw(canvas);
            canvas.translate(0, scoreLayout.getHeight());
        }

        canvas.translate(arrowX, ARROW_SPACING);
        canvas.drawPath(DOWNVOTE_PATH, downPaint);
        canvas.restore();

        canvas.save();

        if (detailsLayout != null) {
            int x = canvas.getWidth() - PADDING - detailsLayout.getWidth();
            int y = (canvas.getHeight() - detailsLayout.getHeight()) / 2;
            canvas.translate(x, y);
            detailsLayout.draw(canvas);
            canvas.translate(-x, -y);
        }

        int titleX = PADDING * 2;
        if (hasThumbnail) {
            titleX += THUMB_WIDTH;
        } else {
            titleX += MAX_SCORE_WIDTH;
        }

        int rightHeight = titleLayout.getHeight() + ELEMENT_PADDING + statusLayout.getHeight();
        int titleY = canvas.getHeight() / 2 - rightHeight / 2;

        canvas.translate(titleX, titleY);
        titleLayout.draw(canvas);

        canvas.translate(0, titleLayout.getHeight() + ELEMENT_PADDING);
        statusLayout.draw(canvas);

        canvas.restore();
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
            listener.onVoteArrowClick(vote);
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
        int voteBottom = PADDING + (ARROW_TOTAL_HEIGHT + ARROW_SPACING) * 2;
        if (thing.thumbnail != null) {
            voteBottom += THUMB_HEIGHT;
        } else {
            voteBottom += scoreLayout.getHeight();
        }
        return voteBottom;
    }

    private int getVoteCenterY() {
        int center = PADDING + ARROW_TOTAL_HEIGHT + ARROW_SPACING;
        if (thing.thumbnail != null) {
            center += THUMB_HEIGHT / 2;
        } else {
            center += scoreLayout.getHeight() / 2;
        }
        return center;
    }
}
