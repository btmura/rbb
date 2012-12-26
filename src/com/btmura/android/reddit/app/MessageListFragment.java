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

import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

/**
 * {@link ThingProviderListFragment} for showing the messages is a thread.
 */
public class MessageListFragment extends ThingProviderListFragment {

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";

    public static MessageListFragment newInstance(String accountName, String thingId) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_THING_ID, thingId);

        MessageListFragment frag = new MessageListFragment();
        frag.setArguments(args);
        return frag;
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
    }

    @Override
    protected void onSessionIdLoaded(long sessionId) {
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
    }

    public void onLoaderReset(Loader<Cursor> arg0) {
    }
}
