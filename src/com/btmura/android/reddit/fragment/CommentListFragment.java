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

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
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
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Comment;
import com.btmura.android.reddit.widget.CommentAdapter;

public class CommentListFragment extends ListFragment implements LoaderCallbacks<List<Comment>>,
        MultiChoiceModeListener {

    public static final String TAG = "CommentListFragment";

    private static final String ARG_THING_ID = "i";

    public interface CommentListener {
        void onReplyToComment(Comment comment);
    }

    private CommentListener listener;
    private CommentAdapter adapter;

    public static CommentListFragment newInstance(String thingId) {
        CommentListFragment frag = new CommentListFragment();
        Bundle b = new Bundle(1);
        b.putString(ARG_THING_ID, thingId);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CommentListener) {
            listener = (CommentListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new CommentAdapter(getActivity());
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

    public Loader<List<Comment>> onCreateLoader(int id, Bundle args) {
        URL url = Urls.commentsUrl(getArguments().getString(ARG_THING_ID));
        return new CommentLoader(getActivity().getApplicationContext(), url);
    }

    public void onLoadFinished(Loader<List<Comment>> loader, List<Comment> comments) {
        adapter.swapData(comments);
        setEmptyText(getString(comments != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<List<Comment>> loader) {
        adapter.swapData(null);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.comment_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int checkedCount = getListView().getCheckedItemCount();
        boolean showReply = checkedCount == 1;
        menu.findItem(R.id.menu_reply).setVisible(showReply);
        return true;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                handleReply();
                return true;

            default:
                return false;
        }
    }

    private void handleReply() {
        if (listener != null) {
            listener.onReplyToComment(findFirstCheckedComment());
        }
    }

    private Comment findFirstCheckedComment() {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        int count = positions.size();
        for (int i = 0; i < count; i++) {
            if (positions.get(i)) {
                return adapter.getItem(i);
            }
        }
        return null;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public void onDestroyActionMode(ActionMode mode) {
    }
}
