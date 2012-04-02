package com.btmura.android.reddit.browser;

import android.content.Context;
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

public class ThingView extends View {

    private static final int MAX_THUMB_WIDTH_DP = 70;
    private static final int PADDING_DP = 10;

    private static final int PAINT_SMALL = 0;
    private static final int PAINT_MEDIUM = 1;
    private static TextPaint[] PAINTS;

    private float density;
    private int padding;
    private int thumbWidth;

    private CharSequence title = "";
    private CharSequence status = "";
    private Drawable drawable;
    private Layout titleLayout;
    private Layout statusLayout;

    public ThingView(Context context) {
        this(context, null);
    }

    public ThingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ThingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
        density = context.getResources().getDisplayMetrics().density;
        padding = (int) (PADDING_DP * density);
        thumbWidth = (int) (MAX_THUMB_WIDTH_DP * density);
    }

    private void init(Context context) {
        if (PAINTS == null) {
            Theme t = context.getTheme();
            int[] styles = new int[] {
                    android.R.style.TextAppearance_Holo_Small,
                    android.R.style.TextAppearance_Holo_Medium,
                    android.R.style.TextAppearance_Holo_Large,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize, android.R.attr.textColor,
            };

            PAINTS = new TextPaint[2];
            for (int i = 0; i < 2; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                PAINTS[i] = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0));
                PAINTS[i].setColor(a.getColor(1, -1));
                a.recycle();
            }
        }
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        requestLayout();
    }

    public void setStatus(CharSequence status) {
        this.status = status;
        requestLayout();
    }

    public void setThumbnailBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            drawable = new BitmapDrawable(bitmap);
            drawable.setBounds(0, 0, (int) (bitmap.getWidth() * density),
                    (int) (bitmap.getHeight() * density));
        } else {
            drawable = getResources().getDrawable(R.drawable.thumbnail);
            drawable.setBounds(0, 0, thumbWidth, thumbWidth);
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

        int textWidth = measuredWidth - padding * 2;
        if (drawable != null) {
            textWidth -= thumbWidth + padding;
        }
        textWidth = Math.max(0, textWidth);
        titleLayout = makeTitleLayout(textWidth);
        statusLayout = makeStatusLayout(textWidth);

        int thumbHeight = thumbWidth;
        int textHeight = titleLayout.getHeight() + padding + statusLayout.getHeight();
        int minHeight = padding + Math.max(thumbHeight, textHeight) + padding;

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
        return new StaticLayout(title, PAINTS[PAINT_MEDIUM], width, Alignment.ALIGN_NORMAL, 1f, 0f,
                true);
    }

    private Layout makeStatusLayout(int width) {
        BoringLayout.Metrics m = BoringLayout.isBoring(status, PAINTS[PAINT_SMALL]);
        return BoringLayout.make(status, PAINTS[PAINT_SMALL], width, Alignment.ALIGN_NORMAL, 1f,
                0f, m, true, TruncateAt.END, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        canvas.translate(padding, padding);
        if (drawable != null) {
            drawable.draw(canvas);
            canvas.translate(thumbWidth + padding, 0);
        }
        titleLayout.draw(canvas);

        canvas.translate(0, canvas.getHeight() - padding * 2 - statusLayout.getHeight());
        statusLayout.draw(canvas);

        canvas.restore();
    }
}
