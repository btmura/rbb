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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.ActionMode;
import android.view.Menu;
import android.widget.ListView;

import com.btmura.android.reddit.widget.SubredditAdapter;

interface SubredditListController {

    boolean isLoadable();

    void restoreInstanceState(Bundle savedInstanceState);

    void saveInstanceState(Bundle outState);

    Loader<Cursor> createLoader();

    boolean swapCursor(Cursor cursor);

    // Actions on subreddits

    void add(ListView listView, int position);

    void subreddit(int position);

    void delete(ListView listView, int position);

    // Menu creation and preparation methods

    boolean createActionMode(ActionMode mode, Menu menu, ListView listView);

    boolean prepareActionMode(ActionMode mode, Menu menu, ListView listView);

    // Getters

    String getAccountName();

    String getActionAccountName();

    SubredditAdapter getAdapter();

    String getSelectedSubreddit();

    boolean hasActionAccountName();

    boolean isSingleChoice();

    boolean isSwipeDismissable(int position);

    // Setters

    void setAccountName(String accountName);

    void setActionAccountName(String actionAccountName);

    String setSelectedPosition(int position);

    void setSelectedSubreddit(String subreddit);
}
