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

import android.app.Activity;
import android.database.Cursor;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditView;

abstract class SubredditListFragment<C extends SubredditListController<A>, MC extends MenuController, AC extends ActionModeController, A extends SubredditAdapter>
    extends AbstractListFragment<C, MC, AC, A> {

  private OnSubredditSelectedListener listener;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnSubredditSelectedListener) {
      listener = (OnSubredditSelectedListener) activity;
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    super.onLoadFinished(loader, cursor);
    SubredditAdapter adapter = controller.getAdapter();
    if (adapter.getCursor() != null && adapter.getCount() > 0
        && TextUtils.isEmpty(controller.getSelectedSubreddit())) {
      String subreddit = adapter.getName(0);
      controller.setSelectedSubreddit(subreddit);
      if (listener != null) {
        listener.onSubredditSelected(null, subreddit, true);
      }
    }
  }

  @Override
  public void onListItemClick(ListView lv, View v, int pos, long id) {
    controller.setSelectedPosition(pos);
    if (controller.isSingleChoice() && v instanceof SubredditView) {
      ((SubredditView) v).setChosen(true);
    }
    if (listener != null) {
      listener.onSubredditSelected(v, controller.getSelectedSubreddit(),
          false);
    }
  }
}
