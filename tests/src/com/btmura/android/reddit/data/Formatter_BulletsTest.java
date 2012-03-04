package com.btmura.android.reddit.data;


public class Formatter_BulletsTest extends AbstractFormatterTest {

    public void testFormat_bullet() {
        CharSequence cs = assertBulletFormat("* bullet1\n* bullet2", "bullet1\nbullet2");
        assertBulletSpan(cs, 0, 7);
        assertBulletSpan(cs, 8, 15);
    }

    public void testFormat_bulletBadFormat() {
        assertBulletFormat("1* bullet1\n2* bullet2", "1* bullet1\n2* bullet2");
    }
}
