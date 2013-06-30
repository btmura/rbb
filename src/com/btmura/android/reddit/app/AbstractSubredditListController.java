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

import java.util.Arrays;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.widget.ListView;

import com.btmura.android.reddit.provider.Provider;

abstract class AbstractSubredditListController implements SubredditListController {

    protected final Context context;

    AbstractSubredditListController(Context context) {
        this.context = context;
    }

    @Override
    public void add(ListView listView, int position) {
        String accountName = getSelectedAccountName();
        String[] subreddits = getCheckedSubreddits(listView);
        Provider.addSubredditAsync(context, accountName, subreddits);
    }

    @Override
    public void subreddit(int position) {
        MenuHelper.startSidebarActivity(context, getSubreddit(position));
    }

    @Override
    public void delete(ListView listView, int position) {
        String accountName = getSelectedAccountName();
        String[] subreddits = getCheckedSubreddits(listView);
        Provider.removeSubredditAsync(context, accountName, subreddits);
    }

    protected abstract String getSelectedAccountName();

    protected String[] getCheckedSubreddits(ListView listView) {
        SparseBooleanArray checked = listView.getCheckedItemPositions();
        int checkedCount = listView.getCheckedItemCount();
        String[] subreddits = new String[checkedCount];
        int i = 0;
        int j = 0;
        int count = getCount();
        for (; i < count; i++) {
            if (checked.get(i) && isDeletable(i)) {
                subreddits[j++] = getSubreddit(i);
            }
        }
        return Arrays.copyOf(subreddits, j);
    }

    protected abstract int getCount();

    protected abstract String getSubreddit(int position);

    protected abstract boolean isDeletable(int position);
}
