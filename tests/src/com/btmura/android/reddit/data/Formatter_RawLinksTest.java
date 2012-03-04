package com.btmura.android.reddit.data;


public class Formatter_RawLinksTest extends AbstractFormatterTest {

    public void testFormat() {
        CharSequence cs = assertRawLinksFormat("http://abcd", "http://abcd");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 11, "http://abcd");

        cs = assertRawLinksFormat("https://abcd", "https://abcd");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 12, "https://abcd");
    }
}
