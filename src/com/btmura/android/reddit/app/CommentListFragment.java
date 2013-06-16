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
import com.btmura.android.reddit.widget.OnVoteListener;

public class CommentListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        MultiChoiceModeListener,
        OnVoteListener {

    public static final String TAG = "CommentListFragment";

    private CommentListController controller;

    public static CommentListFragment newInstance(String accountName, String thingId,
            String linkId) {
        Bundle args = new Bundle(3);
        args.putString(CommentListController.EXTRA_ACCOUNT_NAME, accountName);
        args.putString(CommentListController.EXTRA_THING_ID, thingId);
        args.putString(CommentListController.EXTRA_LINK_ID, linkId);

        CommentListFragment frag = new CommentListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new CommentListController(getActivity(), getArguments(), this);
        if (savedInstanceState != null) {
            controller.restoreInstanceState(savedInstanceState);
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
        setListAdapter(controller.getAdapter());
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return controller.createLoader();
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (controller.swapCursor(cursor)) {
            setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
            setListShown(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        controller.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        controller.expandOrCollapse(position, id);
    }

    public void onVote(View view, int action) {
        int position = getListView().getPositionForView(view);
        controller.vote(action, position);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (controller.getAdapter().getCursor() == null) {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveInstanceState(outState);
    }

    private int getFirstCheckedPosition() {
        SparseBooleanArray checked = getListView().getCheckedItemPositions();
        int size = controller.getAdapter().getCount();
        for (int i = 0; i < size; i++) {
            if (checked.get(i)) {
                return i;
            }
        }
        return -1;
    }
}
