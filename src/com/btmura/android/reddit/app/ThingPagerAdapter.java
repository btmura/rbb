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
import com.btmura.android.reddit.content.ThingBundle;

public class ThingPagerAdapter extends FragmentStateItemPagerAdapter {

    public static final String TAG = "ThingPagerAdapter";

    public static final int TYPE_LINK = 0;
    public static final int TYPE_COMMENTS = 1;

    private final ArrayList<Integer> pages = new ArrayList<Integer>(2);
    private final ArrayList<Integer> oldPages = new ArrayList<Integer>(2);

    private final String accountName;
    private final Bundle thingBundle;

    public ThingPagerAdapter(FragmentManager fm, String accountName, Bundle thingBundle) {
        super(fm);
        this.accountName = accountName;
        this.thingBundle = thingBundle;
        if (ThingBundle.hasLinkUrl(thingBundle)) {
            addPage(TYPE_LINK);
        }
        addPage(TYPE_COMMENTS);
    }

    public void addPage(int type) {
        addPage(pages.size(), type);
    }

    public void addPage(int index, int type) {
        oldPages.clear();
        oldPages.addAll(pages);
        pages.add(index, type);
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        int type = object instanceof LinkFragment ? TYPE_LINK : TYPE_COMMENTS;

        int oldPosition = -1;
        for (int i = 0; i < oldPages.size(); i++) {
            if (type == oldPages.get(i)) {
                oldPosition = i;
                break;
            }
        }

        int newPosition = -1;
        for (int i = 0; i < pages.size(); i++) {
            if (type == pages.get(i)) {
                newPosition = i;
                break;
            }
        }

        if (newPosition == -1) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getItemPosition: " + oldPages + " -> " + pages + " = NONE");
            }
            return POSITION_NONE;
        }

        if (oldPosition == newPosition) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getItemPosition: " + oldPages + " -> " + pages + " = UNCHANGED");
            }
            return POSITION_UNCHANGED;
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getItemPosition: " + oldPages + " -> " + pages + " = " + newPosition);
        }
        return newPosition;
    }

    @Override
    public long getItemId(int position) {
        return pages.get(position);
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Fragment getItem(int position) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getItem: " + position + " -> " + pages.get(position));
        }
        switch (pages.get(position)) {
            case TYPE_LINK:
                return LinkFragment.newInstance(ThingBundle.getTitle(thingBundle),
                        ThingBundle.getLinkUrl(thingBundle));

            case TYPE_COMMENTS:
                return CommentListFragment.newInstance(accountName,
                        ThingBundle.getThingId(thingBundle),
                        ThingBundle.getLinkId(thingBundle));

            default:
                throw new IllegalStateException();
        }
    }
}
