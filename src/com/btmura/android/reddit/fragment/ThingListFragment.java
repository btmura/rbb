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

import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Subreddits;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.activity.SidebarActivity;
import com.btmura.android.reddit.content.ThingLoader;
import com.btmura.android.reddit.data.Flag;
import com.btmura.android.reddit.data.Urls;
import com.btmura.android.reddit.entity.Subreddit;
import com.btmura.android.reddit.entity.Thing;
import com.btmura.android.reddit.widget.ThingAdapter;

public class ThingListFragment extends ListFragment implements
        LoaderCallbacks<List<Thing>>,
        OnScrollListener {

    public static final String TAG = "ThingListFragment";

    public static final int FLAG_SINGLE_CHOICE = 0x1;

    private static final String ARG_SUBREDDIT = "s";
    private static final String ARG_FILTER = "f";
    private static final String ARG_SEARCH_QUERY = "q";
    private static final String ARG_FLAGS = "l";

    private static final String STATE_THING_NAME = "n";
    private static final String STATE_THING_POSITION = "p";

    private static final String LOADER_ARG_MORE_KEY = "m";

    public interface OnThingSelectedListener {
        void onThingSelected(Thing thing, int position);

        int getThingBodyWidth();
    }

    private Subreddit subreddit;
    private String query;

    private ThingAdapter adapter;
    private boolean scrollLoading;

    public static ThingListFragment newInstance(Subreddit sr, int filter, int flags) {
        ThingListFragment f = new ThingListFragment();
        Bundle args = new Bundle(4);
        args.putParcelable(ARG_SUBREDDIT, sr);
        args.putInt(ARG_FILTER, filter);
        args.putInt(ARG_FLAGS, flags);
        f.setArguments(args);
        return f;
    }

    public static ThingListFragment newSearchInstance(String query, int flags) {
        ThingListFragment f = new ThingListFragment();
        Bundle args = new Bundle(2);
        args.putString(ARG_SEARCH_QUERY, query);
        args.putInt(ARG_FLAGS, flags);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subreddit = getSubreddit();
        query = getSearchQuery();
        adapter = new ThingAdapter(getActivity(), Subreddit.getName(subreddit), isSingleChoice());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        list.setOnScrollListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String name;
        int position;
        if (savedInstanceState != null) {
            name = savedInstanceState.getString(STATE_THING_NAME);
            position = savedInstanceState.getInt(STATE_THING_POSITION);
        } else {
            name = null;
            position = -1;
        }
        adapter.setSelectedThing(name, position);
        adapter.setThingBodyWidth(getThingBodyWidth());
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<List<Thing>> onCreateLoader(int id, Bundle args) {
        String moreKey = args != null ? args.getString(LOADER_ARG_MORE_KEY) : null;
        CharSequence url;
        if (subreddit != null) {
            url = Urls.subredditUrl(subreddit, getFilter(), moreKey);
        } else {
            url = Urls.searchUrl(query, moreKey);
        }
        return new ThingLoader(getActivity().getApplicationContext(), url,
                args != null ? adapter.getItems() : null);
    }

    public void onLoadFinished(Loader<List<Thing>> loader, List<Thing> things) {
        scrollLoading = false;
        adapter.swapData(things);
        setEmptyText(getString(things != null ? R.string.empty : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<List<Thing>> loader) {
        adapter.swapData(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Thing t = adapter.getItem(position);
        adapter.setSelectedThing(t.name, position);
        adapter.notifyDataSetChanged();

        switch (t.type) {
            case Thing.TYPE_THING:
                getListener().onThingSelected(adapter.getItem(position), position);
                break;
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
            Loader<List<Thing>> loader = getLoaderManager().getLoader(0);
            if (loader != null) {
                if (!adapter.isEmpty()) {
                    Thing t = adapter.getItem(adapter.getCount() - 1);
                    if (t.type == Thing.TYPE_MORE) {
                        scrollLoading = true;
                        Bundle b = new Bundle(1);
                        b.putString(LOADER_ARG_MORE_KEY, t.moreKey);
                        getLoaderManager().restartLoader(0, b, this);
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_THING_NAME, adapter.getSelectedThingName());
        outState.putInt(STATE_THING_POSITION, adapter.getSelectedThingPosition());
    }

    public void setSelectedThing(Thing t, int position) {
        String name = t != null ? t.name : null;
        if (!adapter.isSelectedThing(name, position)) {
            adapter.setSelectedThing(name, position);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.thing_list_menu, menu);
        menu.findItem(R.id.menu_view_subreddit_sidebar).setVisible(
                subreddit != null && !subreddit.isFrontPage());
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
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
        ContentValues values = new ContentValues(1);
        values.put(Subreddits.COLUMN_NAME, getSubreddit().name);
        Provider.addSubredditInBackground(getActivity(), values);
    }

    private void handleViewSidebar() {
        Intent intent = new Intent(getActivity(), SidebarActivity.class);
        intent.putExtra(SidebarActivity.EXTRA_SUBREDDIT, getSubreddit());
        startActivity(intent);
    }

    private int getThingBodyWidth() {
        return getListener().getThingBodyWidth();
    }

    private OnThingSelectedListener getListener() {
        return (OnThingSelectedListener) getActivity();
    }

    private Subreddit getSubreddit() {
        return getArguments().getParcelable(ARG_SUBREDDIT);
    }

    private int getFilter() {
        return getArguments().getInt(ARG_FILTER);
    }

    private String getSearchQuery() {
        return getArguments().getString(ARG_SEARCH_QUERY);
    }

    private int getFlags() {
        return getArguments().getInt(ARG_FLAGS);
    }

    private boolean isSingleChoice() {
        return Flag.isEnabled(getFlags(), FLAG_SINGLE_CHOICE);
    }
}
