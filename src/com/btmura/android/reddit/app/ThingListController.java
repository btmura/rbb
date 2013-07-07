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

import android.view.ActionMode;
import android.view.Menu;
import android.widget.ListView;

import com.btmura.android.reddit.widget.AbstractThingListAdapter;

interface ThingListController<A extends AbstractThingListAdapter> extends Controller<A> {

    ThingBundle getThingBundle(int position);

    // Actions on things.

    void author(int position);

    void copyUrl(int position);

    void hide(int position, boolean hide);

    void save(int position, boolean save);

    void select(int position);

    void subreddit(int position);

    void vote(int position, int action);

    // Getters.

    String getAccountName();

    int getEmptyText();

    int getFilter();

    String getMoreId();

    String getNextMoreId();

    String getParentSubreddit();

    String getQuery();

    String getSelectedLinkId();

    String getSelectedThingId();

    long getSessionId();

    String getSubreddit();

    boolean hasAccountName();

    boolean hasCursor();

    boolean hasNextMoreId();

    boolean hasQuery();

    boolean isSingleChoice();

    boolean isSwipeDismissable(int position);

    // Setters.

    void setAccountName(String accountName);

    void setEmptyText(int emptyText);

    void setFilter(int filter);

    void setMoreId(String moreId);

    void setParentSubreddit(String parentSubreddit);

    void setSessionId(long sessionId);

    void setSelectedPosition(int position);

    void setSelectedThing(String thingId, String linkId);

    void setSubreddit(String subreddit);

    void setThingBodyWidth(int thingBodyWidth);

    // Menu preparation methods.

    void onPrepareActionMode(ActionMode mode, Menu menu, ListView listView);
}
