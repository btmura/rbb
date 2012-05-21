package com.btmura.android.reddit.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Thing;

public class ThingView extends View {

    public static final String TAG = "ThingView";

    private static float FONT_SCALE;
    private static float DENSITY;

    private static int THUMB_WIDTH;
    private static int PADDING;
    private static int ELEMENT_PADDING;
    private static int MIN_DETAILS_WIDTH;
    private static int MAX_DETAILS_WIDTH;

    private static TextPaint[] PAINTS;
    private static final int TITLE = 0;
    private static final int STATUS = 1;

    private int bodyWidth;

    private Thing thing;
    private Drawable drawable;

    private Layout titleLayout;
    private Layout statusLayout;
    private Layout detailsLayout;

    public ThingView(Context context) {
        this(context, null);
    }

    public ThingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
            THUMB_WIDTH = r.getDimensionPixelSize(R.dimen.max_thumb_width);
            MIN_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.min_details_width);
            MAX_DETAILS_WIDTH = r.getDimensionPixelSize(R.dimen.max_details_width);

            Theme t = context.getTheme();
            int[] styles = new int[] {
                    R.style.ThingTitleText, R.style.ThingStatusText,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize, android.R.attr.textColor,
            };

            PAINTS = new TextPaint[2];
            for (int i = 0; i < 2; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                PAINTS[i] = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * FONT_SCALE);
                PAINTS[i].setColor(a.getColor(1, -1));
                a.recycle();
            }
        }
    }

    public void setBodyWidth(int bodyWidth) {
        this.bodyWidth = bodyWidth;
    }

    public void setThing(Thing thing) {
        this.thing = thing;
        requestLayout();
    }

    public void setThumbnail(Bitmap thumb) {
        if (thumb != null) {
            drawable = new BitmapDrawable(getResources(), thumb);
            drawable.setBounds(0, 0, (int) (thumb.getWidth() * DENSITY),
                    (int) (thumb.getHeight() * DENSITY));
        } else {
            drawable = getResources().getDrawable(R.drawable.thumbnail);
            drawable.setBounds(0, 0, THUMB_WIDTH, THUMB_WIDTH);
        }
        invalidate();
    }

    public void removeThumbnail() {
        drawable = null;
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
        if (drawable != null) {
            titleWidth -= THUMB_WIDTH + PADDING;
            statusWidth -= THUMB_WIDTH + PADDING;
        }
        if (detailsWidth > 0) {
            statusWidth -= detailsWidth + PADDING;
        }

        titleWidth = Math.max(0, titleWidth);
        statusWidth = Math.max(0, statusWidth);
        detailsWidth = Math.max(0, detailsWidth);

        titleLayout = makeTitleLayout(titleWidth);
        statusLayout = makeLayout(thing.status, statusWidth, Alignment.ALIGN_NORMAL);
        if (detailsWidth > 0) {
            detailsLayout = makeLayout(detailsText, detailsWidth, Alignment.ALIGN_OPPOSITE);
        } else {
            detailsLayout = null;
        }

        int thumbHeight = THUMB_WIDTH;
        int textHeight = titleLayout.getHeight() + ELEMENT_PADDING + statusLayout.getHeight();
        int minHeight = PADDING + Math.max(thumbHeight, textHeight) + PADDING;

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
        return new StaticLayout(thing.title, PAINTS[TITLE], width, Alignment.ALIGN_NORMAL, 1f, 0f,
                true);
    }

    private static Layout makeLayout(CharSequence text, int width, Alignment alignment) {
        BoringLayout.Metrics m = BoringLayout.isBoring(text, PAINTS[STATUS]);
        return BoringLayout.make(text, PAINTS[STATUS], width, alignment, 1f, 0f, m, true,
                TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        if (detailsLayout != null) {
            int x = canvas.getWidth() - PADDING - detailsLayout.getWidth();
            int y = (canvas.getHeight() - detailsLayout.getHeight()) / 2;
            canvas.translate(x, y);
            detailsLayout.draw(canvas);
            canvas.translate(-x, -y);
        }

        canvas.translate(PADDING, PADDING);
        if (drawable != null) {
            drawable.draw(canvas);
            canvas.translate(THUMB_WIDTH + PADDING, 0);
        }
        titleLayout.draw(canvas);

        canvas.translate(0, canvas.getHeight() - PADDING * 2 - statusLayout.getHeight());
        statusLayout.draw(canvas);

        canvas.restore();
    }
}
