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
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.text.RelativeTime;

public class ThingView extends CustomView implements OnGestureListener {

    public static final String TAG = "ThingView";

    private static final Formatter FORMATTER = new Formatter();

    private final GestureDetector detector;
    private OnVoteListener listener;

    private int kind;
    private int likes;
    private int thingBodyWidth;
    private String thumbnailUrl;
    private String thingId;
    private String title;

    private Bitmap bitmap;
    private boolean drawVotingArrows;

    private CharSequence bodyText;
    private String scoreText;
    private final SpannableStringBuilder statusText = new SpannableStringBuilder();
    private final SpannableStringBuilder longDetailsText = new SpannableStringBuilder();
    private String shortDetailsText;

    private Layout titleLayout;
    private Layout bodyLayout;
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

    public void setData(String accountName,
            String author,
            String body,
            long createdUtc,
            String domain,
            int downs,
            int kind,
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
        this.kind = kind;
        this.likes = likes;
        this.thingBodyWidth = thingBodyWidth;
        this.thingId = thingId;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;

        this.drawVotingArrows = AccountUtils.isAccount(accountName);
        setScoreText(score, drawVotingArrows);
        setStatusText(author, createdUtc, nowTimeMs, numComments, over18, parentSubreddit, score,
                subreddit, drawVotingArrows);
        setDetailsText(domain, downs, ups);

        if (!TextUtils.isEmpty(body)) {
            bodyText = FORMATTER.formatSpans(getContext(), body);
        } else {
            bodyText = null;
        }

        requestLayout();
    }

    private void setScoreText(int score, boolean votable) {
        // Only set the score text when there will be voting arrows.
        if (votable) {
            scoreText = VotingArrows.getScoreText(score);
        }
    }

    private void setStatusText(String author, long createdUtc, long nowTimeMs, int numComments,
            boolean over18, String parentSubreddit, int score, String subreddit, boolean votable) {
        Context c = getContext();
        Resources r = getResources();

        statusText.clear();
        statusText.clearSpans();

        if (over18) {
            String nsfw = c.getString(R.string.nsfw);
            statusText.append(nsfw).append("  ");
            statusText.setSpan(new ForegroundColorSpan(Color.RED), 0, nsfw.length(), 0);
        }

        if (!subreddit.equalsIgnoreCase(parentSubreddit)) {
            statusText.append(subreddit).append("  ");
        }

        statusText.append(author).append("  ");

        if (!votable) {
            statusText.append(r.getQuantityString(R.plurals.points, score, score)).append("  ");
        }

        statusText.append(RelativeTime.format(c, nowTimeMs, createdUtc)).append("  ");
        statusText.append(r.getQuantityString(R.plurals.comments, numComments, numComments));
    }

    private void setDetailsText(String domain, int downs, int ups) {
        Resources r = getResources();

        longDetailsText.clear();
        longDetailsText.append(r.getQuantityString(R.plurals.votes_up, ups, ups))
                .append("  ");
        longDetailsText.append(r.getQuantityString(R.plurals.votes_down, downs, downs))
                .append("  ");

        if (!TextUtils.isEmpty(domain)) {
            longDetailsText.append(domain);
            shortDetailsText = domain;
        } else {
            shortDetailsText = "";
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

        int width = 0;
        if (drawVotingArrows) {
            width += VotingArrows.getWidth(drawVotingArrows) + PADDING;
            VotingArrows.measureScoreText(scoreText, scoreBounds);
        }
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            width += Thumbnail.getWidth() + PADDING;
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

        titleLayout = null;
        bodyLayout = null;
        rightHeight = 0;

        if (!TextUtils.isEmpty(title)) {
            titleLayout = createTitleLayout(titleWidth);
            rightHeight += titleLayout.getHeight() + ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty(bodyText)) {
            bodyLayout = createBodyLayout(titleWidth);
            rightHeight += bodyLayout.getHeight() + ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty(statusText)) {
            statusLayout = makeLayout(THING_STATUS, statusText, statusWidth, Alignment.ALIGN_NORMAL);
            rightHeight += statusLayout.getHeight();
        }

        detailsLayout = null;
        if (detailsWidth > 0) {
            detailsLayout = makeLayout(THING_STATUS, detailsText, detailsWidth,
                    Alignment.ALIGN_OPPOSITE);
        }

        int leftHeight = Math.max(VotingArrows.getHeight(drawVotingArrows, true),
                Thumbnail.getHeight());
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

    private Layout createTitleLayout(int width) {
        return new StaticLayout(title, TEXT_PAINTS[THING_TITLE], width,
                Alignment.ALIGN_NORMAL, 1f, 0f, true);
    }

    private StaticLayout createBodyLayout(int width) {
        return new StaticLayout(bodyText, TEXT_PAINTS[THING_TITLE], width,
                Alignment.ALIGN_NORMAL, 1, 0, true);
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

        c.translate(PADDING, PADDING);
        if (drawVotingArrows) {
            VotingArrows.draw(c, bitmap, scoreText, scoreBounds, likes, true, true);
            c.translate(VotingArrows.getWidth(drawVotingArrows) + PADDING, 0);
        }

        if (!TextUtils.isEmpty(thumbnailUrl)) {
            Thumbnail.draw(c, bitmap);
            c.translate(Thumbnail.getWidth() + PADDING, 0);
        }

        c.translate(0, -PADDING + (minHeight - rightHeight) / 2);

        // Render the status at the top for comments.
        if (kind == Things.KIND_COMMENT && statusLayout != null) {
            statusLayout.draw(c);
            c.translate(0, statusLayout.getHeight() + ELEMENT_PADDING);
        }

        if (titleLayout != null) {
            titleLayout.draw(c);
            c.translate(0, titleLayout.getHeight() + ELEMENT_PADDING);
        }

        if (bodyLayout != null) {
            bodyLayout.draw(c);
            c.translate(0, bodyLayout.getHeight() + ELEMENT_PADDING);
        }

        // Render the status at the bottom for non-comments.
        if (kind != Things.KIND_COMMENT && statusLayout != null) {
            statusLayout.draw(c);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public boolean onDown(MotionEvent e) {
        return VotingArrows.onDown(e, 0, drawVotingArrows, true, true);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, 0, drawVotingArrows, true, true, listener, thingId);
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
