package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.text.RelativeTime;

public class ThingView extends CustomView implements OnGestureListener {

    public static final String TAG = "ThingView";

    private final GestureDetector detector;
    private OnVoteListener listener;

    private int likes;
    private int thingBodyWidth;
    private String thumbnailUrl;
    private String thingId;
    private String title;

    private Bitmap bitmap;

    private String scoreText;
    private CharSequence statusText;
    private String longDetailsText;
    private String shortDetailsText;

    private Layout titleLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

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
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setThumbnailBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate();
    }

    public void setData(String author,
            long createdUtc,
            String domain,
            int downs,
            int likes,
            long nowTimeMs,
            int numComments,
            boolean over18,
            String parentSubreddit,
            int score,
            String subreddit,
            int thingBodyWidth,
            String thingId,
            String thumbnailUrl,
            String title,
            int ups) {

        this.likes = likes;
        this.thingBodyWidth = thingBodyWidth;
        this.thingId = thingId;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;

        this.scoreText = VotingArrows.getScoreText(score + likes);
        this.statusText = getStatusText(getContext(), author, createdUtc, nowTimeMs, numComments,
                over18, parentSubreddit, subreddit);
        this.longDetailsText = getContext().getString(R.string.thing_details, ups, downs, domain);
        this.shortDetailsText = domain;

        requestLayout();
    }

    private static CharSequence getStatusText(Context context, String author, long createdUtc,
            long nowTimeMs,
            int numComments, boolean over18, String parentSubreddit, String subreddit) {
        int resId = R.string.thing_status;
        if (!subreddit.equalsIgnoreCase(parentSubreddit)) {
            resId = R.string.thing_status_subreddit;
        }

        String nsfw = "";
        if (over18) {
            nsfw = context.getString(R.string.thing_nsfw);
        }

        String rt = RelativeTime.format(context, nowTimeMs, createdUtc);
        Resources r = context.getResources();
        String comments = r.getQuantityString(R.plurals.comments, numComments, numComments);

        CharSequence status = context.getString(resId, subreddit, author, rt, comments, nsfw);
        if (!nsfw.isEmpty()) {
            SpannableStringBuilder b = new SpannableStringBuilder(status);
            b.setSpan(new ForegroundColorSpan(Color.RED), 0, nsfw.length(), 0);
            status = b;
        }
        return status;
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

        VotingArrows.measureScoreText(scoreText, scoreBounds);

        int titleWidth;
        int detailsWidth;
        CharSequence detailsText;

        if (thingBodyWidth > 0) {
            titleWidth = Math.min(measuredWidth, thingBodyWidth) - PADDING * 2;
            int remainingWidth = measuredWidth - thingBodyWidth - PADDING * 2;
            if (remainingWidth > MAX_DETAILS_WIDTH) {
                detailsWidth = MAX_DETAILS_WIDTH;
                detailsText = longDetailsText;
            } else if (remainingWidth > MIN_DETAILS_WIDTH) {
                detailsWidth = MIN_DETAILS_WIDTH;
                detailsText = shortDetailsText;
            } else {
                detailsWidth = 0;
                detailsText = "";
            }
        } else {
            titleWidth = measuredWidth - PADDING * 2;
            detailsWidth = 0;
            detailsText = "";
        }

        int width = VotingArrows.getWidth() + PADDING;
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            width += PADDING + Thumbnail.getWidth();
        }
        titleWidth -= width;

        int statusWidth = measuredWidth - PADDING * 2;
        statusWidth -= width;
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;
        }

        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        detailsWidth = Math.max(0, detailsWidth);

        titleLayout = makeTitleLayout(titleWidth);
        statusLayout = makeLayout(THING_STATUS, statusText, statusWidth, Alignment.ALIGN_NORMAL);
        detailsLayout = null;
        if (detailsWidth > 0) {
            detailsLayout = makeLayout(THING_STATUS, detailsText, detailsWidth,
                    Alignment.ALIGN_OPPOSITE);
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
        return new StaticLayout(title, TEXT_PAINTS[THING_TITLE], width, Alignment.ALIGN_NORMAL, 1f,
                0f, true);
    }

    private static Layout makeLayout(int paint, CharSequence text, int width, Alignment alignment) {
        BoringLayout.Metrics m = BoringLayout.isBoring(text, TEXT_PAINTS[paint]);
        return BoringLayout.make(text, TEXT_PAINTS[paint], width, alignment, 1f, 0f, m, true,
                TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (detailsLayout != null) {
            int dx = c.getWidth() - PADDING - detailsLayout.getWidth();
            int dy = (c.getHeight() - detailsLayout.getHeight()) / 2;
            c.translate(dx, dy);
            detailsLayout.draw(c);
            c.translate(-dx, -dy);
        }

        boolean hasThumb = !TextUtils.isEmpty(thumbnailUrl);
        c.translate(PADDING, PADDING);
        VotingArrows.draw(c, bitmap, hasThumb, scoreText, scoreBounds, likes);
        c.translate(VotingArrows.getWidth() + PADDING, 0);

        if (hasThumb) {
            Thumbnail.draw(c, bitmap);
            c.translate(Thumbnail.getWidth() + PADDING, 0);
        }

        int tdy = (minHeight - rightHeight) / 2;
        c.translate(0, -PADDING + tdy);
        titleLayout.draw(c);

        int sdy = titleLayout.getHeight() + ELEMENT_PADDING;
        c.translate(0, sdy);
        statusLayout.draw(c);
        c.translate(0, -sdy - tdy);
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
        return VotingArrows.onDown(e, 0);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, 0, listener, thingId);
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
