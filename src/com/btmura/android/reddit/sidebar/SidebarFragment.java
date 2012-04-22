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

package com.btmura.android.reddit.sidebar;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.Details;

public class SidebarFragment extends ListFragment implements LoaderCallbacks<Details> {

    private static final String ARGS_NAME = "n";
    private static final String ARGS_POSITION = "p";

    private DetailsAdapter adapter;

    public static SidebarFragment newInstance(String name, int position) {
        SidebarFragment f = new SidebarFragment();
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

    public Loader<Details> onCreateLoader(int id, Bundle args) {
        return new DetailsLoader(getActivity(), getName());
    }

    public void onLoadFinished(Loader<Details> loader, Details data) {
        adapter.swapData(data);
        setEmptyText(getString(data != null ? R.string.empty : R.string.error));
        setListShown(true);
    }

    public void onLoaderReset(Loader<Details> loader) {
        adapter.swapData(null);
    }

    public String getName() {
        return getArguments().getString(ARGS_NAME);
    }

    public int getPosition() {
        return getArguments().getInt(ARGS_POSITION);
    }
}
