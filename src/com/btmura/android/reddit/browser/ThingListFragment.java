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

package com.btmura.android.reddit.browser;

import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;

public class ThingListFragment extends ListFragment implements LoaderCallbacks<List<Thing>>,
        OnScrollListener {

    private static final String ARG_SUBREDDIT = "s";
    private static final String ARG_FILTER = "f";
    private static final String ARG_SINGLE_CHOICE = "c";

    private static final String STATE_CHOSEN_NAME = "s";

    private static final String LOADER_ARG_MORE_KEY = "m";

    interface OnThingSelectedListener {
        void onThingSelected(Thing thing, int position);
        int getThingBodyWidth();
    }

    private ThingAdapter adapter;
    private boolean scrollLoading;

    public static ThingListFragment newInstance(Subreddit sr, int filter, boolean singleChoice) {
        ThingListFragment frag = new ThingListFragment();
        Bundle b = new Bundle(4);
        b.putParcelable(ARG_SUBREDDIT, sr);
        b.putInt(ARG_FILTER, filter);
        b.putBoolean(ARG_SINGLE_CHOICE, singleChoice);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new ThingAdapter(getActivity(), getActivity().getLayoutInflater(),
                getSubreddit(), isSingleChoice());
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
        String name = null;
        if (savedInstanceState != null) {
            name = savedInstanceState.getString(STATE_CHOSEN_NAME);
        }
        adapter.setChosenName(name);
        adapter.setThingBodyWidth(getThingBodyWidth());
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<List<Thing>> onCreateLoader(int id, Bundle args) {
        String moreKey = args != null ? args.getString(LOADER_ARG_MORE_KEY) : null;
        CharSequence url = Urls.subredditUrl(getSubreddit(), getFilter(), moreKey);
        return new ThingLoader(getActivity(), url, args != null ? adapter.getItems() : null);
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
        adapter.setChosenName(t.name);
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
        outState.putString(STATE_CHOSEN_NAME, adapter.getChosenName());
    }

    public void setChosenThing(Thing t) {
        String name = t != null ? t.name : null;
        if (!adapter.isChosenName(name)) {
            adapter.setChosenName(name);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                setListShown(false);
                getLoaderManager().restartLoader(0, null, this);
                return true;
        }
        return false;
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

    private boolean isSingleChoice() {
        return getArguments().getBoolean(ARG_SINGLE_CHOICE);
    }

    private int getThingBodyWidth() {
        return getListener().getThingBodyWidth();
    }
}
