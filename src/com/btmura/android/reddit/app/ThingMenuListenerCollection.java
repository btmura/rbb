/*
 * Copyright (C) 2012 Brian Muramatsu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmura.android.reddit.app;

import java.util.ArrayList;

import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.ThingMenuFragment.ThingMenuListener;

class ThingMenuListenerCollection implements ThingMenuListener {

    private final ArrayList<ThingMenuListener> listeners =
            new ArrayList<ThingMenuListener>(2);

    private ViewPager thingPager;
    private MenuItem linkItem;
    private MenuItem commentsItem;

    public void setThingPager(ViewPager thingPager) {
        this.thingPager = thingPager;
    }

    public void add(ThingMenuListener listener) {
        listeners.add(listener);
    }

    public void remove(ThingMenuListener listener) {
        listeners.remove(listener);
    }

    public void onCreateThingOptionsMenu(Menu menu) {
        linkItem = menu.findItem(R.id.menu_link);
        commentsItem = menu.findItem(R.id.menu_comments);

        int listenerCount = listeners.size();
        for (int i = 0; i < listenerCount; i++) {
            listeners.get(i).onCreateThingOptionsMenu(menu);
        }
    }

    public void onPrepareThingOptionsMenu(Menu menu, int pageType) {
        if (linkItem != null) {
            linkItem.setVisible(getPageCount() > 1 && isPageType(ThingPagerAdapter.TYPE_COMMENTS));
        }
        if (commentsItem != null) {
            commentsItem.setVisible(getPageCount() > 1 && isPageType(ThingPagerAdapter.TYPE_LINK));
        }
        int listenerCount = listeners.size();
        for (int i = 0; i < listenerCount; i++) {
            listeners.get(i).onPrepareThingOptionsMenu(menu, pageType);
        }
    }

    public void onThingOptionsItemSelected(MenuItem item, int pageType) {
        switch (item.getItemId()) {
            case R.id.menu_link:
                thingPager.setCurrentItem(ThingPagerAdapter.PAGE_LINK);
                break;

            case R.id.menu_comments:
                thingPager.setCurrentItem(ThingPagerAdapter.PAGE_COMMENTS);
                break;

            default:
                int listenerCount = listeners.size();
                for (int i = 0; i < listenerCount; i++) {
                    listeners.get(i).onThingOptionsItemSelected(item, pageType);
                }
                break;
        }
    }

    private int getPageCount() {
        if (thingPager != null) {
            ThingPagerAdapter adapter = getThingPagerAdapter();
            if (adapter != null) {
                return adapter.getCount();
            }
        }
        return -1;
    }

    private boolean isPageType(int pageType) {
        return pageType == getCurrentPageType();
    }

    private int getCurrentPageType() {
        if (thingPager != null) {
            ThingPagerAdapter adapter = getThingPagerAdapter();
            if (adapter != null) {
                return adapter.getPageType(thingPager.getCurrentItem());
            }
        }
        return -1;
    }

    private ThingPagerAdapter getThingPagerAdapter() {
        return thingPager != null ? (ThingPagerAdapter) thingPager.getAdapter() : null;
    }

}
