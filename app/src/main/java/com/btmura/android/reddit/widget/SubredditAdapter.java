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

package com.btmura.android.reddit.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import com.btmura.android.reddit.util.Objects;

/** BaseAdapter that handles lists of subreddits. */
public abstract class SubredditAdapter extends BaseCursorAdapter {

  public static final String TAG = "SubredditAdapter";

  protected final boolean singleChoice;
  protected String accountName;
  protected String selectedSubreddit;

  protected SubredditAdapter(Context ctx, boolean singleChoice) {
    super(ctx, null, 0);
    this.singleChoice = singleChoice;
  }

  @Override
  public View newView(Context ctx, Cursor c, ViewGroup parent) {
    return new SubredditView(ctx);
  }

  public void setAccountName(String accountName) {
    if (!Objects.equals(this.accountName, accountName)) {
      this.accountName = accountName;
      notifyDataSetChanged();
    }
  }

  public String getSelectedSubreddit() {
    return selectedSubreddit;
  }

  public void setSelectedSubreddit(String subreddit) {
    if (!Objects.equals(selectedSubreddit, subreddit)) {
      selectedSubreddit = subreddit;
      notifyDataSetChanged();
    }
  }

  public String setSelectedPosition(int pos) {
    String subreddit = getName(pos);
    setSelectedSubreddit(subreddit);
    return subreddit;
  }

  public abstract String getName(int pos);

  public boolean isDeletable(int pos) {
    return true;
  }

  public boolean isSingleChoice() {
    return singleChoice;
  }
}
