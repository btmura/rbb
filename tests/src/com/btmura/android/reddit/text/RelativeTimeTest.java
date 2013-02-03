package com.btmura.android.reddit.text;

import java.util.Formatter;

import android.content.res.Resources;
import android.test.AndroidTestCase;

public class RelativeTimeTest extends AndroidTestCase {

    private static final int MINUTE_SECONDS = 60;
    private static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    private static final int DAY_SECONDS = HOUR_SECONDS * 24;
    private static final int MONTH_SECONDS = DAY_SECONDS * 30;
    private static final int YEAR_SECONDS = MONTH_SECONDS * 12;
    private static final long NOW = YEAR_SECONDS * 3;
    private static final long NOW_MS = NOW * 1000;

    private Resources resources;
    private StringBuilder formatterData;
    private Formatter formatter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resources = mContext.getResources();
        formatterData = new StringBuilder();
        formatter = new Formatter(formatterData);
    }

    public void testFormat_seconds() {
        assertFormat("0 seconds ago", NOW_MS, NOW);
        assertFormat("1 second ago", NOW_MS, NOW - 1);
        assertFormat("20 seconds ago", NOW_MS, NOW - 20);
        assertFormat("59 seconds ago", NOW_MS, NOW - 59);
    }

    public void testFormat_minutes() {
        assertFormat("1 minute ago", NOW_MS, NOW - MINUTE_SECONDS);
        assertFormat("20 minutes ago", NOW_MS, NOW - MINUTE_SECONDS * 20);
        assertFormat("59 minutes ago", NOW_MS, NOW - MINUTE_SECONDS * 59);
        assertFormat("59 minutes ago", NOW_MS, NOW - MINUTE_SECONDS * 59 - 30);
    }

    public void testFormat_hours() {
        assertFormat("1 hour ago", NOW_MS, NOW - HOUR_SECONDS);
        assertFormat("23 hours ago", NOW_MS, NOW - HOUR_SECONDS * 23);
    }

    public void testFormat_days() {
        assertFormat("1 day ago", NOW_MS, NOW - DAY_SECONDS);
        assertFormat("29 days ago", NOW_MS, NOW - DAY_SECONDS * 29);
    }

    public void testFormat_months() {
        assertFormat("1 month ago", NOW_MS, NOW - MONTH_SECONDS);
        assertFormat("11 months ago", NOW_MS, NOW - MONTH_SECONDS * 11);
    }

    public void testFormat_years() {
        assertFormat("1 year ago", NOW_MS, NOW - YEAR_SECONDS);
        assertFormat("2 years ago", NOW_MS, NOW - YEAR_SECONDS * 2);
    }

    private void assertFormat(String expected, long nowMs, long timeSec) {
        formatterData.delete(0, formatterData.length());
        assertEquals(expected, RelativeTime.format(resources, formatter, nowMs, timeSec)
                .toString());
    }
}
