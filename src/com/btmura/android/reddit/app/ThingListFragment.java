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
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.VoteProvider;
import com.btmura.android.reddit.util.Flag;
import com.btmura.android.reddit.util.Objects;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingAdapter;

public class ThingListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
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

    public interface OnThingSelectedListener {
        void onThingSelected(Bundle thingBundle, int position);

        int onMeasureThingBody();
    }

    private String selectedThingId;
    private String selectedLinkId;
    private ThingAdapter adapter;
    Bundle adapterArgs;

    private boolean sync;
    private boolean scrollLoading;

    private OnThingSelectedListener listener;

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sync = savedInstanceState == null;

        if (!TextUtils.isEmpty(ThingAdapter.getMessageUser(getArguments()))) {
            adapter = ThingAdapter.newMessagesInstance(getActivity());
        } else {
            adapter = ThingAdapter.newThingInstance(getActivity());
        }

        int flags = getArguments().getInt(ARG_FLAGS);
        boolean singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);
        adapter.setSingleChoice(singleChoice);

        if (savedInstanceState == null) {
            adapterArgs = new Bundle(7);
            adapterArgs.putAll(getArguments());
            adapterArgs.putString(ThingAdapter.ARG_SESSION_ID,
                    adapter.createSessionId(adapterArgs));
        } else {
            adapterArgs = savedInstanceState.getBundle(STATE_ADAPTER_ARGS);
            selectedThingId = savedInstanceState.getString(STATE_SELECTED_THING_ID);
            selectedLinkId = savedInstanceState.getString(STATE_SELECTED_LINK_ID);
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
        loadIfPossible();
    }

    public void loadIfPossible() {
        if (adapter.isLoadable(adapterArgs)) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        if (args != null) {
            adapterArgs.putAll(args);
        }
        adapterArgs.putBoolean(ThingAdapter.ARG_FETCH, sync);
        return adapter.createLoader(getActivity(), adapterArgs);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }
        sync = false;
        scrollLoading = false;
        adapterArgs.putBoolean(ThingAdapter.ARG_FETCH, sync);
        adapterArgs.remove(ThingAdapter.ARG_MORE);
        adapter.updateLoader(getActivity(), loader, adapterArgs);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
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
            listener.onThingSelected(adapter.getThingBundle(getActivity(), position), position);
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
                        sync = true;
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
            VoteProvider.voteInBackground(getActivity(), accountName, thingId, likes);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.thing_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        int count = getListView().getCheckedItemCount();
        mode.setTitle(getResources().getQuantityString(R.plurals.things, count, count));
        return true;
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_profile:
                return handleViewProfile(mode);

            default:
                return false;
        }
    }

    private boolean handleViewProfile(ActionMode mode) {
        String user = adapter.getAuthor(getFirstCheckedPosition());
        MenuHelper.startProfileActivity(getActivity(), user);
        mode.finish();
        return true;
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(STATE_ADAPTER_ARGS, adapterArgs);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);

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
    public void onDestroy() {
        if (!getActivity().isChangingConfigurations()) {
            adapter.deleteSessionData(getActivity(), adapterArgs);
        }
        super.onDestroy();
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
            adapterArgs.putString(ThingAdapter.ARG_SESSION_ID,
                    adapter.createSessionId(adapterArgs));
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
