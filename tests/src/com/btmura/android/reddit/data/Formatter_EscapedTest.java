package com.btmura.android.reddit.data;


public class Formatter_EscapedTest extends AbstractFormatterTest {

    public void testFormat_noEscapes() {
        assertEscapedFormat("title", "title");
    }
    
    public void testFormat_gt() {
        assertEscapedFormat("gt &gt;", "gt >");
    }
    
    public void testFormat_lt() {
        assertEscapedFormat("lt &lt;", "lt <");
    }
    
    public void testFormat_amp() {
        assertEscapedFormat("amp &amp;", "amp &");
    }
    
    public void testFormat_quot() {
        assertEscapedFormat("quot &quot;", "quot \"");
    }
    
    public void testFormat_apos() {
        assertEscapedFormat("apos &apos;", "apos '");
    }
    
    public void testFormat_nbsp() {
        assertEscapedFormat("nbsp &nbsp;", "nbsp  ");
    }
    
    public void testFormat_multipleEscapes() {
        assertEscapedFormat("gt &gt; lt &lt;", "gt > lt <");
        assertEscapedFormat("&lt;3 &apos;Quote&apos;", "<3 'Quote'");
    } 
}
