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

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;

class CommentMenuController implements MenuController {

    private final Context context;
    private final Refreshable refreshable;
    private final Filterable filterable;

    CommentMenuController(Context context, Refreshable refreshable, Filterable filterable) {
        this.context = context;
        this.refreshable = refreshable;
        this.filterable = filterable;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.comment_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_comments:
                handleRefresh();
                return true;

            case R.id.menu_sort_comments:
                handleSort();
                return true;

            default:
                return false;
        }
    }

    private void handleRefresh() {
        refreshable.refresh();
    }

    private void handleSort() {
        MenuHelper.showSortCommentsDialog(context, filterable);
    }
}
