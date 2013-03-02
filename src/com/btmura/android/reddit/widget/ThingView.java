package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
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
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.text.Formatter;
import com.btmura.android.reddit.text.RelativeTime;

public class ThingView extends CustomView implements OnGestureListener {

    public static final String TAG = "ThingView";

    /** Type of ThingView used when listing a subreddit's things. */
    public static final int TYPE_THING_LIST = 0;

    /** Type of ThingView used when listing the comments on some thing. */
    public static final int TYPE_COMMENT_LIST = 1;

    /** Type of ThingView used when listing the messages in one thread. */
    public static final int TYPE_MESSAGE_THREAD_LIST = 2;

    /** Detail for showing the number of up votes like "5 ups". */
    public static final int DETAIL_UP_VOTES = 0;

    /** Detail for showing the number of down votes like "5 downs". */
    public static final int DETAIL_DOWN_VOTES = 1;

    /** Detail for showing the domain like "i.imgur.com". */
    public static final int DETAIL_DOMAIN = 2;

    /** Detail for showing the subreddit like "AskReddit". */
    public static final int DETAIL_SUBREDDIT = 3;

    /** Detail for showing th. author like "btmura". */
    public static final int DETAIL_AUTHOR = 4;

    /** Detail for showing the timestamp like "4 days ago." */
    public static final int DETAIL_TIMESTAMP = 5;

    /** Detail for showing the destination like "rbbtest2". */
    public static final int DETAIL_DESTINATION = 6;

    /** Maximum number of details to allocate space for internally. */
    private static final int MAX_INTERNAL_DETAILS = 3;

    /** Maximum width of the content inside a details cell. */
    private static int DETAILS_INNER_CELL_WIDTH;

    /** Number of formatters to build quantity strings. First three correspond to details. */
    private static final int NUM_FORMATTERS = 4;

    /** Formatter for building quantities for the status line. */
    private static final int FORMATTER_STATUS = 3;

    private final GestureDetector detector;
    private OnVoteListener listener;

    private String author;
    private long createdUtc;
    private String destination;
    private String domain;
    private int downs;
    private boolean expanded;
    private int kind;
    private int likes;
    private int nesting;
    private long nowTimeMs;
    private String linkTitle;
    private String subreddit;
    private int thingBodyWidth;
    private String title;
    private int ups;

    private boolean showThumbnail;
    private BitmapShader thumbShader;
    private Matrix thumbMatrix;
    private Rect thumbRect;

    private boolean drawVotingArrows;
    private boolean drawScore;
    private boolean isVotable;

    private CharSequence bodyText;
    private String scoreText;
    private final SpannableStringBuilder statusText = new SpannableStringBuilder();
    private StyleSpan italicSpan;

    private int linkTitlePaint;
    private int titlePaint;
    private int bodyPaint;
    private int statusPaint;

    private Layout linkTitleLayout;
    private Layout titleLayout;
    private Layout bodyLayout;
    private BoringLayout.Metrics statusMetrics;
    private BoringLayout statusLayout;

    private Rect scoreBounds;
    private RectF bodyBounds;
    private int rightHeight;
    private int minHeight;

    /** Array of Formatters used for making quantity strings and relative times. */
    private java.util.Formatter[] formatters;

    /** Array of StringBuilder objects backing the array of Formatters. */
    private StringBuilder[] formatterData;

    /** Array of details types to be rendered in the extra space. */
    private int[] details;

    /** Number of details that we measured will fit in the extra space. */
    private int numFittingDetails;

    /** Array of reusable metrics corresponding to each cell. */
    private BoringLayout.Metrics[] detailMetrics;

    /** Array of reusable layouts corresponding to each cell. */
    private BoringLayout[] detailLayouts;

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
        setType(TYPE_THING_LIST);
        DETAILS_INNER_CELL_WIDTH = DETAILS_CELL_WIDTH - ELEMENT_PADDING * 2;
    }

    public void setOnVoteListener(OnVoteListener listener) {
        this.listener = listener;
    }

    public void setThumbnailBitmap(Bitmap bitmap) {
        BitmapShader currentShader = thumbShader;
        if (bitmap != null) {
            if (thumbMatrix == null) {
                thumbMatrix = new Matrix();
            }
            thumbShader = Thumbnail.newBitmapShader(bitmap, thumbMatrix);
        } else {
            thumbShader = null;
        }

        int newLeft = PADDING;
        if (drawVotingArrows) {
            newLeft += VotingArrows.getWidth(drawVotingArrows) + PADDING;
        }
        if (thumbRect == null) {
            thumbRect = new Rect();
        }
        Thumbnail.setBounds(thumbRect, newLeft, PADDING);

        if (currentShader != thumbShader) {
            if (showThumbnail) {
                invalidate(thumbRect);
            } else {
                invalidate();
            }
        }
    }

    public void setType(int type) {
        // TODO: Clean up styles for the different types.
        switch (type) {
            case TYPE_MESSAGE_THREAD_LIST: // No style of its own yet.
            case TYPE_COMMENT_LIST:
                linkTitlePaint = THING_LINK_TITLE; // No style yet.
                titlePaint = COMMENT_TITLE;
                bodyPaint = COMMENT_BODY;
                statusPaint = COMMENT_STATUS;
                break;

            default:
                linkTitlePaint = THING_LINK_TITLE;
                titlePaint = THING_TITLE;
                bodyPaint = THING_BODY;
                statusPaint = THING_STATUS;
                break;
        }
    }

    public void setBody(String body, boolean isNew, Formatter formatter) {
        if (!TextUtils.isEmpty(body)) {
            bodyText = formatter.formatSpans(getContext(), body);
            if (bodyBounds == null) {
                bodyBounds = new RectF();
            }
        } else {
            bodyText = null;
        }

        bodyPaint = isNew ? THING_NEW_BODY : THING_BODY;
        requestLayout();
    }

    public void setData(String accountName,
            String author,
            long createdUtc,
            String destination,
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
            int ups,
            boolean drawVotingArrows,
            boolean showThumbnail,
            boolean showStatusPoints) {

        // Save the attributes needed by onMeasure or may be used to construct
        // details if we discover extra space while measuring.
        this.author = author;
        this.createdUtc = createdUtc;
        this.destination = destination;
        this.domain = domain;
        this.downs = downs;
        this.expanded = expanded;
        this.kind = kind;
        this.nesting = nesting;
        this.nowTimeMs = nowTimeMs;
        this.likes = likes;
        this.linkTitle = linkTitle;
        this.subreddit = subreddit;
        this.thingBodyWidth = thingBodyWidth;
        this.title = title;
        this.ups = ups;

        this.drawVotingArrows = drawVotingArrows;
        isVotable = drawVotingArrows && !TextUtils.isEmpty(thingId) && expanded;
        drawScore = drawVotingArrows && kind == Kinds.KIND_LINK;
        if (drawScore) {
            if (scoreBounds == null) {
                scoreBounds = new Rect();
            }
            scoreText = VotingArrows.getScoreText(score);
        }

        this.showThumbnail = showThumbnail;

        boolean showSubreddit = !TextUtils.isEmpty(subreddit)
                && !subreddit.equalsIgnoreCase(parentSubreddit);

        boolean showNumComments = kind == Kinds.KIND_LINK;
        setStatusText(over18, showSubreddit, showStatusPoints, showNumComments,
                author, createdUtc, nowTimeMs, numComments, score, subreddit);

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
            statusText.append(getQuantityString(r, R.plurals.points, score,
                    FORMATTER_STATUS)).append("  ");
        }

        if (createdUtc != 0) {
            statusText.append(getRelativeTime(r, nowTimeMs, createdUtc,
                    FORMATTER_STATUS)).append("  ");
        }

        if (showNumComments) {
            statusText.append(getQuantityString(r, R.plurals.comments, numComments,
                    FORMATTER_STATUS));
        }

        if (!expanded) {
            if (italicSpan == null) {
                italicSpan = new StyleSpan(Typeface.ITALIC);
            }
            statusText.setSpan(italicSpan, 0, statusText.length(), 0);
        }
    }

    private CharSequence getQuantityString(Resources resources, int resId, int quantity,
            int formatterIndex) {
        java.util.Formatter formatter = resetFormatter(formatterIndex, 20);
        String format = resources.getQuantityText(resId, quantity).toString();
        formatter.format(resources.getConfiguration().locale, format, quantity);
        return formatterData[formatterIndex];
    }

    private CharSequence getRelativeTime(Resources resources, long nowTimeMs, long createdUtc,
            int formatterIndex) {
        java.util.Formatter formatter = resetFormatter(formatterIndex, 15);
        RelativeTime.format(resources, formatter, nowTimeMs, createdUtc);
        return formatterData[formatterIndex];
    }

    private java.util.Formatter resetFormatter(int index, int expectedSize) {
        if (formatters == null) {
            formatterData = new StringBuilder[NUM_FORMATTERS];
            formatters = new java.util.Formatter[NUM_FORMATTERS];
        }
        if (formatters[index] == null) {
            formatterData[index] = new StringBuilder(expectedSize);
            formatters[index] = new java.util.Formatter(formatterData[index]);
        }
        formatterData[index].delete(0, formatterData[index].length());
        return formatters[index];
    }

    /**
     * Sets the type of details to be rendered in the extra space from left to right. Callers should
     * use other setters to set the fields necessary for the details to render.
     */
    public void setDetails(int[] details) {
        this.details = details;
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

        // Total outer padding of left and right sides.
        // Also includes additional padding for nested comments.
        int outerPadding = PADDING * (2 + nesting);

        // Width to fit our content inside of without padding.
        int contentWidth = measuredWidth - outerPadding;

        // Total width of all the extra details put together.
        int totalDetailsWidth = 0;

        int linkTitleWidth;
        int titleWidth;

        if (thingBodyWidth > 0) {
            linkTitleWidth = titleWidth = Math.min(measuredWidth, thingBodyWidth) - outerPadding;

            // Calculate and setup the details we can fit in the extra space.
            numFittingDetails = 0;
            int remainingWidth = measuredWidth - thingBodyWidth - outerPadding;
            int maxDetails = details.length;
            for (int i = 0; i < maxDetails; i++) {
                if (remainingWidth < DETAILS_CELL_WIDTH) {
                    break;
                }
                remainingWidth -= DETAILS_CELL_WIDTH;
                totalDetailsWidth += DETAILS_CELL_WIDTH;
                numFittingDetails++;
                makeDetails(i);
            }
        } else {
            linkTitleWidth = titleWidth = contentWidth;

            // No thing body width means no details will be shown.
            numFittingDetails = 0;
        }

        int leftGadgetWidth = 0;
        if (drawVotingArrows) {
            leftGadgetWidth += VotingArrows.getWidth(drawVotingArrows) + PADDING;
            if (drawScore) {
                VotingArrows.measureScoreText(scoreText, scoreBounds);
            }
        }
        if (showThumbnail) {
            leftGadgetWidth += Thumbnail.getWidth() + PADDING;
        }
        titleWidth -= leftGadgetWidth;

        int statusWidth = contentWidth - leftGadgetWidth;
        if (totalDetailsWidth > 0) {
            statusWidth -= totalDetailsWidth + PADDING;
        }

        linkTitleWidth = Math.max(0, linkTitleWidth);
        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        totalDetailsWidth = Math.max(0, totalDetailsWidth);

        int leftHeight = 0;
        if (drawVotingArrows && expanded) {
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

        minHeight = PADDING + Math.max(leftHeight, rightHeight) + PADDING;

        // Move from left to right one more time.
        int x = PADDING + (PADDING * nesting);
        if (drawVotingArrows) {
            x += VotingArrows.getWidth(drawVotingArrows);
            x += PADDING;
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

        if (titleLayout != null) {
            y += titleLayout.getHeight() + ELEMENT_PADDING;
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
                TEXT_PAINTS[linkTitlePaint], width, TruncateAt.END);
        return makeStaticLayout(linkTitlePaint, truncated, width);
    }

    private Layout createTitleLayout(int width) {
        return makeStaticLayout(titlePaint, title, width);
    }

    private Layout createBodyLayout(int width) {
        return makeStaticLayout(bodyPaint, bodyText, width);
    }

    private static Layout makeStaticLayout(int paint, CharSequence text, int width) {
        return new StaticLayout(text, TEXT_PAINTS[paint], width,
                Alignment.ALIGN_NORMAL, 1f, 0f, true);
    }

    private BoringLayout createStatusLayout(int width) {
        TextPaint paint = TEXT_PAINTS[statusPaint];
        statusMetrics = BoringLayout.isBoring(statusText, paint, statusMetrics);
        if (statusLayout != null) {
            statusLayout = statusLayout.replaceOrMake(statusText, paint, width,
                    Alignment.ALIGN_NORMAL, 1f, 0f, statusMetrics, true, TruncateAt.END, width);
        } else {
            statusLayout = BoringLayout.make(statusText, paint, width,
                    Alignment.ALIGN_NORMAL, 1f, 0f, statusMetrics, true, TruncateAt.END, width);
        }
        return statusLayout;
    }

    private void makeDetails(int index) {
        Resources r = getResources();
        switch (details[index]) {
            case DETAIL_UP_VOTES:
                makeDetailsLayout(index, getQuantityString(r, R.plurals.votes_up, ups, index));
                break;

            case DETAIL_DOWN_VOTES:
                makeDetailsLayout(index, getQuantityString(r, R.plurals.votes_down, downs, index));
                break;

            case DETAIL_DOMAIN:
                makeDetailsLayout(index, domain);
                break;

            case DETAIL_SUBREDDIT:
                makeDetailsLayout(index, subreddit);
                break;

            case DETAIL_AUTHOR:
                makeDetailsLayout(index, author);
                break;

            case DETAIL_TIMESTAMP:
                makeDetailsLayout(index, getRelativeTime(r, nowTimeMs, createdUtc, index));
                break;

            case DETAIL_DESTINATION:
                makeDetailsLayout(index, destination);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    private void makeDetailsLayout(int i, CharSequence text) {
        // Allocate internal storage for the metrics and layouts, since we now
        // know we will need them.
        if (detailMetrics == null) {
            detailMetrics = new BoringLayout.Metrics[MAX_INTERNAL_DETAILS];
        }
        if (detailLayouts == null) {
            detailLayouts = new BoringLayout[MAX_INTERNAL_DETAILS];
        }

        // Calculate the metrics and try to reuse the existing instance.
        TextPaint paint = TEXT_PAINTS[statusPaint];
        detailMetrics[i] = BoringLayout.isBoring(text, paint, detailMetrics[i]);

        // Create the layout and try to reuse the existing layout.
        if (detailLayouts[i] != null) {
            detailLayouts[i] = detailLayouts[i].replaceOrMake(text, paint,
                    DETAILS_INNER_CELL_WIDTH,
                    Alignment.ALIGN_CENTER, 1, 0, detailMetrics[i], true, TruncateAt.END,
                    DETAILS_INNER_CELL_WIDTH);
        } else {
            detailLayouts[i] = BoringLayout.make(text, paint, DETAILS_INNER_CELL_WIDTH,
                    Alignment.ALIGN_CENTER, 1, 0, detailMetrics[i], true, TruncateAt.END,
                    DETAILS_INNER_CELL_WIDTH);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        // Draw details on the right if we can fit some.
        if (numFittingDetails > 0) {
            // Translate to the first details cell from the right edge.
            int dx = c.getWidth() - PADDING - numFittingDetails * DETAILS_CELL_WIDTH;
            c.translate(dx, 0);

            // Draw a detail, move right, and repeat...
            for (int i = 0; i < numFittingDetails; i++) {
                drawDetails(c, i);
                c.translate(DETAILS_CELL_WIDTH, 0);
            }
            c.translate(-dx - DETAILS_CELL_WIDTH * numFittingDetails, 0);
        }

        c.translate(PADDING * (1 + nesting), PADDING);

        if (linkTitleLayout != null) {
            linkTitleLayout.draw(c);
            c.translate(0, linkTitleLayout.getHeight() + ELEMENT_PADDING);
        }

        if (drawVotingArrows) {
            if (expanded) {
                VotingArrows.draw(c, scoreText, scoreBounds, likes, drawScore, isVotable);
            }
            c.translate(VotingArrows.getWidth(drawVotingArrows) + PADDING, 0);
        }

        if (showThumbnail) {
            Thumbnail.draw(c, thumbShader);
            c.translate(Thumbnail.getWidth() + PADDING, 0);
        }

        c.translate(0, -PADDING + (minHeight - rightHeight) / 2);

        // Render the status at the top for comments.
        if (isTopStatus() && !TextUtils.isEmpty(statusText)) {
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
        if (!isTopStatus() && !TextUtils.isEmpty(statusText)) {
            statusLayout.draw(c);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e)
                || onThumbnailTouchEvent(e)
                || onBodyTouchEvent(e)
                || super.onTouchEvent(e);
    }

    private boolean onThumbnailTouchEvent(MotionEvent e) {
        int action = e.getAction();
        return showThumbnail
                && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                && thumbRect.contains((int) e.getX(), (int) e.getY());
    }

    private boolean onBodyTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP)
                && bodyText instanceof Spannable
                && bodyLayout != null
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

    private void drawDetails(Canvas c, int index) {
        BoringLayout layout = detailLayouts[index];
        int dx = ELEMENT_PADDING;
        int dy = (c.getHeight() - layout.getHeight()) / 2;
        c.translate(dx, dy);
        layout.draw(c);
        c.translate(-dx, -dy);
    }

    public boolean onDown(MotionEvent e) {
        return Thumbnail.onDown(e, thumbRect, showThumbnail)
                || VotingArrows.onDown(e, getTopOffset(), getLeftOffset(), drawVotingArrows,
                        drawScore, isVotable);
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return Thumbnail.onSingleTapUp(e, thumbRect, showThumbnail, listener)
                || VotingArrows.onSingleTapUp(e, getTopOffset(), getLeftOffset(), drawVotingArrows,
                        drawScore, isVotable, listener, this, likes);
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
