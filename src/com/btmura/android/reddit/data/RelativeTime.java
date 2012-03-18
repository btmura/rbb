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

package com.btmura.android.reddit.data;

import android.content.Context;

import com.btmura.android.reddit.R;

public class RelativeTime {

    private static final int MINUTE_SECONDS = 60;
    private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    private static final int DAY_SECONDS = HOUR_SECONDS * 24;
    private static final int MONTH_SECONDS = DAY_SECONDS * 30;
    private static final int YEAR_SECONDS = MONTH_SECONDS * 12;

    public static String format(Context context, long now, long time) {
        long diff = now - time;
        int resId;
        int divisor;
        if (diff > YEAR_SECONDS * 2) {
            resId = R.string.x_time_years;
            divisor = YEAR_SECONDS;
        } else if (diff > MONTH_SECONDS * 2) {
            resId = R.string.x_time_months;
            divisor = MONTH_SECONDS;
        } else if (diff > DAY_SECONDS * 2) {
            resId = R.string.x_time_days;
            divisor = DAY_SECONDS;
        } else if (diff > HOUR_SECONDS * 2) {
            resId = R.string.x_time_hours;
            divisor = HOUR_SECONDS;
        } else if (diff > MINUTE_SECONDS * 2) {
            resId = R.string.x_time_minutes;
            divisor = MINUTE_SECONDS;
        } else {
            resId = R.string.x_time_seconds;
            divisor = 1;
        }

        long value = Math.round(Math.floor((double) diff / divisor));
        return context.getString(resId, value);
    }
}
