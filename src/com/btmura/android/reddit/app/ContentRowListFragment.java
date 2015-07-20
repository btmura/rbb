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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.ContentAdapter;

public class ContentRowListFragment extends ListFragment
    implements LoaderCallbacks<Cursor> {

  private static final String ARG_URI = "uri";

  private ContentAdapter adapter;

  public static ContentRowListFragment newInstance(Uri uri) {
    Bundle args = new Bundle(1);
    args.putString(ARG_URI, uri.toString());

    ContentRowListFragment frag = new ContentRowListFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActivity().setTitle(getArguments().getString(ARG_URI));
    adapter = new ContentAdapter(getActivity());
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setListAdapter(adapter);
    setListShown(false);
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    Uri uri = Uri.parse(getArguments().getString(ARG_URI));
    return new CursorLoader(getActivity(), uri, null, null, null, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    adapter.swapCursor(cursor);
    setListShown(true);
    setEmptyText(getString(R.string.empty_list));
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    adapter.swapCursor(null);
    // No ListView available at this time to update.
  }
}
