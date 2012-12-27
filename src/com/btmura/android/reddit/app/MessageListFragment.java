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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.LoaderAdapter;
import com.btmura.android.reddit.widget.MessageLoaderAdapter;

import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

/**
 * {@link ThingProviderListFragment} for showing the messages is a thread.
 */
public class MessageListFragment extends ThingProviderListFragment {

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";

    private static final String STATE_SESSION_ID = "sessionId";

    private LoaderAdapter adapter;

    public static MessageListFragment newInstance(String accountName, String thingId) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_THING_ID, thingId);

        MessageListFragment frag = new MessageListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MessageLoaderAdapter(getActivity());
        adapter.setAccountName(getArguments().getString(ARG_ACCOUNT_NAME));
        adapter.setThingId(getArguments().getString(ARG_THING_ID));
        if (savedInstanceState != null) {
            adapter.setSessionId(getArguments().getLong(STATE_SESSION_ID));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        if (adapter.isLoadable()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return adapter.getLoader(getActivity(), args);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);
        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    protected void onSessionIdLoaded(long sessionId) {
        adapter.setSessionId(sessionId);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        throw new IllegalStateException();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SESSION_ID, adapter.getSessionId());
    }
}
