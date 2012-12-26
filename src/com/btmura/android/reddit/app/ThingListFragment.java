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
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.database.SaveActions;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingAdapter;

public class ThingListFragment extends ThingProviderListFragment implements
        OnScrollListener,
        OnVoteListener,
        MultiChoiceModeListener {

    public static final String TAG = "ThingListFragment";

    /** Optional bit mask for controlling fragment appearance. */
    private static final String ARG_FLAGS = "flags";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String STATE_ADAPTER_ARGS = "adapterArgs";
    private static final String STATE_SELECTED_THING_ID = "selectedThingId";
    private static final String STATE_SELECTED_LINK_ID = "selectedLinkId";
    private static final String STATE_EMPTY_TEXT = "emptyText";

    public interface OnThingSelectedListener {
        void onThingSelected(Bundle thingBundle);

        int onMeasureThingBody();
    }

    private OnThingSelectedListener listener;
    private OnSubredditEventListener eventListener;
    private ThingAdapter adapter;
    private Bundle adapterArgs;
    private String selectedThingId;
    private String selectedLinkId;
    private int emptyText;
    private boolean scrollLoading;

    public static ThingListFragment newSubredditInstance(String accountName, String subreddit,
            int filter, int flags) {
        Bundle args = new Bundle(4);
        args.putString(ThingAdapter.ARG_ACCOUNT_NAME, accountName);
        args.putString(ThingAdapter.ARG_SUBREDDIT, subreddit);
        args.putInt(ThingAdapter.ARG_FILTER, filter);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newQueryInstance(String accountName, String query, int flags) {
        Bundle args = new Bundle(3);
        args.putString(ThingAdapter.ARG_ACCOUNT_NAME, accountName);
        args.putString(ThingAdapter.ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newMessageMessagesInstance(String accountName, String thingId,
            int flags) {
        Bundle args = new Bundle(2);
        args.putString(ThingAdapter.ARG_ACCOUNT_NAME, accountName);
        args.putString(ThingAdapter.ARG_MESSAGE_THREAD_ID, thingId);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    public static ThingListFragment newInstance(String accountName, String query,
            String profileUser, String messageUser, int filter, int flags) {
        Bundle args = new Bundle(6);
        args.putString(ThingAdapter.ARG_ACCOUNT_NAME, accountName);
        args.putString(ThingAdapter.ARG_QUERY, query);
        args.putString(ThingAdapter.ARG_PROFILE_USER, profileUser);
        args.putString(ThingAdapter.ARG_MESSAGE_USER, messageUser);
        args.putInt(ThingAdapter.ARG_FILTER, filter);
        args.putInt(ARG_FLAGS, flags);
        return newFragment(args);
    }

    private static ThingListFragment newFragment(Bundle args) {
        ThingListFragment frag = new ThingListFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnThingSelectedListener) {
            listener = (OnThingSelectedListener) activity;
        }
        if (activity instanceof OnSubredditEventListener) {
            eventListener = (OnSubredditEventListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TextUtils.isEmpty(ThingAdapter.getMessageUser(getArguments()))) {
            adapter = ThingAdapter.newMessageInstance(getActivity());
        } else if (!TextUtils.isEmpty(ThingAdapter.getMessageThreadId(getArguments()))) {
            adapter = ThingAdapter.newMessageThreadInstance(getActivity());
        } else {
            adapter = ThingAdapter.newThingInstance(getActivity());
        }

        int flags = getArguments().getInt(ARG_FLAGS);
        boolean singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);
        adapter.setSingleChoice(singleChoice);

        if (savedInstanceState == null) {
            adapterArgs = new Bundle(7);
            adapterArgs.putAll(getArguments());
        } else {
            adapterArgs = savedInstanceState.getBundle(STATE_ADAPTER_ARGS);
            selectedThingId = savedInstanceState.getString(STATE_SELECTED_THING_ID);
            selectedLinkId = savedInstanceState.getString(STATE_SELECTED_LINK_ID);
            emptyText = savedInstanceState.getInt(STATE_EMPTY_TEXT);
        }

        adapter.setAccountName(ThingAdapter.getAccountName(adapterArgs));
        adapter.setParentSubreddit(ThingAdapter.getSubreddit(adapterArgs));
        adapter.setOnVoteListener(this);
        adapter.setSelectedThing(selectedThingId, selectedLinkId);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setVerticalScrollBarEnabled(false);
        l.setOnScrollListener(this);
        l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        l.setMultiChoiceModeListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter.setThingBodyWidth(getThingBodyWidth());
        setListAdapter(adapter);
        setListShown(false);
        if (emptyText == 0) {
            loadIfPossible();
        } else {
            showEmpty();
        }
    }

    public void loadIfPossible() {
        if (adapter.isLoadable(adapterArgs)) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public void setEmpty(boolean error) {
        this.emptyText = error ? R.string.error : R.string.empty_list;
        showEmpty();
    }

    private void showEmpty() {
        setEmptyText(getString(emptyText));
        setListShown(true);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        if (args != null) {
            adapterArgs.putAll(args);
        }
        return adapter.createLoader(getActivity(), adapterArgs);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }

        // Process ThingProvider results.
        super.onLoadFinished(loader, cursor);

        scrollLoading = false;
        adapterArgs.remove(ThingAdapter.ARG_MORE);
        adapter.updateLoader(getActivity(), loader, adapterArgs);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    protected void onSessionIdLoaded(long sessionId) {
        adapterArgs.putLong(ThingAdapter.ARG_SESSION_ID, sessionId);
    }

    @Override
    protected void onSubredditLoaded(String subreddit) {
        adapterArgs.putString(ThingAdapter.ARG_SUBREDDIT, subreddit);
        adapter.setParentSubreddit(subreddit);
        if (eventListener != null) {
            eventListener.onSubredditDiscovery(subreddit);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        adapter.setSelectedPosition(position);
        selectedThingId = adapter.getSelectedThingId();
        selectedLinkId = adapter.getSelectedLinkId();
        if (listener != null) {
            listener.onThingSelected(adapter.getThingBundle(getActivity(), position));
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (visibleItemCount <= 0 || scrollLoading) {
            return;
        }
        if (firstVisibleItem + visibleItemCount * 2 >= totalItemCount) {
            if (getLoaderManager().getLoader(0) != null) {
                if (!adapter.isEmpty()) {
                    String more = adapter.getMoreThingId();
                    if (!TextUtils.isEmpty(more)) {
                        scrollLoading = true;
                        Bundle b = new Bundle(1);
                        b.putString(ThingAdapter.ARG_MORE, more);
                        getLoaderManager().restartLoader(0, b, this);
                    }
                }
            }
        }
    }

    public void onVote(String thingId, int likes) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLike id: " + thingId + " likes: " + likes);
        }
        String accountName = ThingAdapter.getAccountName(adapterArgs);
        if (!TextUtils.isEmpty(accountName)) {
            Provider.voteAsync(getActivity(), accountName, thingId, likes);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thing_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        boolean hasAccount = AccountUtils.isAccount(ThingAdapter.getAccountName(adapterArgs));
        mode.setTitle(getResources().getQuantityString(R.plurals.things, count, count));

        menu.findItem(R.id.menu_reply).setVisible(hasAccount && count == 1);
        menu.findItem(R.id.menu_compose_message).setVisible(hasAccount && count == 1);
        menu.findItem(R.id.menu_view_profile).setVisible(count == 1);
        menu.findItem(R.id.menu_copy_url).setVisible(count == 1);

        boolean showSave = false;
        boolean showUnsave = false;
        if (hasAccount && count == 1) {
            showSave = !adapter.isSaved(getFirstCheckedPosition());
            showUnsave = !showSave;
        }
        menu.findItem(R.id.menu_save).setVisible(showSave);
        menu.findItem(R.id.menu_unsave).setVisible(showUnsave);

        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reply:
                return handleReply(mode);

            case R.id.menu_compose_message:
                return handleComposeMessage(mode);

            case R.id.menu_save:
                return handleSave(mode, SaveActions.ACTION_SAVE);

            case R.id.menu_unsave:
                return handleSave(mode, SaveActions.ACTION_UNSAVE);

            case R.id.menu_view_profile:
                return handleViewProfile(mode);

            case R.id.menu_copy_url:
                return handleCopyUrl(mode);

            default:
                return false;
        }
    }

    private boolean handleReply(ActionMode mode) {
        int position = getFirstCheckedPosition();
        String user = adapter.getAuthor(position);
        Bundle extras = adapter.getReplyExtras(adapterArgs, position);
        MenuHelper.startComposeActivity(getActivity(),
                ComposeActivity.COMPOSITION_MESSAGE_REPLY, user, extras);
        mode.finish();
        return true;
    }

    private boolean handleComposeMessage(ActionMode mode) {
        String user = adapter.getAuthor(getFirstCheckedPosition());
        MenuHelper.startComposeActivity(getActivity(),
                ComposeActivity.COMPOSITION_MESSAGE, user, null);
        mode.finish();
        return true;
    }

    private boolean handleSave(ActionMode mode, int action) {
        String accountName = ThingAdapter.getAccountName(adapterArgs);
        String thingId = adapter.getThingId(getFirstCheckedPosition());
        Provider.saveAsync(getActivity(), accountName, thingId, action);
        mode.finish();
        return true;
    }

    private boolean handleViewProfile(ActionMode mode) {
        String user = adapter.getAuthor(getFirstCheckedPosition());
        MenuHelper.startProfileActivity(getActivity(), user);
        mode.finish();
        return true;
    }

    private boolean handleCopyUrl(ActionMode mode) {
        int position = getFirstCheckedPosition();
        String title = adapter.getTitle(position);
        CharSequence url = adapter.getUrl(position);
        MenuHelper.setClipAndToast(getActivity(), title, url);
        mode.finish();
        return true;
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        String subreddit = ThingAdapter.getSubreddit(adapterArgs);
        menu.findItem(R.id.menu_view_subreddit_sidebar)
                .setVisible(subreddit != null && !Subreddits.isFrontPage(subreddit));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleAdd();
                return true;

            case R.id.menu_view_subreddit_sidebar:
                handleViewSidebar();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAdd() {
    }

    private void handleViewSidebar() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, ThingAdapter.getSubreddit(adapterArgs));
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ADAPTER_ARGS, adapterArgs);
        outState.putString(STATE_SELECTED_THING_ID, selectedThingId);
        outState.putString(STATE_SELECTED_LINK_ID, selectedLinkId);
        outState.putInt(STATE_EMPTY_TEXT, emptyText);
    }

    private int getThingBodyWidth() {
        return listener != null ? listener.onMeasureThingBody() : 0;
    }

    public void setAccountName(String accountName) {
        adapterArgs.putString(ThingAdapter.ARG_ACCOUNT_NAME, accountName);
        adapter.setAccountName(accountName);
    }

    public void setSelectedThing(String thingId, String linkId) {
        selectedThingId = thingId;
        selectedLinkId = linkId;
        adapter.setSelectedThing(thingId, linkId);
    }

    public String getAccountName() {
        return adapterArgs.getString(ThingAdapter.ARG_ACCOUNT_NAME);
    }

    public void setSubreddit(String subreddit) {
        if (!Objects.equalsIgnoreCase(subreddit, ThingAdapter.getSubreddit(adapterArgs))) {
            adapterArgs.putString(ThingAdapter.ARG_SUBREDDIT, subreddit);
        }
    }

    public String getQuery() {
        return ThingAdapter.getQuery(adapterArgs);
    }

    public int getFilter() {
        return ThingAdapter.getFilter(adapterArgs);
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
