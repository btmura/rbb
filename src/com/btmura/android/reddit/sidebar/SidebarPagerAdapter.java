package com.btmura.android.reddit.sidebar;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class SidebarPagerAdapter extends FragmentPagerAdapter {
    
    private final String[] subreddits;
    
    public SidebarPagerAdapter(FragmentManager fm, String[] subreddits) {
        super(fm);
        this.subreddits = subreddits;
    }
    
    @Override
    public int getCount() {
        return subreddits.length;
    }
    
    @Override
    public Fragment getItem(int position) {
        return SidebarFragment.newInstance(subreddits[position], -1);
    }
}
