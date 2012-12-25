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
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
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
import com.btmura.android.reddit.database.CommentLogic;
import com.btmura.android.reddit.database.CommentLogic.CommentList;
import com.btmura.android.reddit.database.Kinds;
import com.btmura.android.reddit.database.Things;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.widget.CommentAdapter;
import com.btmura.android.reddit.widget.OnVoteListener;

public class CommentListFragment extends ListFragment implements LoaderCallbacks<Cursor>,
        MultiChoiceModeListener, OnVoteListener, CommentList {

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

    private static final String STATE_SESSION_ID = "sessionId";
    private static final String STATE_TITLE = ARG_TITLE;
    private static final String STATE_URL = ARG_URL;

    private OnThingEventListener listener;
    private String accountName;
    private String thingId;
    private String linkId;
    private String title;
    private CharSequence url;
    private int flags;

    private boolean sync;
    private String sessionId;
    private CommentAdapter adapter;

    public static CommentListFragment newInstance(String accountName, String thingId,
            String linkId, String title, CharSequence url, int flags) {
        Bundle args = new Bundle(4);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingEventListener) {
            listener = (OnThingEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        accountName = getArguments().getString(ARG_ACCOUNT_NAME);
        thingId = getArguments().getString(ARG_THING_ID);
        linkId = getArguments().getString(ARG_LINK_ID);
        flags = getArguments().getInt(ARG_FLAGS);
        sync = savedInstanceState == null;

        // Don't create a new session if changing configuration.
        if (savedInstanceState != null) {
            sessionId = savedInstanceState.getString(STATE_SESSION_ID);
            title = savedInstanceState.getString(STATE_TITLE);
            url = savedInstanceState.getCharSequence(STATE_URL);
        } else {
            sessionId = thingId + "-" + System.currentTimeMillis();
            title = getArguments().getString(ARG_TITLE);
            url = getArguments().getCharSequence(ARG_URL);
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
        return CommentAdapter.getLoader(getActivity(), accountName, sessionId,
                thingId, linkId, sync);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }
        sync = false;
        CommentAdapter.updateLoader(getActivity(), loader, accountName, sessionId,
                thingId, linkId, sync);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);

        // Update the title and url if they weren't provided already.
        if (adapter.getCount() > 0) {
            if (TextUtils.isEmpty(title)) {
                title = adapter.getString(0, CommentAdapter.INDEX_TITLE);
            }
            if (TextUtils.isEmpty(url)) {
                String permaLink = adapter.getString(0, CommentAdapter.INDEX_PERMA_LINK);
                if (!TextUtils.isEmpty(permaLink)) {
                    url = Urls.perma(permaLink, null);
                }
            }
            getActivity().invalidateOptionsMenu();
        }

        // Broadcast the link if there is one in case the parent doesn't know.
        if (listener != null) {
            listener.onTitleDiscovery(thingId, adapter.getString(0, CommentAdapter.INDEX_TITLE));
            if (!adapter.getBoolean(0, CommentAdapter.INDEX_SELF)) {
                listener.onLinkDiscovery(thingId, adapter.getString(0, CommentAdapter.INDEX_URL));
            }
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Only comments can be expanded and collapsed.
        if (adapter.getInt(position, CommentAdapter.INDEX_KIND) != Kinds.KIND_COMMENT) {
            return;
        }

        // Collapse if expanded. Expand if collapsed.
        if (adapter.getBoolean(position, CommentAdapter.INDEX_EXPANDED)) {
            long[] childIds = CommentLogic.getChildren(this, position);
            Provider.collapseCommentAsync(getActivity(), id, childIds);
        } else {
            Provider.expandCommentAsync(getActivity(), sessionId, id);
        }
    }

    public void onVote(String thingId, int likes) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLike thingId: " + thingId + " likes: " + likes);
        }
        if (AccountUtils.isAccount(accountName)) {
            Provider.voteAsync(getActivity(), accountName, thingId, likes);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateOptionsMenu");
        }
        inflater.inflate(R.menu.comment_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPrepareOptionsMenu");
        }

        boolean linkReady = !TextUtils.isEmpty(title) && !TextUtils.isEmpty(url);
        menu.findItem(R.id.menu_comment_open).setVisible(linkReady);
        menu.findItem(R.id.menu_comment_copy_url).setVisible(linkReady);

        MenuItem shareItem = menu.findItem(R.id.menu_comment_share);
        shareItem.setVisible(linkReady);
        if (linkReady) {
            MenuHelper.setShareProvider(shareItem, title, url);
        }

        boolean linkConfirmed = Flag.isEnabled(flags, FLAG_SHOW_LINK_MENU_ITEM);
        boolean showLink = linkConfirmed || (adapter.getCursor() != null
                && !adapter.getBoolean(0, CommentAdapter.INDEX_SELF));
        menu.findItem(R.id.menu_link).setVisible(showLink);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_link:
                return handleLink();

            case R.id.menu_comment_open:
                return handleOpen();

            case R.id.menu_comment_copy_url:
                return handleCopyUrl();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleLink() {
        if (listener != null) {
            listener.onLinkMenuItemClick();
        }
        return true;
    }

    private boolean handleOpen() {
        MenuHelper.startIntentChooser(getActivity(), url);
        return true;
    }

    private boolean handleCopyUrl() {
        MenuHelper.setClipAndToast(getActivity(), title, url);
        return true;
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

        menu.findItem(R.id.menu_reply).setVisible(isReplyItemVisible());
        menu.findItem(R.id.menu_delete).setVisible(isDeleteItemVisible());
        menu.findItem(R.id.menu_compose_message).setVisible(isComposeMessageItemVisible());
        menu.findItem(R.id.menu_copy_url).setVisible(isCopyUrlItemVisible());
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
        if (Things.DELETED.equals(author)) {
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

    private boolean isComposeMessageItemVisible() {
        // You need an account to compose a message.
        if (!AccountUtils.isAccount(accountName)) {
            return false;
        }
        return getListView().getCheckedItemCount() == 1;
    }

    private boolean isCopyUrlItemVisible() {
        return getListView().getCheckedItemCount() == 1;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                return handleReply(mode);

            case R.id.menu_delete:
                return handleDelete(mode);

            case R.id.menu_view_profile:
                return handleViewProfile(mode);

            case R.id.menu_compose_message:
                return handleComposeMessage(mode);

            case R.id.menu_copy_url:
                return handleCopyUrl(mode);

            default:
                return false;
        }
    }

    private boolean handleReply(ActionMode mode) {
        int position = getFirstCheckedPosition();
        if (position != -1) {
            long parentId = adapter.getLong(0, CommentAdapter.INDEX_ID);
            int parentNumComments = adapter.getInt(0, CommentAdapter.INDEX_NUM_COMMENTS);
            String replyAuthor = adapter.getString(position, CommentAdapter.INDEX_AUTHOR);
            String replyThingId = adapter.getString(position, CommentAdapter.INDEX_THING_ID);
            int nesting = CommentLogic.getInsertNesting(this, position);
            int sequence = CommentLogic.getInsertSequence(this, position);

            Bundle args = CommentReplyActivity.newArgs(parentId, parentNumComments, thingId,
                    replyAuthor, replyThingId, nesting, sequence, sessionId);

            Intent intent = new Intent(getActivity(), CommentReplyActivity.class);
            intent.putExtra(CommentReplyActivity.EXTRA_ARGS, args);
            startActivity(intent);
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

    private boolean handleDelete(ActionMode mode) {
        long headerId = adapter.getLong(0, CommentAdapter.INDEX_ID);
        int headerNumComments = adapter.getInt(0, CommentAdapter.INDEX_NUM_COMMENTS);

        long[] ids = getListView().getCheckedItemIds();
        String[] thingIds = new String[ids.length];
        boolean[] hasChildren = new boolean[ids.length];
        fillCheckedInfo(thingIds, hasChildren);
        Provider.deleteCommentAsync(getActivity(), accountName, headerId, headerNumComments,
                thingId, ids, thingIds, hasChildren);
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

    private boolean handleViewProfile(ActionMode mode) {
        String user = adapter.getString(getFirstCheckedPosition(), CommentAdapter.INDEX_AUTHOR);
        MenuHelper.startProfileActivity(getActivity(), user);
        mode.finish();
        return true;
    }

    private boolean handleComposeMessage(ActionMode mode) {
        String user = adapter.getString(getFirstCheckedPosition(), CommentAdapter.INDEX_AUTHOR);
        MenuHelper.startComposeActivity(getActivity(),
                ComposeActivity.COMPOSITION_MESSAGE, user, null);
        mode.finish();
        return true;
    }

    private boolean handleCopyUrl(ActionMode mode) {
        int position = getFirstCheckedPosition();

        // Determine the label of the text to copy to the clipboard.
        CharSequence label;
        if (position != 0) {
            label = adapter.getString(position, CommentAdapter.INDEX_BODY);
        } else {
            label = adapter.getString(0, CommentAdapter.INDEX_TITLE);
        }

        // Determine the text to copy to the clipboard.
        String thingId;
        if (position != 0) {
            thingId = adapter.getString(position, CommentAdapter.INDEX_THING_ID);
        } else {
            thingId = null; // Don't append anything to the permalink.
        }
        String permaLink = adapter.getString(0, CommentAdapter.INDEX_PERMA_LINK);
        CharSequence text = Urls.perma(permaLink, thingId);

        // Copy to the clipboard and present a toast.
        MenuHelper.setClipAndToast(getActivity(), label, text);
        return true;
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SESSION_ID, sessionId);
        outState.putString(STATE_TITLE, title);
        outState.putCharSequence(STATE_URL, url);
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
