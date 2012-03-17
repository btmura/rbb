package com.btmura.android.reddit.browser;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;

public class ThingPagerAdapter extends FragmentStatePagerAdapter {

    public static final int TYPE_LINK = 0;
    public static final int TYPE_COMMENTS = 1;

    private final Thing thing;

    public ThingPagerAdapter(FragmentManager fm, Thing thing) {
        super(fm);
        this.thing = thing;
    }

    @Override
    public int getCount() {
        return thing.isSelf ? 1 : 2;
    }

    @Override
    public Fragment getItem(int position) {
        switch (getType(thing, position)) {
            case TYPE_LINK:
                return LinkFragment.newInstance(thing.url);

            case TYPE_COMMENTS:
                return CommentListFragment.newInstance(thing.getId());

            default:
                throw new IllegalStateException();
        }
    }

    public static int getType(Thing t, int position) {
        switch (position) {
            case 0:
                return t.isSelf ? TYPE_COMMENTS : TYPE_LINK;

            case 1:
                return TYPE_COMMENTS;

            default:
                throw new IllegalStateException();
        }
    }
}
