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

    static boolean onDown(
            MotionEvent e,
            RectF rect,
            OnThingViewClickListener listener) {

        return isReportableClick(e, rect, listener);
    }

    static boolean onSingleTapUp(
            MotionEvent e,
            RectF rect,
            OnThingViewClickListener listener,
            View view) {

        if (isReportableClick(e, rect, listener)) {
            view.playSoundEffect(SoundEffectConstants.CLICK);
            listener.onStatusClick(view);
            return true;
        }
        return false;
    }

    private static boolean isReportableClick(
            MotionEvent e,
            RectF rect,
            OnThingViewClickListener listener) {

        return listener != null && rect.contains(e.getX(), e.getY());
    }
}
