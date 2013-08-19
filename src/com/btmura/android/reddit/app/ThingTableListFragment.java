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

abstract class ThingTableListFragment<C extends ThingListController<?>>
        extends ThingListFragment<C, ThingTableMenuController, ThingTableActionModeController> {

    private ThingBundleHolder thingBundleHolder;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ThingBundleHolder) {
            thingBundleHolder = (ThingBundleHolder) activity;
        }
    }

    @Override
    protected ThingTableMenuController createMenuController(C controller) {
        return new ThingTableMenuController(getActivity(),
                getFragmentManager(),
                controller.getAccountName(),
                controller.getSubreddit(),
                controller.getQuery(),
                thingBundleHolder,
                this);
    }

    @Override
    protected ThingTableActionModeController createActionModeController(C controller) {
        return new ThingTableActionModeController(getActivity(),
                controller.getAccountName(),
                controller.getAdapter());
    }

}
