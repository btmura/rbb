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

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.activity.SidebarActivity;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.provider.VoteProvider;
import com.btmura.android.reddit.widget.OnVoteListener;
import com.btmura.android.reddit.widget.ThingAdapter;

public class ThingListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        OnScrollListener,
        OnVoteListener {

    public static final String TAG = "ThingListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_ACCOUNT_NAME = "an";
    private static final String ARG_SUBREDDIT = "s";
    private static final String ARG_FILTER = "f";
    private static final String ARG_QUERY = "q";
    private static final String ARG_FLAGS = "l";

    private static final String STATE_THING_NAME = "n";
    private static final String STATE_THING_POSITION = "p";

    private static final String LOADER_ARG_MORE = "m";

    public interface OnThingSelectedListener {
        void onThingSelected(Bundle thingBundle, int position);

        int onMeasureThingBody();
    }

    private String accountName;
    private String sessionId;
    private Subreddit subreddit;
    private int filter;
    private String query;

    private ThingAdapter adapter;
    private OnThingSelectedListener listener;
    private boolean scrollLoading;

    public static ThingListFragment newInstance(String accountName, Subreddit subreddit,
            int filter,
            String query, int flags) {
        Bundle args = new Bundle(5);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putParcelable(ARG_SUBREDDIT, subreddit);
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
        listener = (OnThingSelectedListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = savedInstanceState != null ? savedInstanceState : getArguments();
        accountName = b.getString(ARG_ACCOUNT_NAME);
        subreddit = b.getParcelable(ARG_SUBREDDIT);
        filter = b.getInt(ARG_FILTER);
        query = b.getString(ARG_QUERY);

        sessionId = Subreddit.getName(subreddit) + "-" + System.currentTimeMillis();
        adapter = new ThingAdapter(getActivity(), Subreddit.getName(subreddit), this);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView l = (ListView) v.findViewById(android.R.id.list);
        l.setVerticalScrollBarEnabled(false);
        l.setOnScrollListener(this);
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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loadIfPossible an:" + accountName + " s:" + subreddit
                    + " q:" + query + " f:" + filter);
        }
        if (accountName != null && (subreddit != null || query != null)) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateLoader args: " + args);
        }
        String accountName = getAccountName();
        String subredditName = Subreddit.getName(subreddit);
        String more = args != null ? args.getString(LOADER_ARG_MORE) : null;
        if (more != null) {
            // ThingProvider uses a cursor that deletes its data after it is
            // closed. Don't delete the data if we are appending more data to
            // it. This call shouldn't be here, but I'm not sure where else to
            // put it at this point.
            ThingProvider.cancelDeletion(adapter.getCursor());
        }
        Uri uri = ThingAdapter.createUri(accountName, sessionId, subredditName, filter, more, true);
        return ThingAdapter.createLoader(getActivity(), uri, sessionId);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor things) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onLoadFinished count: " + (things != null ? things.getCount() : -1));
        }

        Uri uri = ThingAdapter.createUri(getAccountName(), sessionId, Subreddit.getName(subreddit),
                filter, null, false);
        CursorLoader cursorLoader = (CursorLoader) loader;
        cursorLoader.setUri(uri);

        scrollLoading = false;
        adapter.swapCursor(things);
        setEmptyText(getString(things != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_ACCOUNT_NAME, accountName);
        outState.putParcelable(ARG_SUBREDDIT, subreddit);
        outState.putInt(ARG_FILTER, filter);
        outState.putString(ARG_QUERY, query);
        // outState.putString(STATE_THING_NAME, adapter.getSelectedThingName());
        // outState.putInt(STATE_THING_POSITION,
        // adapter.getSelectedThingPosition());
    }

    public void setSelectedThing(Bundle thingBundle, int position) {
        // String name = t != null ? t.name : null;
        // if (!adapter.isSelectedThing(name, position)) {
        // adapter.setSelectedThing(name, position);
        // adapter.notifyDataSetChanged();
        // }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);
        menu.findItem(R.id.menu_view_subreddit_sidebar).setVisible(
                subreddit != null && !subreddit.isFrontPage());
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
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, getSubreddit());
        startActivity(intent);
    }

    private int getThingBodyWidth() {
        return listener.onMeasureThingBody();
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Subreddit getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(Subreddit subreddit) {
        this.subreddit = subreddit;
    }

    public int getFilter() {
        return filter;
    }

    public void setFilter(int filter) {
        this.filter = filter;
    }

    public String getQuery() {
        return query;
    }

    private int getFlags() {
        return getArguments().getInt(ARG_FLAGS);
    }

    private boolean isSingleChoice() {
        return Flag.isEnabled(getFlags(), FLAG_SINGLE_CHOICE);
    }
}
