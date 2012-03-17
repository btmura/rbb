package com.btmura.android.reddit.data;

import android.content.Context;

import com.btmura.android.reddit.R;

public class RelativeTime {

    private static final int MINUTE_SECONDS = 60;
    private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    private static final int DAY_SECONDS = HOUR_SECONDS * 24;
    private static final int MONTH_SECONDS = DAY_SECONDS * 30;
    private static final int YEAR_SECONDS = MONTH_SECONDS * 12;

    public static String format(Context context, long diff) {
        int resId;
        int divisor;
        if (diff > YEAR_SECONDS * 2) {
            resId = R.string.x_years_ago;
            divisor = YEAR_SECONDS;
        } else if (diff > MONTH_SECONDS * 2) {
            resId = R.string.x_months_ago;
            divisor = MONTH_SECONDS;
        } else if (diff > DAY_SECONDS * 2) {
            resId = R.string.x_days_ago;
            divisor = DAY_SECONDS;
        } else if (diff > HOUR_SECONDS * 2) {
            resId = R.string.x_hours_ago;
            divisor = HOUR_SECONDS;
        } else if (diff > MINUTE_SECONDS * 2) {
            resId = R.string.x_minutes_ago;
            divisor = MINUTE_SECONDS;
        } else {
            resId = R.string.x_seconds_ago;
            divisor = 1;
        }
        
        long value = Math.round(Math.floor((double) diff / divisor));
        return context.getString(resId, value);
    }
}
