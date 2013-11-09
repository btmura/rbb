/*
 * Copyright (C) 2013 Brian Muramatsu
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

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.btmura.android.reddit.widget.ThingView.OnThingViewClickListener;

/** Logic related to the status line that is shown in a {@link ThingView}. */
final class StatusLine {

    /** Returns whether or not there was a down event within the status line. */
    static boolean onDown(
            boolean isStatusClickable,
            OnThingViewClickListener listener,
            RectF statusBounds,
            MotionEvent e) {

        return shouldReportClick(isStatusClickable, listener, statusBounds, e);
    }

    /** Possibly notifies the listener that a single tap up has occurred within the status line. */
    static boolean onSingleTapUp(
            boolean isStatusClickable,
            OnThingViewClickListener listener,
            RectF statusBounds,
            MotionEvent e,
            View view) {

        if (shouldReportClick(isStatusClickable, listener, statusBounds, e)) {
            view.playSoundEffect(SoundEffectConstants.CLICK);
            listener.onStatusClick(view);
            return true;
        }
        return false;
    }

    private static boolean shouldReportClick(
            boolean isStatusClickable,
            OnThingViewClickListener listener,
            RectF statusBounds,
            MotionEvent e) {

        return isStatusClickable && listener != null && statusBounds.contains(e.getX(), e.getY());
    }
}
