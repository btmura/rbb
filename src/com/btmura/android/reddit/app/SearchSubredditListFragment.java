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

import com.btmura.android.reddit.app.AbstractBrowserActivity.LeftFragment;
import com.btmura.android.reddit.app.AbstractBrowserActivity.RightFragment;
import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.widget.SearchSubredditAdapter;

public class SearchSubredditListFragment
    extends SubredditListFragment<SearchSubredditListController,
    NoMenuController,
    SubredditActionModeController,
    SearchSubredditAdapter>
    implements LeftFragment, RightFragment {

  private AccountResultHolder accountResultHolder;

  public static SearchSubredditListFragment newInstance(
      String accountName,
      String query,
      boolean singleChoice) {
    Bundle args = new Bundle(3);
    args.putString(SearchSubredditListController.EXTRA_ACCOUNT_NAME,
        accountName);
    args.putString(SearchSubredditListController.EXTRA_QUERY, query);
    args.putBoolean(SearchSubredditListController.EXTRA_SINGLE_CHOICE,
        singleChoice);

    SearchSubredditListFragment frag = new SearchSubredditListFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public boolean equalFragments(ComparableFragment o) {
    return ComparableFragments.equalClasses(this, o)
        && ComparableFragments.equalStrings(this, o,
        SearchSubredditListController.EXTRA_ACCOUNT_NAME)
        && ComparableFragments.equalStrings(this, o,
        SearchSubredditListController.EXTRA_QUERY);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof AccountResultHolder) {
      accountResultHolder = (AccountResultHolder) activity;
    }
  }

  @Override
  protected SearchSubredditListController createController() {
    return new SearchSubredditListController(getActivity(), getArguments());
  }

  @Override
  protected NoMenuController createMenuController(
      SearchSubredditListController controller) {
    return NoMenuController.INSTANCE;
  }

  @Override
  protected SubredditActionModeController createActionModeController(
      SearchSubredditListController controller) {
    return new SubredditActionModeController(getActivity(),
        getFragmentManager(),
        controller.getAdapter(),
        accountResultHolder);
  }

  @Override
  public void setSelectedThing(String thingId, String linkId) {
  }
}
