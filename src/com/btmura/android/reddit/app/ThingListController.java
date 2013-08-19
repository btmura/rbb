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

import com.btmura.android.reddit.widget.AbstractThingListAdapter;

interface ThingListController<A extends AbstractThingListAdapter> extends Controller<A> {

    ThingBundle getThingBundle(int position);

    void onThingSelected(int position);

    // Getters.

    String getAccountName();

    int getFilter();

    String getMoreId();

    String getNextMoreId();

    String getQuery();

    String getSubreddit();

    boolean hasNextMoreId();

    boolean isSingleChoice();

    // Setters.

    void setMoreId(String moreId);

    void setParentSubreddit(String parentSubreddit);

    void setSelectedPosition(int position);

    void setSelectedThing(String thingId, String linkId);

    void setSubreddit(String subreddit);

    void setThingBodyWidth(int thingBodyWidth);
}
