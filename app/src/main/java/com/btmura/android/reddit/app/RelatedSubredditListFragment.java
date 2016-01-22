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
import com.btmura.android.reddit.widget.RelatedSubredditAdapter;

public class RelatedSubredditListFragment
    extends SubredditListFragment<RelatedSubredditListController,
    NoMenuController,
    SubredditActionModeController,
    RelatedSubredditAdapter>
    implements LeftFragment, RightFragment {

  private AccountResultHolder accountResultHolder;

  public static RelatedSubredditListFragment newInstance(
      String accountName,
      String subreddit,
      boolean singleChoice) {
    Bundle args = new Bundle(3);
    args.putString(RelatedSubredditListController.EXTRA_ACCOUNT_NAME,
        accountName);
    args.putString(RelatedSubredditListController.EXTRA_SIDEBAR_SUBREDDIT,
        subreddit);
    args.putBoolean(RelatedSubredditListController.EXTRA_SINGLE_CHOICE,
        singleChoice);

    RelatedSubredditListFragment frag = new RelatedSubredditListFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public boolean equalFragments(ComparableFragment o) {
    return ComparableFragments.equalClasses(this, o)
        && ComparableFragments.equalStrings(this, o,
        RelatedSubredditListController.EXTRA_ACCOUNT_NAME)
        && ComparableFragments.equalStrings(this, o,
        RelatedSubredditListController.EXTRA_SIDEBAR_SUBREDDIT);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof AccountResultHolder) {
      accountResultHolder = (AccountResultHolder) activity;
    }
  }

  @Override
  protected RelatedSubredditListController createController() {
    return new RelatedSubredditListController(getActivity(), getArguments());
  }

  @Override
  protected NoMenuController createMenuController(
      RelatedSubredditListController controller) {
    return NoMenuController.INSTANCE;
  }

  @Override
  protected SubredditActionModeController
  createActionModeController(RelatedSubredditListController controller) {
    return new SubredditActionModeController(getActivity(),
        getFragmentManager(),
        controller.getAdapter(),
        accountResultHolder);
  }

  @Override
  public void setSelectedThing(String thingId, String linkId) {
  }
}
