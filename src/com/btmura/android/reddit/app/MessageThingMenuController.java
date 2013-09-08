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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.btmura.android.reddit.R;

public class MessageThingMenuController implements MenuController {

    private final Context context;
    private final Refreshable refreshable;

    public MessageThingMenuController(Context context, Refreshable refreshable) {
        this.context = context;
        this.refreshable = refreshable;
    }

    @Override
    public void restoreInstanceState(Bundle savedInstanceState) {
        // No state to restore
    }

    @Override
    public void saveInstanceState(Bundle outState) {
        // No state to save
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_thing_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_message:
                handleNewMessage();
                return true;

            case R.id.menu_refresh:
                handleRefresh();
                return true;

            default:
                return false;
        }
    }

    private void handleNewMessage() {
        MenuHelper.startComposeActivity(context,
                ComposeActivity.MESSAGE_TYPE_SET,
                null,
                null,
                null,
                null,
                null,
                false);
    }

    private void handleRefresh() {
        refreshable.refresh();
    }
}
