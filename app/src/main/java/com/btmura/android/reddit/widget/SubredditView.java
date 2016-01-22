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
import android.graphics.Color;
import android.text.BoringLayout;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.view.SwipeDismissTouchListener;

/**
 * {@link CustomView} for displaying subreddit names. It can show subscriber
 * count if the information is available.
 */
public class SubredditView extends CustomView {

  private String title;
  private BoringLayout.Metrics titleMetrics;
  private BoringLayout titleLayout;

  private SpannableStringBuilder statusText;
  private BoringLayout.Metrics statusMetrics;
  private BoringLayout statusLayout;

  public SubredditView(Context ctx) {
    this(ctx, null);
  }

  public SubredditView(Context ctx, AttributeSet attrs) {
    this(ctx, attrs, 0);
  }

  public SubredditView(Context ctx, AttributeSet attrs, int defStyle) {
    super(ctx, attrs, defStyle);
  }

  /**
   * @param name        of the subreddit or empty string for front page
   * @param over18      of the subreddit's content
   * @param subscribers or -1 if no subscriber info available
   */
  public void setData(String name, boolean over18, int subscribers) {
    title = Subreddits.getTitle(getContext(), name);
    setStatusText(over18, subscribers);
    SwipeDismissTouchListener.resetAnimation(this);
    requestLayout();
  }

  private void setStatusText(boolean over18, int subscribers) {
    // If negative subscribers, then it's just a list without status.
    if (subscribers != -1) {
      Resources r = getResources();

      if (statusText == null) {
        statusText = new SpannableStringBuilder();
      } else {
        statusText.clear();
        statusText.clearSpans();
      }

      if (over18) {
        String nsfw = r.getString(R.string.nsfw);
        statusText.append(nsfw).append("  ");
        statusText.setSpan(new ForegroundColorSpan(Color.RED), 0, nsfw.length(),
            0);
      }

      statusText.append(
          r.getQuantityString(R.plurals.subscribers, subscribers, subscribers));
    } else {
      if (statusText != null) {
        statusText.clear();
        statusText.clearSpans();
      }
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

    int contentWidth = Math.max(measuredWidth - PADDING * 2, 0);
    setTitleLayout(contentWidth);
    if (statusText != null) {
      setStatusLayout(contentWidth);
    }

    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    switch (heightMode) {
      case MeasureSpec.AT_MOST:
      case MeasureSpec.EXACTLY:
        measuredHeight = heightSize;
        break;

      case MeasureSpec.UNSPECIFIED:
        measuredHeight = getMinimumViewHeight();
        break;
    }

    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  @Override
  protected void onDraw(Canvas c) {
    c.translate(PADDING, PADDING);
    titleLayout.draw(c);
    if (!TextUtils.isEmpty(statusText)) {
      c.translate(0, titleLayout.getHeight() + ELEMENT_PADDING);
      statusLayout.draw(c);
    }
  }

  private void setTitleLayout(int width) {
    TextPaint titlePaint = TEXT_PAINTS[SUBREDDIT_TITLE];
    titleMetrics = BoringLayout.isBoring(title, titlePaint, titleMetrics);
    if (titleLayout == null) {
      titleLayout = BoringLayout.make(title, titlePaint, width,
          Alignment.ALIGN_NORMAL, 1f, 0f, titleMetrics, false, TruncateAt.END,
          width);
    } else {
      titleLayout.replaceOrMake(title, titlePaint, width,
          Alignment.ALIGN_NORMAL, 1f, 0f, titleMetrics, false, TruncateAt.END,
          width);
    }
  }

  private void setStatusLayout(int width) {
    TextPaint statusPaint = TEXT_PAINTS[SUBREDDIT_STATUS];
    statusMetrics = BoringLayout.isBoring(statusText, statusPaint,
        statusMetrics);
    if (statusLayout == null) {
      statusLayout = BoringLayout.make(statusText, statusPaint, width,
          Alignment.ALIGN_NORMAL, 1f, 0f, statusMetrics, false, TruncateAt.END,
          width);
    } else {
      statusLayout.replaceOrMake(statusText, statusPaint, width,
          Alignment.ALIGN_NORMAL, 1f, 0f, statusMetrics, false, TruncateAt.END,
          width);
    }
  }

  private int getMinimumViewHeight() {
    int height = PADDING + titleLayout.getHeight();
    if (statusText != null) {
      height += ELEMENT_PADDING + statusLayout.getHeight();
    }
    return height + PADDING;
  }
}
