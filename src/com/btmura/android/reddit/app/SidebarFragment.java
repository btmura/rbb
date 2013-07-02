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

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.SidebarLoader;
import com.btmura.android.reddit.net.SidebarResult;
import com.btmura.android.reddit.widget.SidebarAdapter;

public class SidebarFragment extends ListFragment implements LoaderCallbacks<SidebarResult> {

    private static final String ARGS_SUBREDDIT = "subreddit";

    private SidebarAdapter adapter;

    public static SidebarFragment newInstance(String subreddit) {
        Bundle b = new Bundle(1);
        b.putString(ARGS_SUBREDDIT, subreddit);

        SidebarFragment frag = new SidebarFragment();
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SidebarAdapter(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<SidebarResult> onCreateLoader(int id, Bundle args) {
        return new SidebarLoader(getActivity().getApplicationContext(), getSubredditArgument());
    }

    public void onLoadFinished(Loader<SidebarResult> loader, SidebarResult data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<SidebarResult> loader) {
        adapter.swapData(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MenuHelper.startSubredditActivity(getActivity(), getSubredditArgument());
    }

    private String getSubredditArgument() {
        return getArguments().getString(ARGS_SUBREDDIT);
    }
}
