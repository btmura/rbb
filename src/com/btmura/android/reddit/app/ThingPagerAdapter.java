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

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.widget.ThingBundle;

/**
 * {@link FragmentStateItemPagerAdapter} for controlling what is shown in the
 * ViewPager for looking at a thing. In multipane layouts, it is responsible for
 * handling the right side of the screen.
 */
public class ThingPagerAdapter extends FragmentStateItemPagerAdapter {

    public static final String TAG = "ThingPagerAdapter";

    public static final int TYPE_LINK = 0;
    public static final int TYPE_COMMENTS = 1;
    public static final int TYPE_MESSAGES = 2;

    public static final int PAGE_LINK = TYPE_LINK;
    public static final int PAGE_COMMENTS = TYPE_COMMENTS;
    public static final int PAGE_MESSAGES = 0;

    private final ArrayList<Integer> pageTypes = new ArrayList<Integer>(2);
    private final ArrayList<Integer> oldPageTypes = new ArrayList<Integer>(2);

    private final String accountName;
    private final Bundle thingBundle;
    private final int clfFlags;

    public ThingPagerAdapter(FragmentManager fm, String accountName, Bundle thingBundle) {
        super(fm);
        this.accountName = accountName;
        this.thingBundle = thingBundle;
        this.clfFlags = getFlags(thingBundle);
        setupPages(thingBundle);
    }

    private static int getFlags(Bundle thingBundle) {
        int flags = 0;
        if (ThingBundle.hasLinkUrl(thingBundle)) {
            flags |= CommentListFragment.FLAG_SHOW_LINK_MENU_ITEM;
        }
        return flags;
    }

    private void setupPages(Bundle thingBundle) {
        if (ThingBundle.hasNoComments(thingBundle)) {
            addPage(TYPE_MESSAGES);
        } else {
            if (ThingBundle.hasLinkUrl(thingBundle)) {
                addPage(TYPE_LINK);
            }
            addPage(TYPE_COMMENTS);
        }
    }

    public void addPage(int type) {
        addPage(pageTypes.size(), type);
    }

    public void addPage(int index, int type) {
        oldPageTypes.clear();
        oldPageTypes.addAll(pageTypes);
        pageTypes.add(index, type);
        notifyDataSetChanged();
    }

    public int getPageType(int position) {
        return pageTypes.get(position);
    }

    public int findPageType(int type) {
        int count = pageTypes.size();
        for (int i = 0; i < count; i++) {
            if (pageTypes.get(i) == type) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public int getItemPosition(Object object) {
        int type = object instanceof LinkFragment ? TYPE_LINK : TYPE_COMMENTS;

        int oldPosition = -1;
        for (int i = 0; i < oldPageTypes.size(); i++) {
            if (type == oldPageTypes.get(i)) {
                oldPosition = i;
                break;
            }
        }

        int newPosition = -1;
        for (int i = 0; i < pageTypes.size(); i++) {
            if (type == pageTypes.get(i)) {
                newPosition = i;
                break;
            }
        }

        if (newPosition == -1) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getItemPosition: " + oldPageTypes
                        + " -> " + pageTypes + " = NONE");
            }
            return POSITION_NONE;
        }

        if (oldPosition == newPosition) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getItemPosition: " + oldPageTypes
                        + " -> " + pageTypes + " = UNCHANGED");
            }
            return POSITION_UNCHANGED;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getItemPosition: " + oldPageTypes
                    + " -> " + pageTypes + " = " + newPosition);
        }
        return newPosition;
    }

    @Override
    public long getItemId(int position) {
        return pageTypes.get(position);
    }

    @Override
    public int getCount() {
        return pageTypes.size();
    }

    @Override
    public Fragment getItem(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getItem: " + position + " -> " + pageTypes.get(position));
        }
        switch (pageTypes.get(position)) {
            case TYPE_LINK:
                return LinkFragment.newInstance(ThingBundle.getTitle(thingBundle),
                        ThingBundle.getLinkUrl(thingBundle));

            case TYPE_COMMENTS:
                return CommentListFragment.newInstance(accountName,
                        ThingBundle.getThingId(thingBundle),
                        ThingBundle.getLinkId(thingBundle),
                        ThingBundle.getTitle(thingBundle),
                        ThingBundle.getCommentUrl(thingBundle),
                        clfFlags);

            case TYPE_MESSAGES:
                return MessageThreadListFragment.newInstance(accountName,
                        ThingBundle.getThingId(thingBundle));

            default:
                throw new IllegalStateException();
        }
    }
}
