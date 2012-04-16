package com.btmura.android.reddit.sidebar;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.btmura.android.reddit.R;

public class SidebarActivity extends Activity {

    public static final String EXTRA_SUBREDDIT = "s";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sidebar);

        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        String subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
        setTitle(subreddit);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        String[] subreddits = subreddit.split("\\+");
        pager.setAdapter(new SidebarPagerAdapter(getFragmentManager(), subreddits));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
