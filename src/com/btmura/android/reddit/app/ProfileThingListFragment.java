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

import android.app.Activity;
import android.os.Bundle;

import com.btmura.android.reddit.util.ComparableFragments;

public class ProfileThingListFragment
        extends ThingListFragment<ProfileThingListController, ThingTableActionModeController> {

    private ThingBundleHolder thingBundleHolder;

    public static ProfileThingListFragment newInstance(String accountName, String profileUser,
            int filter, boolean singleChoice) {
        Bundle args = new Bundle(4);
        args.putString(ProfileThingListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(ProfileThingListController.EXTRA_PROFILE_USER, profileUser);
        args.putInt(ProfileThingListController.EXTRA_FILTER, filter);
        args.putBoolean(ProfileThingListController.EXTRA_SINGLE_CHOICE, singleChoice);

        ProfileThingListFragment frag = new ProfileThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ThingBundleHolder) {
            thingBundleHolder = (ThingBundleHolder) activity;
        }
    }

    @Override
    protected ProfileThingListController createController() {
        return new ProfileThingListController(getActivity(), getArguments(), this);
    }

    @Override
    protected ThingTableActionModeController createActionModeController(
            ProfileThingListController controller) {
        return new ThingTableActionModeController(getActivity(),
                getFragmentManager(),
                controller.getAccountName(),
                controller.getSubreddit(),
                controller.getQuery(),
                controller.getAdapter(),
                thingBundleHolder);
    }

    public String getProfileUser() {
        return controller.getProfileUser();
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o)
                && ComparableFragments.equalStrings(this, o,
                        ProfileThingListController.EXTRA_ACCOUNT_NAME)
                && ComparableFragments.equalStrings(this, o,
                        ProfileThingListController.EXTRA_PROFILE_USER)
                && ComparableFragments.equalInts(this, o,
                        ProfileThingListController.EXTRA_FILTER);
    }
}
