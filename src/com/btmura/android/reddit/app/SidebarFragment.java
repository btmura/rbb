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

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.SidebarLoader;
import com.btmura.android.reddit.net.SidebarResult;
import com.btmura.android.reddit.widget.SidebarAdapter;
import com.btmura.android.reddit.widget.SidebarAdapter.OnSidebarButtonClickListener;

public class SidebarFragment extends ListFragment implements LoaderCallbacks<SidebarResult>,
        OnSidebarButtonClickListener {

    private static final String ARGS_SUBREDDIT = "subreddit";
    private static final String ARG_SHOW_HEADER_BUTTONS = "showHeaderButtons";

    private SidebarAdapter adapter;

    public static SidebarFragment newInstance(String subreddit, boolean showHeaderButtons) {
        Bundle b = new Bundle(2);
        b.putString(ARGS_SUBREDDIT, subreddit);
        b.putBoolean(ARG_SHOW_HEADER_BUTTONS, showHeaderButtons);

        SidebarFragment frag = new SidebarFragment();
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new SidebarAdapter(getActivity(), showHeaderButtons());
        adapter.setOnSidebarButtonClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<SidebarResult> onCreateLoader(int id, Bundle args) {
        return new SidebarLoader(getActivity().getApplicationContext(), getSubreddit());
    }

    public void onLoadFinished(Loader<SidebarResult> loader, SidebarResult data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty_list : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<SidebarResult> loader) {
        adapter.swapData(null);
    }

    public void onAddClicked() {
        MenuHelper.showAddSubredditDialog(getFragmentManager(), getSubreddit());
    }

    public void onViewClicked() {
        MenuHelper.startSubredditActivity(getActivity(), getSubreddit());
    }

    private String getSubreddit() {
        return getArguments().getString(ARGS_SUBREDDIT);
    }

    private boolean showHeaderButtons() {
        return getArguments().getBoolean(ARG_SHOW_HEADER_BUTTONS);
    }
}
