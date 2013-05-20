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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
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
import com.btmura.android.reddit.content.CommentLoader;
import com.btmura.android.reddit.widget.CommentAdapter;
import com.btmura.android.reddit.widget.OnVoteListener;

public class CommentListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        MultiChoiceModeListener,
        OnVoteListener {

    public static final String TAG = "CommentListFragment";

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_THING_ID = "thingId";
    private static final String ARG_LINK_ID = "linkId";

    /** Optional string that has a title for the thing. */
    private static final String ARG_TITLE = "title";

    /** Optional string that is a complete url to the comments. */
    private static final String ARG_URL = "permaLink";

    /** Optional bit mask for controlling fragment appearance. */
    private static final String ARG_FLAGS = "flags";

    /** Flag to immediately show link button if thing definitely has a link. */
    public static final int FLAG_SHOW_LINK_MENU_ITEM = 0x1;

    private static final String STATE_TITLE = ARG_TITLE;
    private static final String STATE_URL = ARG_URL;

    private CommentAdapter adapter;
    private CommentListFragmentController controller;
    private String title;
    private CharSequence url;

    public static CommentListFragment newInstance(String accountName, String thingId,
            String linkId, String title, CharSequence url, int flags) {
        Bundle args = new Bundle(6);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_THING_ID, thingId);
        args.putString(ARG_LINK_ID, linkId);
        args.putString(ARG_TITLE, title);
        args.putCharSequence(ARG_URL, url);
        args.putInt(ARG_FLAGS, flags);

        CommentListFragment frag = new CommentListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        adapter = new CommentAdapter(getActivity(), accountName, this);
        controller = new CommentListFragmentController(getActivity(), accountName, adapter);

        // Don't create a new session if changing configuration.
        if (savedInstanceState != null) {
            title = savedInstanceState.getString(STATE_TITLE);
            url = savedInstanceState.getCharSequence(STATE_URL);
        } else {
            title = getArguments().getString(ARG_TITLE);
            url = getArguments().getCharSequence(ARG_URL);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
        l.setDivider(null);
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
        String accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        String thingId = getArguments().getString(ARG_THING_ID);
        String linkId = getArguments().getString(ARG_LINK_ID);
        return new CommentLoader(getActivity(), accountName, thingId, linkId);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        controller.expandOrCollapse(position, id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TITLE, title);
        outState.putCharSequence(STATE_URL, url);
    }

    public void onVote(View view, int action) {
        int position = getListView().getPositionForView(view);
        controller.vote(action, position);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (adapter.getCursor() == null) {
            getListView().clearChoices();
            return false;
        }
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.comment_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        mode.setTitle(getResources().getQuantityString(R.plurals.comments, count, count));
        controller.prepareActionMenu(menu, getListView(), getFirstCheckedPosition());
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                controller.reply(getFirstCheckedPosition());
                mode.finish();
                return true;

            case R.id.menu_edit:
                controller.edit(getFirstCheckedPosition());
                mode.finish();
                return true;

            case R.id.menu_delete:
                controller.delete(getListView());
                mode.finish();
                return true;

            case R.id.menu_author:
                controller.author(getFirstCheckedPosition());
                mode.finish();
                return true;

            case R.id.menu_copy_url:
                controller.copyUrl(getFirstCheckedPosition());
                mode.finish();
                return true;

            default:
                return false;
        }
    }

    public void onDestroyActionMode(ActionMode mode) {
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
