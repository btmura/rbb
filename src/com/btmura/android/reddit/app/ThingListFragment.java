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

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_SUBREDDIT = "subreddit";
    private static final String ARG_FILTER = "filter";
    private static final String ARG_QUERY = "query";
    private static final String ARG_FLAGS = "flags";

    private static final String STATE_ACCOUNT_NAME = ARG_ACCOUNT_NAME;
    private static final String STATE_SESSION_ID = "sessionId";
    private static final String STATE_SELECTED_THING_ID = "selectedThingId";
    private static final String STATE_SUBREDDIT = ARG_SUBREDDIT;

    private static final String LOADER_ARG_MORE = "more";

    public interface OnThingSelectedListener {
        void onThingSelected(Bundle thingBundle, int position);

        int onMeasureThingBody();
    }

    private String accountName;
    private String sessionId;
    private String subreddit;
    private int filter;
    private String query;
    private boolean sync;

    private String selectedThingId;
    private ThingAdapter adapter;
    private OnThingSelectedListener listener;
    private boolean scrollLoading;

    public static ThingListFragment newInstance(String accountName, String subreddit,
            int filter, String query, int flags) {
        Bundle args = new Bundle(5);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_SUBREDDIT, subreddit);
        args.putInt(ARG_FILTER, filter);
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_FLAGS, flags);

        ThingListFragment f = new ThingListFragment();
        f.setArguments(args);
        return f;
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
        filter = getArguments().getInt(ARG_FILTER);
        query = getArguments().getString(ARG_QUERY);
        sync = savedInstanceState == null;

        if (savedInstanceState == null) {
            accountName = getArguments().getString(ARG_ACCOUNT_NAME);
            subreddit = getArguments().getString(ARG_SUBREDDIT);
            sessionId = createSessionId();
        } else {
            accountName = savedInstanceState.getString(STATE_ACCOUNT_NAME);
            subreddit = savedInstanceState.getString(STATE_SUBREDDIT);
            selectedThingId = savedInstanceState.getString(STATE_SELECTED_THING_ID);
            sessionId = savedInstanceState.getString(STATE_SESSION_ID);
        }

        int flags = getArguments().getInt(ARG_FLAGS);
        boolean singleChoice = Flag.isEnabled(flags, FLAG_SINGLE_CHOICE);

        adapter = new ThingAdapter(getActivity(), accountName, subreddit, singleChoice, this);
        adapter.setSelectedThing(selectedThingId);
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
        if (accountName != null && sessionId != null && (subreddit != null || query != null)) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        String more = args != null ? args.getString(LOADER_ARG_MORE) : null;
        return ThingAdapter.getLoader(getActivity(), accountName, sessionId, subreddit, filter,
                more, query, sync);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished cursor: " + (cursor != null ? cursor.getCount() : "-1"));
        }
        sync = false;
        scrollLoading = false;
        ThingAdapter.updateLoader(getActivity(), accountName, sessionId, subreddit, filter, null,
                query, sync, loader);

        adapter.swapCursor(cursor);
        setEmptyText(getString(cursor != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        selectedThingId = adapter.setSelectedPosition(position);
        if (listener != null) {
            listener.onThingSelected(adapter.getThingBundle(position), position);
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
                        b.putString(LOADER_ARG_MORE, more);
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
        return false;
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACCOUNT_NAME, accountName);
        outState.putString(STATE_SESSION_ID, sessionId);
        outState.putString(STATE_SELECTED_THING_ID, selectedThingId);
        outState.putString(STATE_SUBREDDIT, subreddit);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);
        menu.findItem(R.id.menu_view_subreddit_sidebar).setVisible(
                subreddit != null && !Subreddits.isFrontPage(subreddit));
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
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, subreddit);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        if (!getActivity().isChangingConfigurations()) {
            ThingAdapter.deleteSessionData(getActivity(), sessionId);
        }
        super.onDestroy();
    }

    private int getThingBodyWidth() {
        return listener != null ? listener.onMeasureThingBody() : 0;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
        adapter.setAccountName(accountName);
    }

    public void setSelectedThing(String thingId) {
        selectedThingId = thingId;
        adapter.setSelectedThing(thingId);
    }

    public String getAccountName() {
        return accountName;
    }

    public void setSubreddit(String subreddit) {
        if (!Objects.equalsIgnoreCase(this.subreddit, subreddit)) {
            this.subreddit = subreddit;
            this.sessionId = createSessionId();
        }
    }

    public int getFilter() {
        return filter;
    }

    public String getQuery() {
        return query;
    }

    private String createSessionId() {
        if (!TextUtils.isEmpty(query)) {
            return query + "-" + System.currentTimeMillis();
        } else if (subreddit != null) {
            return subreddit + "-" + System.currentTimeMillis();
        }
        return null;
    }
}
