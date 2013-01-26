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
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.MotionEvent;
import android.view.View;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.VoteActions;

class VotingArrows {

    public static final String TAG = "VotingArrows";

    private static final int EVENT_NONE = 0;
    private static final int EVENT_UPVOTE = 1;
    private static final int EVENT_DOWNVOTE = 2;

    private static float FONT_SCALE;
    private static int PADDING;
    private static int ELEMENT_PADDING;

    private static Paint[] PAINTS;
    private static Paint[] STROKE_PAINTS;
    private static TextPaint[] TEXT_PAINTS;

    private static final int NUM_PAINTS = 3;
    private static final int NEUTRAL = 0;
    private static final int UP = 1;
    private static final int DOWN = 2;

    private static int ARROW_TOTAL_WIDTH;
    private static int ARROW_TOTAL_HEIGHT;
    private static int SCORE_TOTAL_HEIGHT;
    private static int SCORELESS_TOTAL_HEIGHT;

    private static Path PATH_UPVOTE;
    private static Path PATH_DOWNVOTE;

    static void init(Context context) {
        Resources r = context.getResources();
        float fontScale = r.getConfiguration().fontScale;
        if (FONT_SCALE != fontScale) {
            FONT_SCALE = fontScale;
            PADDING = r.getDimensionPixelSize(R.dimen.padding);
            ELEMENT_PADDING = r.getDimensionPixelSize(R.dimen.element_padding);

            Theme t = context.getTheme();
            int[] colorIds = new int[] {
                    R.color.arrow_neutral,
                    R.color.arrow_up,
                    R.color.arrow_down,
            };
            PAINTS = new Paint[NUM_PAINTS];
            for (int i = 0; i < NUM_PAINTS; i++) {
                PAINTS[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                PAINTS[i].setColor(r.getColor(colorIds[i]));
            }

            STROKE_PAINTS = new Paint[NUM_PAINTS];
            for (int i = 0; i < NUM_PAINTS; i++) {
                STROKE_PAINTS[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
                STROKE_PAINTS[i].setColor(PAINTS[i].getColor());
                STROKE_PAINTS[i].setStyle(Style.STROKE);
            }

            int[] styles = new int[] {
                    R.style.ThingNeutralScoreText,
                    R.style.ThingUpScoreText,
                    R.style.ThingDownScoreText,
            };
            int[] attrs = new int[] {
                    android.R.attr.textSize,
                    android.R.attr.textColor,
            };
            TEXT_PAINTS = new TextPaint[NUM_PAINTS];
            for (int i = 0; i < NUM_PAINTS; i++) {
                TypedArray a = t.obtainStyledAttributes(styles[i], attrs);
                TEXT_PAINTS[i] = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                TEXT_PAINTS[i].setTextSize(a.getDimensionPixelSize(0, 0) * FONT_SCALE);
                TEXT_PAINTS[i].setColor(a.getColor(1, -1));
                a.recycle();
            }

            ARROW_TOTAL_WIDTH = r.getDimensionPixelSize(R.dimen.arrow_total_width);
            ARROW_TOTAL_HEIGHT = r.getDimensionPixelSize(R.dimen.arrow_total_height);
            SCORE_TOTAL_HEIGHT = r.getDimensionPixelSize(R.dimen.score_total_height);
            SCORELESS_TOTAL_HEIGHT = r.getDimensionPixelSize(R.dimen.scoreless_total_height);

            int arrowHeadHeight = r.getDimensionPixelSize(R.dimen.arrow_head_height);
            int arrowStemHeight = ARROW_TOTAL_HEIGHT - arrowHeadHeight;
            int arrowStemWidth = r.getDimensionPixelSize(R.dimen.arrow_stem_width);
            int arrowStemIndent = (ARROW_TOTAL_WIDTH - arrowStemWidth) / 2;

            PATH_UPVOTE = new Path();
            PATH_UPVOTE.moveTo(ARROW_TOTAL_WIDTH / 2, 0);
            PATH_UPVOTE.lineTo(ARROW_TOTAL_WIDTH, arrowHeadHeight);
            PATH_UPVOTE.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, arrowHeadHeight);
            PATH_UPVOTE.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, ARROW_TOTAL_HEIGHT);
            PATH_UPVOTE.lineTo(arrowStemIndent, ARROW_TOTAL_HEIGHT);
            PATH_UPVOTE.lineTo(arrowStemIndent, arrowHeadHeight);
            PATH_UPVOTE.lineTo(0, arrowHeadHeight);
            PATH_UPVOTE.close();

            PATH_DOWNVOTE = new Path();
            PATH_DOWNVOTE.moveTo(arrowStemIndent, 0);
            PATH_DOWNVOTE.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, 0);
            PATH_DOWNVOTE.lineTo(ARROW_TOTAL_WIDTH - arrowStemIndent, arrowStemHeight);
            PATH_DOWNVOTE.lineTo(ARROW_TOTAL_WIDTH, arrowStemHeight);
            PATH_DOWNVOTE.lineTo(ARROW_TOTAL_WIDTH / 2, ARROW_TOTAL_HEIGHT);
            PATH_DOWNVOTE.lineTo(0, arrowStemHeight);
            PATH_DOWNVOTE.lineTo(arrowStemIndent, arrowStemHeight);
            PATH_DOWNVOTE.close();
        }
    }

    static void draw(Canvas c, String scoreText, Rect scoreBounds, int likes,
            boolean drawScore, boolean isVotable) {
        int upPaintIndex = NEUTRAL;
        int scorePaintIndex = NEUTRAL;
        int downPaintIndex = NEUTRAL;
        switch (likes) {
            case VoteActions.ACTION_VOTE_UP:
                upPaintIndex = scorePaintIndex = UP;
                break;

            case VoteActions.ACTION_VOTE_DOWN:
                scorePaintIndex = downPaintIndex = DOWN;
                break;
        }

        c.save();

        Paint[] arrowPaints = isVotable ? PAINTS : STROKE_PAINTS;
        c.drawPath(PATH_UPVOTE, arrowPaints[upPaintIndex]);
        c.translate(0, ARROW_TOTAL_HEIGHT);

        if (drawScore) {
            int sdx = (ARROW_TOTAL_WIDTH - scoreBounds.width()) / 2;
            int sdy = (SCORE_TOTAL_HEIGHT + scoreBounds.height()) / 2;
            c.translate(sdx, sdy);
            c.drawText(scoreText, 0, 0, TEXT_PAINTS[scorePaintIndex]);
            c.translate(-sdx, -sdy);
            c.translate(0, SCORE_TOTAL_HEIGHT);
        } else {
            c.translate(0, SCORELESS_TOTAL_HEIGHT);
        }

        c.drawPath(PATH_DOWNVOTE, arrowPaints[downPaintIndex]);
        c.restore();
    }

    static String getScoreText(int score) {
        return TextUtils.ellipsize(Integer.toString(score),
                TEXT_PAINTS[NEUTRAL],
                PADDING + ARROW_TOTAL_WIDTH + PADDING - ELEMENT_PADDING,
                TruncateAt.END).toString();
    }

    static void measureScoreText(String scoreText, Rect bounds) {
        TEXT_PAINTS[NEUTRAL].getTextBounds(scoreText, 0, scoreText.length(), bounds);
    }

    static int getWidth(boolean votable) {
        return votable ? ARROW_TOTAL_WIDTH : 0;
    }

    static int getHeight(boolean votable, boolean drawScore) {
        if (votable) {
            return ARROW_TOTAL_HEIGHT * 2
                    + (drawScore ? SCORE_TOTAL_HEIGHT : SCORELESS_TOTAL_HEIGHT);
        }
        return 0;
    }

    static boolean onDown(MotionEvent e, float top, float left, boolean drawArrows,
            boolean drawScore, boolean isVotable) {
        return getEvent(e, top, left, drawArrows, drawScore, isVotable) != EVENT_NONE;
    }

    static boolean onSingleTapUp(MotionEvent e, float top, float left, boolean drawArrows,
            boolean drawScore, boolean isVotable, OnVoteListener listener, View view, int likes) {
        if (listener != null) {
            int event = getEvent(e, top, left, drawArrows, drawScore, isVotable);
            switch (event) {
                case EVENT_UPVOTE:
                    listener.onVote(view, likes != VoteActions.ACTION_VOTE_UP ?
                            VoteActions.ACTION_VOTE_UP : VoteActions.ACTION_VOTE_NEUTRAL);
                    return true;

                case EVENT_DOWNVOTE:
                    listener.onVote(view, likes != VoteActions.ACTION_VOTE_DOWN ?
                            VoteActions.ACTION_VOTE_DOWN : VoteActions.ACTION_VOTE_NEUTRAL);
                    return true;
            }
        }
        return false;
    }

    private static int getEvent(MotionEvent e, float top, float left, boolean drawArrows,
            boolean drawScore, boolean isVotable) {
        if (isVotable) {
            float right = left + PADDING + getWidth(drawArrows) + PADDING;
            if (drawArrows && e.getX() > left && e.getX() < right) {
                float upBottom = top + PADDING + ARROW_TOTAL_HEIGHT + PADDING;
                if (e.getY() < upBottom) {
                    return EVENT_UPVOTE;
                }

                float downTop = top + PADDING + getHeight(true, drawScore)
                        - ARROW_TOTAL_HEIGHT - PADDING;
                float downBottom = top + PADDING + getHeight(true, drawScore) + PADDING * 2;
                if (e.getY() > downTop && e.getY() < downBottom) {
                    return EVENT_DOWNVOTE;
                }
            }
        }
        return EVENT_NONE;
    }
}
