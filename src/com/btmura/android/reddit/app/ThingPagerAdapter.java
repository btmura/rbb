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

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.content.ThingDataLoader.ThingData;
import com.btmura.android.reddit.database.Kinds;

import java.util.ArrayList;

/**
 * {@link FragmentStateItemPagerAdapter} for controlling what is shown in the
 * ViewPager for looking at a thing. In multipane layouts, it is responsible for
 * handling the right side of the screen.
 */
public class ThingPagerAdapter extends FragmentStateItemPagerAdapter {

  public static final String TAG = "ThingPagerAdapter";

  public static final int TYPE_LINK = 0;
  public static final int TYPE_COMMENTS = 1;
  public static final int TYPE_MESSAGE = 2;

  public static final int PAGE_LINK = TYPE_LINK;
  public static final int PAGE_COMMENTS = TYPE_COMMENTS;

  private static final String STATE_PAGE_TYPES = "pageTypes";
  private static final String STATE_OLD_PAGE_TYPES = "oldPageTypes";

  private final ArrayList<Integer> pageTypes = new ArrayList<Integer>(2);
  private final ArrayList<Integer> oldPageTypes = new ArrayList<Integer>(2);
  private final String accountName;
  private final ThingData thingData;

  public ThingPagerAdapter(
      FragmentManager fm,
      String accountName,
      ThingData thingData) {
    super(fm);
    this.accountName = accountName;
    this.thingData = thingData;
    setupPages(thingData.parent);
  }

  @Override
  public Parcelable saveState() {
    Bundle state = (Bundle) super.saveState();
    if (state != null) {
      state.putIntegerArrayList(STATE_PAGE_TYPES, pageTypes);
      state.putIntegerArrayList(STATE_OLD_PAGE_TYPES, oldPageTypes);
    }
    return state;
  }

  @Override
  public void restoreState(Parcelable state, ClassLoader loader) {
    super.restoreState(state, loader);
    if (state != null) {
      Bundle bundle = (Bundle) state;
      pageTypes.clear();
      pageTypes.addAll(bundle.getIntegerArrayList(STATE_PAGE_TYPES));
      oldPageTypes.clear();
      oldPageTypes.addAll(bundle.getIntegerArrayList(STATE_OLD_PAGE_TYPES));
      notifyDataSetChanged();
    }
  }

  private void setupPages(ThingBundle thingBundle) {
    switch (thingBundle.getKind()) {
      case Kinds.KIND_LINK:
        if (thingBundle.hasLinkUrl()) {
          addPage(TYPE_LINK);
        }
        addPage(TYPE_COMMENTS);
        break;

      case Kinds.KIND_MESSAGE:
        addPage(TYPE_MESSAGE);
        break;
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
        String url = thingData.parent.getLinkUrl().toString();
        return LinkFragment.newInstance(url);

      case TYPE_COMMENTS:
        // TODO: Standardize parentId/childId selector throughout app.
        if (thingData.child != null) {
          return CommentListFragment.newInstance(accountName,
              thingData.child.getThingId(),
              thingData.parent.getThingId());
        } else {
          return CommentListFragment.newInstance(accountName,
              thingData.parent.getThingId(),
              null);
        }

      case TYPE_MESSAGE:
        return MessageThreadListFragment.newInstance(accountName,
            thingData.parent.getThingId());

      default:
        throw new IllegalArgumentException();
    }
  }
}
