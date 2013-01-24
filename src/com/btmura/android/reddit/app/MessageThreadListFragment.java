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

import android.app.Activity;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.ThingMenuFragment.OnThingMenuEventListener;
import com.btmura.android.reddit.app.ThingMenuFragment.ThingMenuEventListenerHolder;
import com.btmura.android.reddit.widget.MessageThreadAdapter;

/**
 * {@link ThingProviderListFragment} for showing the messages in a thread.
 */
public class MessageThreadListFragment extends ThingProviderListFragment implements
        MultiChoiceModeListener,
        OnThingMenuEventListener,
        ThingHolder {

    public static final String TAG = "MessageThreadListFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";

    private static final String STATE_SESSION_ID = "sessionId";

    private OnThingEventListener listener;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingEventListener) {
            listener = (OnThingEventListener) activity;
        }
        if (activity instanceof ThingMenuEventListenerHolder) {
            ((ThingMenuEventListenerHolder) activity).setOnThingMenuEventListener(this);
        }
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
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
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

        if (adapter.getCount() > 0 && listener != null) {
            listener.onThingLoaded(this);
        }
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

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.message_thread_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        mode.setTitle(getResources().getQuantityString(R.plurals.messages, count, count));

        boolean showNewComment = count == 1;
        if (showNewComment) {
            showNewComment &= !TextUtils.isEmpty(adapter.getThingId(getFirstCheckedPosition()));
        }

        menu.findItem(R.id.menu_new_comment).setVisible(showNewComment);
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_comment:
                handleNewComment(getFirstCheckedPosition());
                mode.finish();
                return true;

            default:
                return false;
        }
    }

    private void handleNewComment(int position) {
        String user = adapter.getUser(position);

        Bundle extras = new Bundle(3);

        // Message threads are odd in that the thing id doesn't refer to the
        // topmost message, so the actions may not match up with the id. So get
        // the parent id from the first element.
        extras.putString(ComposeActivity.EXTRA_MESSAGE_PARENT_THING_ID, adapter.getThingId(0));
        extras.putLong(ComposeActivity.EXTRA_MESSAGE_SESSION_ID, adapter.getSessionId());
        extras.putString(ComposeActivity.EXTRA_MESSAGE_THING_ID, adapter.getThingId(position));

        MenuHelper.startComposeActivity(getActivity(), ComposeActivity.MESSAGE_REPLY_TYPE_SET,
                null, user, null, extras, false);
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SESSION_ID, adapter.getSessionId());
    }

    // OnThingMenuEventListener implementation

    public void onSavedItemSelected() {
        throw new UnsupportedOperationException();
    }

    public void onUnsavedItemSelected() {
        throw new UnsupportedOperationException();
    }

    public void onNewItemSelected() {
        handleNewComment(0);
    }

    // ThingHolder implementation

    public String getThingId() {
        return adapter.getThingId();
    }

    public String getAuthor() {
        return adapter.getUser(0);
    }

    public String getTitle() {
        return adapter.getSubject();
    }

    public String getUrl() {
        throw new UnsupportedOperationException();
    }

    public boolean isReplyable() {
        return true;
    }

    public boolean isSaved() {
        return false;
    }

    public boolean isSelf() {
        return true;
    }

    private int getFirstCheckedPosition() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int size = adapter.getCount();
        for (int i = 0; i < size; i++) {
            if (checked.get(i)) {
                return i;
            }
        }
        return -1;
    }
}
