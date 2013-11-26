package com.btmura.android.reddit.content;

import android.test.AndroidTestCase;

public class AccountPrefsTest extends AndroidTestCase {

    // TODO(btmura): add tests for other trivial methods

    public void testSetLastCommentFilter() {
        AccountPrefs.setLastCommentFilter(mContext, 0);
        assertEquals(0, AccountPrefs.getLastCommentFilter(mContext, 1337));

        AccountPrefs.setLastCommentFilter(mContext, 1);
        assertEquals(1, AccountPrefs.getLastCommentFilter(mContext, 1337));
    }
}
