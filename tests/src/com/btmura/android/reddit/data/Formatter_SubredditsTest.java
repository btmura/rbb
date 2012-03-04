package com.btmura.android.reddit.data;


public class Formatter_SubredditsTest extends AbstractFormatterTest {
    
    public void testFormat() {
        CharSequence s = assertSubredditFormat("/r/food", "/r/food");
        assertSubredditSpan(s, 0, 7, "food");
        
        s = assertSubredditFormat("/r/food/", "/r/food/");
        assertSubredditSpan(s, 0, 8, "food");
        
        s = assertSubredditFormat("/r/under_score/", "/r/under_score/");
        assertSubredditSpan(s, 0, 15, "under_score");
    }
}
