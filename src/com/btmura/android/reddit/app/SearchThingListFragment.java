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

public class SearchThingListFragment
        extends ThingListFragment<SearchThingListController, SearchThingMenuController,
        ThingTableActionModeController>
        implements Filterable {

    private ThingHolder thingHolder;

    public static SearchThingListFragment newInstance(String accountName, String subreddit,
            String query, int filter, boolean singleChoice) {
        Bundle args = new Bundle(6);
        args.putString(SearchThingListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(SearchThingListController.EXTRA_PARENT_SUBREDDIT, subreddit);
        args.putString(SearchThingListController.EXTRA_SUBREDDIT, subreddit);
        args.putString(SearchThingListController.EXTRA_QUERY, query);
        args.putInt(SearchThingListController.EXTRA_FILTER, filter);
        args.putBoolean(SearchThingListController.EXTRA_SINGLE_CHOICE, singleChoice);

        SearchThingListFragment frag = new SearchThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ThingHolder) {
            thingHolder = (ThingHolder) activity;
        }
    }

    @Override
    protected SearchThingListController createController() {
        return new SearchThingListController(getActivity(), getArguments(), this);
    }

    @Override
    protected SearchThingMenuController createMenuController(
            SearchThingListController controller) {
        return new SearchThingMenuController(getActivity(), thingHolder, this, this);
    }

    @Override
    protected ThingTableActionModeController createActionModeController(
            SearchThingListController controller) {
        return new ThingTableActionModeController(getActivity(),
                controller.getAccountName(),
                controller.getSwipeAction(),
                controller.getAdapter());
    }

    @Override
    public boolean equalFragments(ComparableFragment o) {
        return ComparableFragments.equalClasses(this, o)
                && ComparableFragments.equalStrings(this, o,
                        SearchThingListController.EXTRA_ACCOUNT_NAME)
                && ComparableFragments.equalStrings(this, o,
                        SearchThingListController.EXTRA_SUBREDDIT)
                && ComparableFragments.equalStrings(this, o,
                        SearchThingListController.EXTRA_QUERY);
    }

    @Override
    public int getFilter() {
        return controller.getFilter();
    }

    @Override
    public void setFilter(int filter) {
        // TODO(btmura): remove code duplication with CommentListFragment.
        if (filter != controller.getFilter()) {
            controller.setFilter(filter);
            controller.swapCursor(null);
            setListAdapter(controller.getAdapter());
            setListShown(false);
            getLoaderManager().restartLoader(0, null, this);
        }
    }
}
