package com.btmura.android.reddit.data;


public class Formatter_NamedLinksTest extends AbstractFormatterTest {

    public void testFormat() {
        CharSequence cs = assertNamedLinksFormat("[foo](abc)", "foo");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 3, "http://abc");

        cs = assertNamedLinksFormat("[foo] (abc)", "foo");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 3, "http://abc");

        cs = assertNamedLinksFormat("[foo] (abc desc)", "foo");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 3, "http://abc");
    }

    public void testFormat_badFormat() {
        assertNamedLinksFormat("[foo] bar (abc)", "[foo] bar (abc)");
    }

    public void testFormat_nestedParens() {
        CharSequence cs = assertNamedLinksFormat("Here is a [link](/abc (123) (456)).",
                "Here is a link.");
        AbstractFormatterTest.assertUrlSpan(cs, 10, 14, "http://www.reddit.com/abc");
    }

    public void testFormat_multiple() {
        CharSequence cs = assertNamedLinksFormat("[Link 1](/a (123)) and [Link 2](/b (456))",
                "Link 1 and Link 2");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 6, "http://www.reddit.com/a");
        AbstractFormatterTest.assertUrlSpan(cs, 11, 17, "http://www.reddit.com/b");
    }

    public void testFormat_nestedParensMultipleLines() {
        CharSequence cs = assertNamedLinksFormat("[Link 1](/a (123))\n[Link 2](/b (456))",
                "Link 1\nLink 2");
        AbstractFormatterTest.assertUrlSpan(cs, 0, 6, "http://www.reddit.com/a");
        AbstractFormatterTest.assertUrlSpan(cs, 7, 13, "http://www.reddit.com/b");
    }
}
