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

package com.btmura.android.reddit.fragment;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
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
import com.btmura.android.reddit.provider.VoteProvider;
import com.btmura.android.reddit.widget.CommentAdapter;
import com.btmura.android.reddit.widget.OnVoteListener;

public class CommentListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        MultiChoiceModeListener, OnVoteListener {

    public static final String TAG = "CommentListFragment";

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_THING_ID = "ti";

    private String accountName;
    private String sessionId;
    private String thingId;
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
        sessionId = thingId + "-" + System.currentTimeMillis();
        adapter = new CommentAdapter(getActivity(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        Uri uri = CommentAdapter.createUri(accountName, sessionId, thingId, true);
        return CommentAdapter.createLoader(getActivity(), uri, sessionId);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Uri uri = CommentAdapter.createUri(accountName, sessionId, thingId, false);
        CursorLoader cursorLoader = (CursorLoader) loader;
        cursorLoader.setUri(uri);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
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
        boolean hasAccount = AccountUtils.isAccount(accountName);
        if (hasAccount) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.comment_action_menu, menu);
        }
        return hasAccount;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean showReply = getListView().getCheckedItemCount() == 1;
        menu.findItem(R.id.menu_reply).setVisible(showReply);
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                handleReply(mode);
                return true;

            default:
                return false;
        }
    }

    private void handleReply(ActionMode mode) {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int position = -1;
        int size = adapter.getCount();
        for (int i = 0; i < size; i++) {
            if (checked.get(i)) {
                position = i;
                break;
            }
        }

        if (position != -1) {
            CommentReplyFragment frag = CommentReplyFragment.newInstance(accountName,
                    adapter.getCommentBundle(position));
            frag.show(getFragmentManager(), CommentReplyFragment.TAG);
        }

        mode.finish();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }
}
