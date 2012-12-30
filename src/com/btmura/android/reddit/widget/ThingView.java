package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.text.RelativeTime;

public class ThingView extends CustomView implements OnGestureListener {

    public static final String TAG = "ThingView";

    // TODO: Fix thread safety issue here.
    private static final Formatter FORMATTER = new Formatter();

    private final GestureDetector detector;
    private OnVoteListener listener;

    private boolean expanded;
    private int kind;
    private int likes;
    private int nesting;
    private String linkTitle;
    private int thingBodyWidth;
    private String thumbnailUrl;
    private String thingId;
    private String title;

    private Bitmap bitmap;
    private boolean drawVotingArrows;
    private boolean drawScore;

    private CharSequence bodyText;
    private String scoreText;
    private final SpannableStringBuilder statusText = new SpannableStringBuilder();
    private StyleSpan italicSpan;
    private final SpannableStringBuilder longDetailsText = new SpannableStringBuilder();
    private String shortDetailsText;

    private Layout linkTitleLayout;
    private Layout titleLayout;
    private Layout bodyLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

    private Rect scoreBounds;
    private RectF bodyBounds;
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
            boolean expanded,
            int kind,
            int likes,
            String linkTitle,
            int nesting,
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
        this.expanded = expanded;
        this.kind = kind;
        this.nesting = nesting;
        this.likes = likes;
        this.linkTitle = linkTitle;
        this.thingBodyWidth = thingBodyWidth;
        this.thingId = thingId;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;

        drawVotingArrows = AccountUtils.isAccount(accountName)
                && kind != Kinds.KIND_MESSAGE
                && expanded;
        drawScore = drawVotingArrows && kind == Kinds.KIND_LINK;
        if (drawScore) {
            if (scoreBounds == null) {
                scoreBounds = new Rect();
            }
            scoreText = VotingArrows.getScoreText(score);
        }

        boolean showSubreddit = !TextUtils.isEmpty(subreddit)
                && !subreddit.equalsIgnoreCase(parentSubreddit);
        boolean showPoints = !drawScore && kind != Kinds.KIND_MESSAGE;

        boolean showNumComments = kind == Kinds.KIND_LINK;
        setStatusText(over18, showSubreddit, showPoints, showNumComments,
                author, createdUtc, nowTimeMs, numComments, score, subreddit);

        boolean showUpsDowns = kind == Kinds.KIND_LINK;
        setDetailsText(showUpsDowns, domain, downs, ups);

        if (!TextUtils.isEmpty(body)) {
            bodyText = FORMATTER.formatSpans(getContext(), body);
            if (bodyBounds == null) {
                bodyBounds = new RectF();
            }
        } else {
            bodyText = null;
        }

        requestLayout();
    }

    private void setStatusText(boolean showNsfw, boolean showSubreddit, boolean showPoints,
            boolean showNumComments, String author, long createdUtc, long nowTimeMs,
            int numComments, int score, String subreddit) {
        Context c = getContext();
        Resources r = getResources();

        statusText.clear();
        statusText.clearSpans();

        if (showNsfw) {
            String nsfw = c.getString(R.string.nsfw);
            statusText.append(nsfw).append("  ");
            statusText.setSpan(new ForegroundColorSpan(Color.RED), 0, nsfw.length(), 0);
        }

        if (showSubreddit) {
            statusText.append(subreddit).append("  ");
        }

        statusText.append(author).append("  ");

        if (showPoints) {
            statusText.append(r.getQuantityString(R.plurals.points, score, score)).append("  ");
        }

        statusText.append(RelativeTime.format(c, nowTimeMs, createdUtc)).append("  ");

        if (showNumComments) {
            statusText.append(r.getQuantityString(R.plurals.comments, numComments, numComments));
        }

        if (!expanded) {
            if (italicSpan == null) {
                italicSpan = new StyleSpan(Typeface.ITALIC);
            }
            statusText.setSpan(italicSpan, 0, statusText.length(), 0);
        }
    }

    private void setDetailsText(boolean showUpsDowns, String domain, int downs, int ups) {
        Resources r = getResources();

        longDetailsText.clear();
        if (showUpsDowns) {
            longDetailsText.append(r.getQuantityString(R.plurals.votes_up, ups, ups))
                    .append("  ");
            longDetailsText.append(r.getQuantityString(R.plurals.votes_down, downs, downs))
                    .append("  ");
        }

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

        int linkTitleWidth;
        int titleWidth;
        int detailsWidth;
        CharSequence detailsText;

        int totalPadding = PADDING * (2 + nesting);
        int contentWidth = measuredWidth - totalPadding;

        if (thingBodyWidth > 0) {
            linkTitleWidth = titleWidth = Math.min(measuredWidth, thingBodyWidth) - totalPadding;
            int remainingWidth = measuredWidth - thingBodyWidth - totalPadding;
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
            linkTitleWidth = titleWidth = contentWidth;
            detailsWidth = 0;
            detailsText = "";
        }

        int leftGadgetWidth = 0;
        if (drawVotingArrows) {
            leftGadgetWidth += VotingArrows.getWidth(drawVotingArrows) + PADDING;
            if (drawScore) {
                VotingArrows.measureScoreText(scoreText, scoreBounds);
            }
        }
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            leftGadgetWidth += Thumbnail.getWidth() + PADDING;
        }
        titleWidth -= leftGadgetWidth;

        int statusWidth = contentWidth - leftGadgetWidth;
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;
        }

        linkTitleWidth = Math.max(0, linkTitleWidth);
        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        detailsWidth = Math.max(0, detailsWidth);

        int leftHeight = 0;
        if (drawVotingArrows) {
            leftHeight = Math.max(leftHeight, VotingArrows.getHeight(drawVotingArrows, drawScore));
        }
        if (kind == Kinds.KIND_LINK) {
            leftHeight = Math.max(leftHeight, Thumbnail.getHeight());
        }

        linkTitleLayout = null;
        titleLayout = null;
        bodyLayout = null;
        rightHeight = 0;

        if (expanded && !TextUtils.isEmpty(linkTitle)) {
            linkTitleLayout = createLinkTitleLayout(linkTitleWidth);
            rightHeight += linkTitleLayout.getHeight() + ELEMENT_PADDING;
        }

        if (expanded && !TextUtils.isEmpty(title)) {
            titleLayout = createTitleLayout(titleWidth);
            rightHeight += titleLayout.getHeight() + ELEMENT_PADDING;
        }

        if (expanded && !TextUtils.isEmpty(bodyText)) {
            bodyLayout = createBodyLayout(titleWidth);
            rightHeight += bodyLayout.getHeight() + ELEMENT_PADDING;
        }

        if (!TextUtils.isEmpty(statusText)) {
            statusLayout = createStatusLayout(statusWidth);
            rightHeight += statusLayout.getHeight();
        }

        detailsLayout = null;
        if (detailsWidth > 0) {
            detailsLayout = makeBoringLayout(THING_STATUS, detailsText, detailsWidth,
                    Alignment.ALIGN_OPPOSITE);
        }

        minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

        // Move from left to right one more time.
        int x = PADDING;
        if (drawVotingArrows) {
            x += VotingArrows.getWidth(drawVotingArrows);
        }
        if (bodyLayout != null) {
            bodyBounds.left = x;
            x += bodyLayout.getWidth();
            bodyBounds.right = x;
        }

        // Move from top to bottom one more time.
        int y = (minHeight - rightHeight) / 2;

        if (linkTitleLayout != null) {
            y += linkTitleLayout.getHeight() + ELEMENT_PADDING;
        }

        if (isTopStatus() && statusLayout != null) {
            y += statusLayout.getHeight() + ELEMENT_PADDING;
        }

        if (bodyLayout != null) {
            bodyBounds.top = y;
            y += bodyLayout.getHeight();
            bodyBounds.bottom = y;
        }

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

    private boolean isTopStatus() {
        return kind == Kinds.KIND_COMMENT;
    }

    private Layout createLinkTitleLayout(int width) {
        CharSequence truncated = TextUtils.ellipsize(linkTitle,
                TEXT_PAINTS[THING_LINK_TITLE], width, TruncateAt.END);
        return makeStaticLayout(THING_LINK_TITLE, truncated, width);
    }

    private Layout createTitleLayout(int width) {
        return makeStaticLayout(THING_TITLE, title, width);
    }

    private Layout createBodyLayout(int width) {
        return makeStaticLayout(THING_BODY, bodyText, width);
    }

    private Layout createStatusLayout(int width) {
        return makeBoringLayout(THING_STATUS, statusText, width, Alignment.ALIGN_NORMAL);
    }

    private static Layout makeStaticLayout(int paint, CharSequence text, int width) {
        return new StaticLayout(text, TEXT_PAINTS[paint], width,
                Alignment.ALIGN_NORMAL, 1f, 0f, true);
    }

    private static Layout makeBoringLayout(int paint, CharSequence text, int width,
            Alignment alignment) {
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

        c.translate(PADDING * (1 + nesting), PADDING);

        if (linkTitleLayout != null) {
            linkTitleLayout.draw(c);
            c.translate(0, linkTitleLayout.getHeight() + ELEMENT_PADDING);
        }

        if (drawVotingArrows) {
            VotingArrows.draw(c, bitmap, scoreText, scoreBounds, likes, drawScore, true);
            c.translate(VotingArrows.getWidth(drawVotingArrows) + PADDING, 0);
        }

        if (!TextUtils.isEmpty(thumbnailUrl)) {
            Thumbnail.draw(c, bitmap);
            c.translate(Thumbnail.getWidth() + PADDING, 0);
        }

        c.translate(0, -PADDING + (minHeight - rightHeight) / 2);

        // Render the status at the top for comments.
        if (isTopStatus() && statusLayout != null) {
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
        if (!isTopStatus() && statusLayout != null) {
            statusLayout.draw(c);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e) || onBodyTouchEvent(e) || super.onTouchEvent(e);
    }

    private boolean onBodyTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                && bodyText instanceof Spannable
                && bodyBounds != null
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
        return VotingArrows.onDown(e, getTopOffset(), getLeftOffset(),
                drawVotingArrows, drawScore, true);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return VotingArrows.onSingleTapUp(e, getTopOffset(), getLeftOffset(),
                drawVotingArrows, drawScore, true, listener, thingId, likes);
    }

    private float getTopOffset() {
        return linkTitleLayout != null ? linkTitleLayout.getHeight() + ELEMENT_PADDING : 0;
    }

    private float getLeftOffset() {
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
