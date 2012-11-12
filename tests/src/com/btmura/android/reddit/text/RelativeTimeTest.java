package com.btmura.android.reddit.text;

import android.test.AndroidTestCase;

public class RelativeTimeTest extends AndroidTestCase {

    private static final int MINUTE_SECONDS = 60;
    private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    private static final int DAY_SECONDS = HOUR_SECONDS * 24;
    private static final int MONTH_SECONDS = DAY_SECONDS * 30;
    private static final int YEAR_SECONDS = MONTH_SECONDS * 12;
    private static final long NOW = YEAR_SECONDS * 3;
    private static final long NOW_MS = NOW * 1000;

    public void testFormat_seconds() {
        assertEquals("0 seconds ago",
                RelativeTime.format(mContext, NOW_MS, NOW));
        assertEquals("1 second ago",
                RelativeTime.format(mContext, NOW_MS, NOW - 1));
        assertEquals("20 seconds ago",
                RelativeTime.format(mContext, NOW_MS, NOW - 20));
        assertEquals("59 seconds ago",
                RelativeTime.format(mContext, NOW_MS, NOW - 59));
    }

    public void testFormat_minutes() {
        assertEquals("1 minute ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MINUTE_SECONDS));
        assertEquals("20 minutes ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MINUTE_SECONDS * 20));
        assertEquals("59 minutes ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MINUTE_SECONDS * 59));
        assertEquals("59 minutes ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MINUTE_SECONDS * 59 - 30));
    }

    public void testFormat_hours() {
        assertEquals("1 hour ago",
                RelativeTime.format(mContext, NOW_MS, NOW - HOUR_SECONDS));
        assertEquals("23 hours ago",
                RelativeTime.format(mContext, NOW_MS, NOW - HOUR_SECONDS * 23));
    }

    public void testFormat_days() {
        assertEquals("1 day ago",
                RelativeTime.format(mContext, NOW_MS, NOW - DAY_SECONDS));
        assertEquals("29 days ago",
                RelativeTime.format(mContext, NOW_MS, NOW - DAY_SECONDS * 29));
    }

    public void testFormat_months() {
        assertEquals("1 month ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MONTH_SECONDS));
        assertEquals("11 months ago",
                RelativeTime.format(mContext, NOW_MS, NOW - MONTH_SECONDS * 11));
    }

    public void testFormat_years() {
        assertEquals("1 year ago",
                RelativeTime.format(mContext, NOW_MS, NOW - YEAR_SECONDS));
        assertEquals("2 years ago",
                RelativeTime.format(mContext, NOW_MS, NOW - YEAR_SECONDS * 2));
    }
}
