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

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.CommentReplyFragment.OnCommentReplyListener;
import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.Comments;
import com.btmura.android.reddit.database.CommentLogic.CommentList;
import com.btmura.android.reddit.provider.CommentProvider;
import com.btmura.android.reddit.provider.VoteProvider;
import com.btmura.android.reddit.widget.CommentAdapter;
import com.btmura.android.reddit.widget.OnVoteListener;

public class CommentListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        MultiChoiceModeListener, OnCommentReplyListener, OnVoteListener, CommentList {

    public static final String TAG = "CommentListFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";

    private static final String STATE_SESSION_ID = "sessionId";

    private String accountName;
    private String sessionId;
    private String thingId;
    private boolean sync;
    private CommentAdapter adapter;

    public static CommentListFragment newInstance(String accountName, String thingId) {
        CommentListFragment frag = new CommentListFragment();
        Bundle b = new Bundle(2);
        b.putString(ARG_ACCOUNT_NAME, accountName);
        b.putString(ARG_THING_ID, thingId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        thingId = getArguments().getString(ARG_THING_ID);
        sync = savedInstanceState == null;

        // Don't create a new session if changing configuration.
        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getString(STATE_SESSION_ID);
        } else {
            sessionId = thingId + "-" + System.currentTimeMillis();
        }

        adapter = new CommentAdapter(getActivity(), accountName, this);
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
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        return CommentAdapter.getLoader(getActivity(), accountName, sessionId, thingId, sync);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }
        sync = false;
        CommentAdapter.updateLoader(getActivity(), loader, accountName, sessionId, thingId, sync);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Only comments can be expanded and collapsed.
        if (adapter.getInt(position, CommentAdapter.INDEX_KIND) != Comments.KIND_COMMENT) {
            return;
        }

        // Collapse if expanded. Expand if collapsed.
        if (adapter.getBoolean(position, CommentAdapter.INDEX_EXPANDED)) {
            long[] childIds = CommentLogic.getChildren(this, position);
            CommentProvider.collapseInBackground(getActivity(), id, childIds);
        } else {
            CommentProvider.expandInBackground(getActivity(), sessionId, id);
        }
    }

    public void onVote(String thingId, int likes) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLike thingId: " + thingId + " likes: " + likes);
        }
        if (!TextUtils.isEmpty(accountName)) {
            VoteProvider.voteInBackground(getActivity(), accountName, thingId, likes);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.comment_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.findItem(R.id.menu_reply).setVisible(isReplyItemVisible());
        menu.findItem(R.id.menu_delete).setVisible(isDeleteItemVisible());
        return true;
    }


    private boolean isReplyItemVisible() {
        // You need an account to reply to some comment.
        if (!AccountUtils.isAccount(accountName)) {
            return false;
        }

        // You can only reply to one comment at a time.
        if (getListView().getCheckedItemCount() != 1) {
            return false;
        }

        // The single comment must have a valid id and not be deleted.
        int position = getFirstCheckedPosition();
        String thingId = adapter.getString(position, CommentAdapter.INDEX_THING_ID);
        if (TextUtils.isEmpty(thingId)) {
            return false;
        }

        String author = adapter.getString(position, CommentAdapter.INDEX_AUTHOR);
        if (Comments.DELETED.equals(author)) {
            return false;
        }

        return true;
    }

    private boolean isDeleteItemVisible() {
        // You need an account to delete items.
        if (!AccountUtils.isAccount(accountName)) {
            return false;
        }

        // We can delete as many items we want.
        if (getListView().getCheckedItemCount() == 0) {
            return false;
        }

        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            if (checked.get(i)) {

                // All items must be authored by the current account.
                String author = adapter.getString(i, CommentAdapter.INDEX_AUTHOR);
                if (!author.equals(accountName)) {
                    return false;
                }

                // All items must have a valid id.
                String thingId = adapter.getString(i, CommentAdapter.INDEX_THING_ID);
                if (TextUtils.isEmpty(thingId)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                return handleReply(mode);

            case R.id.menu_delete:
                return handleDelete(mode);

            default:
                return false;
        }
    }

    private boolean handleReply(ActionMode mode) {
        int position = getFirstCheckedPosition();
        if (position != -1) {
            String thingId = adapter.getString(position, CommentAdapter.INDEX_THING_ID);
            String author = adapter.getString(position, CommentAdapter.INDEX_AUTHOR);
            long sessionCreationTime = adapter.getLong(position,
                    CommentAdapter.INDEX_SESSION_CREATION_TIME);

            Bundle extras = new Bundle(3);
            extras.putInt(Comments.COLUMN_NESTING, CommentLogic.getInsertNesting(this, position));
            extras.putInt(Comments.COLUMN_SEQUENCE, CommentLogic.getInsertSequence(this, position));
            extras.putLong(Comments.COLUMN_SESSION_TIMESTAMP, sessionCreationTime);

            CommentReplyFragment frag = CommentReplyFragment.newInstance(thingId, author, extras);
            frag.setTargetFragment(this, 0);
            frag.show(getFragmentManager(), CommentReplyFragment.TAG);
        }

        mode.finish();
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

    public void onCommentReply(String replyAccountName, String replyThingId,
            String text, Bundle extras) {
        int nesting = extras.getInt(Comments.COLUMN_NESTING);
        int sequence = extras.getInt(Comments.COLUMN_SEQUENCE);
        long sessionCreationTime = extras.getLong(Comments.COLUMN_SESSION_TIMESTAMP);
        CommentProvider.insertInBackground(getActivity(), replyAccountName, text, nesting,
                thingId, sequence, sessionId, sessionCreationTime, replyThingId);
    }

    private boolean handleDelete(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        String[] thingIds = new String[ids.length];
        boolean[] hasChildren = new boolean[ids.length];
        fillCheckedInfo(thingIds, hasChildren);
        CommentProvider.deleteInBackground(getActivity(), accountName, thingId, ids, thingIds,
                hasChildren);
        mode.finish();
        return true;
    }

    private void fillCheckedInfo(String[] thingIds, boolean[] hasChildren) {
        SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
        int count = adapter.getCount();
        int j = 0;
        for (int i = 0; i < count; i++) {
            if (checkedPositions.get(i)) {
                thingIds[j] = adapter.getString(i, CommentAdapter.INDEX_THING_ID);
                hasChildren[j] = CommentLogic.hasChildren(this, i);
                j++;
            }
        }
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SESSION_ID, sessionId);
    }

    @Override
    public void onDestroy() {
        if (!getActivity().isChangingConfigurations()) {
            CommentAdapter.deleteSessionData(getActivity(), sessionId);
        }
        super.onDestroy();
    }

    public int getCommentCount() {
        return adapter.getCount();
    }

    public long getCommentId(int position) {
        return adapter.getLong(position, CommentAdapter.INDEX_ID);
    }

    public int getCommentNesting(int position) {
        return adapter.getInt(position, CommentAdapter.INDEX_NESTING);
    }

    public int getCommentSequence(int position) {
        return adapter.getInt(position, CommentAdapter.INDEX_SEQUENCE);
    }
}
