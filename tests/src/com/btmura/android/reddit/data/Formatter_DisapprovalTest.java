package com.btmura.android.reddit.data;

import com.btmura.android.reddit.data.Formatter.Disapproval;

public class Formatter_DisapprovalTest extends AbstractFormatterTest {

    public void testFormat_unicode() {
        CharSequence cs = Disapproval.format(mContext, "ಠ_ಠ");
        assertImageSpan(cs, 0, 3);
    }
}
