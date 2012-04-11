package com.btmura.android.reddit.data;

import android.test.AndroidTestCase;

public class RelativeTimeTest extends AndroidTestCase {

    private static final int MINUTE_SECONDS = 60;
    private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    private static final int DAY_SECONDS = HOUR_SECONDS * 24;
    private static final int MONTH_SECONDS = DAY_SECONDS * 30;
    private static final int YEAR_SECONDS = MONTH_SECONDS * 12;
    private static final long NOW = YEAR_SECONDS * 3;

    public void testFormat_seconds() {
        assertEquals("0 seconds", RelativeTime.format(mContext, NOW, NOW));
        assertEquals("1 second", RelativeTime.format(mContext, NOW, NOW - 1));
        assertEquals("20 seconds", RelativeTime.format(mContext, NOW, NOW - 20));
        assertEquals("59 seconds", RelativeTime.format(mContext, NOW, NOW - 59));
    }

    public void testFormat_minutes() {
        assertEquals("1 minute", RelativeTime.format(mContext, NOW, NOW - MINUTE_SECONDS));
        assertEquals("20 minutes", RelativeTime.format(mContext, NOW, NOW - MINUTE_SECONDS * 20));
        assertEquals("59 minutes", RelativeTime.format(mContext, NOW, NOW - MINUTE_SECONDS * 59));
        assertEquals("59 minutes",
                RelativeTime.format(mContext, NOW, NOW - MINUTE_SECONDS * 59 - 30));
    }

    public void testFormat_hours() {
        assertEquals("1 hour", RelativeTime.format(mContext, NOW, NOW - HOUR_SECONDS));
        assertEquals("23 hours", RelativeTime.format(mContext, NOW, NOW - HOUR_SECONDS * 23));
    }

    public void testFormat_days() {
        assertEquals("1 day", RelativeTime.format(mContext, NOW, NOW - DAY_SECONDS));
        assertEquals("29 days", RelativeTime.format(mContext, NOW, NOW - DAY_SECONDS * 29));
    }

    public void testFormat_months() {
        assertEquals("1 month", RelativeTime.format(mContext, NOW, NOW - MONTH_SECONDS));
        assertEquals("11 months", RelativeTime.format(mContext, NOW, NOW - MONTH_SECONDS * 11));
    }

    public void testFormat_years() {
        assertEquals("1 year", RelativeTime.format(mContext, NOW, NOW - YEAR_SECONDS));
        assertEquals("2 years", RelativeTime.format(mContext, NOW, NOW - YEAR_SECONDS * 2));
    }
}
