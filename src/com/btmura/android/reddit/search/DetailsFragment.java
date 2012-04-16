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

package com.btmura.android.reddit.search;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.view.MenuItem;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.browser.BrowserActivity;
import com.btmura.android.reddit.search.SubredditInfoListFragment.OnSelectedListener;

public class DetailsFragment extends ListFragment implements LoaderCallbacks<SubredditInfo> {

    private static final String ARGS_NAME = "n";
    private static final String ARGS_POSITION = "p";
    
    private DetailsAdapter adapter;

    public static DetailsFragment newInstance(String name, int position) {
        DetailsFragment f = new DetailsFragment();
        Bundle b = new Bundle(2);
        b.putString(ARGS_NAME, name);
        b.putInt(ARGS_POSITION, position);
        f.setArguments(b);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DetailsAdapter(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }
       
    public Loader<SubredditInfo> onCreateLoader(int id, Bundle args) {
        return new DetailsLoader(getActivity(), getName());
    }
    
    public void onLoadFinished(Loader<SubredditInfo> loader, SubredditInfo data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty : R.string.error));
        setHasOptionsMenu(true);
        setListShown(true);
    }
    
    public void onLoaderReset(Loader<SubredditInfo> loader) {
        adapter.swapData(null);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                handleAddSubreddit();
                return true;

            case R.id.menu_view:
                handleViewSubreddit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleAddSubreddit() {
        List<SubredditInfo> added = new ArrayList<SubredditInfo>(1);
        added.add(adapter.getItem(0));
        getListener().onSelected(added, -1, OnSelectedListener.EVENT_ACTION_ITEM_CLICKED);
    }

    private void handleViewSubreddit() {
        Intent intent = new Intent(getActivity(), BrowserActivity.class);
        intent.putExtra(BrowserActivity.EXTRA_SUBREDDIT, getName());
        startActivity(intent);
    }

    public String getName() {
        return getArguments().getString(ARGS_NAME);
    }

    public int getPosition() {
        return getArguments().getInt(ARGS_POSITION);
    }

    private OnSelectedListener getListener() {
        return (OnSelectedListener) getActivity();
    }
}
