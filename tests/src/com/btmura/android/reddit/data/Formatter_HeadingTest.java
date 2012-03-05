package com.btmura.android.reddit.data;

public class Formatter_HeadingTest extends AbstractFormatterTest {

    public void testFormat() {
        assertHeadingFormat("#Hello", "Hello");
        assertHeadingFormat("##Hello", "Hello");
        assertHeadingFormat("#Hello#", "Hello");
        assertHeadingFormat("##Hello##", "Hello");
    }
}
