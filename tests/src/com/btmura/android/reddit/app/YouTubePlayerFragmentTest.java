package com.btmura.android.reddit.app;

import junit.framework.TestCase;

public class YouTubePlayerFragmentTest extends TestCase {

    public void testIsYouTubeVideoUrl() throws Exception {
        assertUrl("http://www.youtube.com", false);
        assertUrl("http://youtu.be", false);
        assertUrl("http://www.youtube.com/watch?v=foobar", true);
        assertUrl("http://youtube.com/watch?v=foobar", true);
        assertUrl("http://youtu.be/foobar", true);
        assertUrl("http://www.youtu.be/foobar", true);
    }

    private void assertUrl(String url, boolean matches) {
        assertEquals(matches, YouTubePlayerFragment.isYouTubeVideoUrl(url));
    }

    public void testGetVideoId() throws Exception {
        assertVideoId("http://youtube.com", null);
        assertVideoId("http://youtube.com/watch?v=fOObar", "fOObar");
        assertVideoId("http://www.youtube.com/watch?v=foobar", "foobar");
        assertVideoId("http://youtu.be/fOObar", "fOObar");
        assertVideoId("http://www.youtu.be/foobar", "foobar");
    }

    private void assertVideoId(String url, String expectedVideoId) {
        assertEquals(expectedVideoId, YouTubePlayerFragment.getVideoId(url));
    }
}
