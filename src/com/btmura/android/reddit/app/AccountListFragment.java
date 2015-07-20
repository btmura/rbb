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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.widget.AccountResultAdapter;

import java.io.IOException;

public class AccountListFragment extends ListFragment
    implements LoaderCallbacks<AccountResult>,
    MultiChoiceModeListener {

  private static final String TAG = "AccountListFragment";

  public interface OnAccountSelectedListener {
    void onAccountSelected(String accountName);
  }

  private OnAccountSelectedListener listener;
  private AccountResultAdapter adapter;

  public static AccountListFragment newInstance() {
    return new AccountListFragment();
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnAccountSelectedListener) {
      listener = (OnAccountSelectedListener) activity;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adapter = AccountResultAdapter.newAccountListInstance(getActivity());
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = super.onCreateView(inflater, container, savedInstanceState);
    ListView l = (ListView) v.findViewById(android.R.id.list);
    l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    l.setMultiChoiceModeListener(this);
    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setListAdapter(adapter);
    setListShown(false);
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
    return new AccountLoader(getActivity(), false, true);
  }

  @Override
  public void onLoadFinished(
      Loader<AccountResult> loader,
      AccountResult result) {
    adapter.setAccountResult(result);
    setEmptyText(getString(result != null
        ? R.string.empty_accounts
        : R.string.error));
    setListShown(true);
  }

  @Override
  public void onLoaderReset(Loader<AccountResult> loader) {
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    if (listener != null) {
      listener.onAccountSelected(adapter.getItem(position).getAccountName());
    }
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    mode.getMenuInflater().inflate(R.menu.account_action_menu, menu);
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    int count = getListView().getCheckedItemCount();
    mode.setTitle(getResources()
        .getQuantityString(R.plurals.accounts, count, count));
    return true;
  }

  @Override
  public void onItemCheckedStateChanged(
      ActionMode mode,
      int position,
      long id,
      boolean checked) {
    mode.invalidate();
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_delete:
        handleDelete(mode);
        return true;

      default:
        return false;
    }
  }

  private void handleDelete(ActionMode mode) {
    SparseBooleanArray checked = getListView().getCheckedItemPositions();
    int checkedCount = getListView().getCheckedItemCount();
    String[] accountNames = new String[checkedCount];
    int count = adapter.getCount();
    int j = 0;
    for (int i = 0; i < count; i++) {
      if (checked.get(i)) {
        accountNames[j++] = adapter.getItem(i).getAccountName();
      }
    }
    removeAccounts(accountNames);
    mode.finish();
  }

  private void removeAccounts(final String[] accountNames) {
    final Context ctx = getActivity().getApplicationContext();
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voidRay) {
        for (int i = 0; i < accountNames.length; i++) {
          AccountProvider.removeAccount(ctx, accountNames[i]);
        }
        return null;
      }
    }.execute();
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
  }
}
