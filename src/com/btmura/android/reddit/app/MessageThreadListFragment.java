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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.MessageThreadAdapter;

/**
 * {@link ThingProviderListFragment} for showing the messages in a thread.
 */
public class MessageThreadListFragment extends ThingProviderListFragment {

    public static final String TAG = "MessageThreadListFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";

    private static final String STATE_SESSION_ID = "sessionId";

    public static final String EXTRA_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_THING_ID = "thingId";

    private MessageThreadAdapter adapter;

    public static MessageThreadListFragment newInstance(String accountName, String thingId) {
        Bundle args = new Bundle(2);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_THING_ID, thingId);

        MessageThreadListFragment frag = new MessageThreadListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MessageThreadAdapter(getActivity());
        adapter.setAccountName(getArguments().getString(ARG_ACCOUNT_NAME));
        adapter.setThingId(getArguments().getString(ARG_THING_ID));
        if (savedInstanceState != null) {
            adapter.setSessionId(savedInstanceState.getLong(STATE_SESSION_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        registerForContextMenu(l);
        return v;
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
        return adapter.getLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);

        adapter.updateLoaderUri(getActivity(), loader);
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.message_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_reply:
                return handleReply(info);

            default:
                return false;
        }
    }

    private boolean handleReply(AdapterContextMenuInfo info) {
        String user = adapter.getUser(info.position);
        Bundle extras = new Bundle(3);
        extras.putString(EXTRA_PARENT_THING_ID, adapter.getThingId());
        extras.putLong(EXTRA_SESSION_ID, adapter.getSessionId());
        extras.putString(EXTRA_THING_ID, adapter.getThingId(info.position));
        MenuHelper.startComposeActivity(getActivity(),
                ComposeActivity.COMPOSITION_MESSAGE_REPLY, user, null, extras);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SESSION_ID, adapter.getSessionId());
    }
}
