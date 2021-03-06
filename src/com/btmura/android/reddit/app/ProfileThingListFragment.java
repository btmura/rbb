/*
 * Copyright (C) 2013 Brian Muramatsu
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

import com.btmura.android.reddit.util.ComparableFragments;

public class ProfileThingListFragment
    extends ThingTableListFragment<ProfileThingListController> {

  public static ProfileThingListFragment newInstance(
      String accountName,
      String profileUser,
      int filter,
      boolean singleChoice) {
    Bundle args = new Bundle(4);
    args.putString(ProfileThingListController.EXTRA_ACCOUNT_NAME, accountName);
    args.putString(ProfileThingListController.EXTRA_PROFILE_USER, profileUser);
    args.putInt(ProfileThingListController.EXTRA_FILTER, filter);
    args.putBoolean(ProfileThingListController.EXTRA_SINGLE_CHOICE,
        singleChoice);

    ProfileThingListFragment frag = new ProfileThingListFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  protected ProfileThingListController createController() {
    return new ProfileThingListController(getActivity(), getArguments(), this);
  }

  @Override
  public boolean equalFragments(ComparableFragment o) {
    return ComparableFragments.equalClasses(this, o)
        && ComparableFragments.equalStrings(this, o,
        ProfileThingListController.EXTRA_ACCOUNT_NAME)
        && ComparableFragments.equalStrings(this, o,
        ProfileThingListController.EXTRA_PROFILE_USER)
        && ComparableFragments.equalInts(this, o,
        ProfileThingListController.EXTRA_FILTER);
  }
}
