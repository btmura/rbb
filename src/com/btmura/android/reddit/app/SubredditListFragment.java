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

import android.view.View;
import android.widget.ListView;

import com.btmura.android.reddit.widget.SubredditAdapter;
import com.btmura.android.reddit.widget.SubredditView;

abstract class SubredditListFragment<C extends SubredditListController<A>, AC extends ActionModeController, A extends SubredditAdapter>
        extends AbstractListFragment<C, AC, A> {

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        controller.setSelectedPosition(position);
        if (controller.isSingleChoice() && view instanceof SubredditView) {
            ((SubredditView) view).setChosen(true);
        }
    }
}
