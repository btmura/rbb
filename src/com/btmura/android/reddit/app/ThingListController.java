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
import android.view.Menu;
import android.widget.ListView;

interface ThingListController {

    boolean isLoadable();

    void restoreState(Bundle inState);

    void saveState(Bundle outState);

    Loader<Cursor> createLoader();

    Bundle getThingBundle(int position);

    // Actions on things.

    void author(int position);

    void copyUrl(int position);

    void hide(int position, boolean hide);

    void save(int position, boolean save);

    void select(int position);

    void subreddit(int position);

    void vote(int position, int action);

    // Getters.

    String getMoreId();

    String getNextMoreId();

    long getSessionId();

    boolean hasNextMoreId();

    boolean isSwipeDismissable(int position);

    // Setters.

    void setMoreId(String moreId);

    void setSessionId(long sessionId);

    // Menu preparation methods.

    void prepareActionMenu(Menu menu, ListView listView, int position);
}
