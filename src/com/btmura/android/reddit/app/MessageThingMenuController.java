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

class MessageThingMenuController implements MenuController {

    private final Context context;
    private final String accountName;
    private final ThingHolder thingHolder;
    private final Refreshable refreshable;

    MessageThingMenuController(Context context,
            String accountName,
            ThingHolder thingHolder,
            Refreshable refreshable) {
        this.context = context;
        this.accountName = accountName;
        this.thingHolder = thingHolder;
        this.refreshable = refreshable;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.message_thing_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean hasThing = thingHolder != null && thingHolder.isShowingThing();
        boolean showNewMessage = !hasThing;
        boolean showRefresh = !hasThing;
        menu.findItem(R.id.menu_new_message).setVisible(showNewMessage);
        menu.findItem(R.id.menu_refresh).setVisible(showRefresh);
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
        MenuHelper.startNewMessageComposer(context, accountName, null);
    }

    private void handleRefresh() {
        refreshable.refresh();
    }
}
