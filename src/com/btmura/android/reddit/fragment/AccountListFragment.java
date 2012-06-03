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

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.Provider;
import com.btmura.android.reddit.Provider.Accounts;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.widget.AccountAdapter;

public class AccountListFragment extends ListFragment implements
        LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    public static final String TAG = "AccountListFragment";

    public static AccountListFragment newInstance() {
        return new AccountListFragment();
    }

    private AccountAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new AccountAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return AccountAdapter.createLoader(getActivity().getApplicationContext());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
        setEmptyViewText(R.string.empty_accounts, data == null);
        setListShown(true);
    }
    
    private void setEmptyViewText(int emptyMessageId, boolean error) {
        setEmptyText(getString(error ? R.string.error : emptyMessageId));
        int minHeight = getResources().getDimensionPixelSize(R.dimen.min_dialog_content_height);        
        getListView().getEmptyView().setMinimumHeight(minHeight);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.account_action_menu, menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove:
                handleRemoveAction(mode);
                return true;

            default:
                return false;
        }
    }

    private void handleRemoveAction(ActionMode mode) {
        long[] ids = getListView().getCheckedItemIds();
        Provider.deleteInBackground(getActivity(), Accounts.CONTENT_URI, ids);
        mode.finish();
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
    }

    public void onDestroyActionMode(ActionMode mode) {
    }
}
