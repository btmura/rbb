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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.text.InputFilters;
import com.btmura.android.reddit.widget.AccountResultAdapter;

public class AddSubredditFragment extends DialogFragment
    implements LoaderCallbacks<AccountResult>, OnClickListener {

  public static final String TAG = "AddSubredditFragment";

  private static final String ARG_SUBREDDIT = "subreddit";
  private static final String ARG_MULTIPLE_SUBREDDITS = "multipleSubreddits";

  private AccountResultAdapter adapter;
  private boolean restoringState;
  private Spinner accountSpinner;
  private EditText nameField;
  private Button cancel;
  private Button ok;

  public static AddSubredditFragment newInstance(String subreddit) {
    Bundle args = new Bundle(1);
    args.putString(ARG_SUBREDDIT, subreddit);
    return newFragment(args);
  }

  public static AddSubredditFragment newInstance(String[] subreddits) {
    Bundle args = new Bundle(1);
    args.putStringArray(ARG_MULTIPLE_SUBREDDITS, subreddits);
    return newFragment(args);
  }

  private static AddSubredditFragment newFragment(Bundle args) {
    AddSubredditFragment frag = new AddSubredditFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adapter = AccountResultAdapter.newAccountNameListInstance(getActivity());
    restoringState = savedInstanceState != null;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    boolean multiple = isAddingMultipleSubreddits();

    getDialog().setTitle(multiple
        ? R.string.add_subreddits
        : R.string.add_subreddit);

    int layout = multiple
        ? R.layout.add_multiple_subreddits
        : R.layout.add_subreddit;
    View v = inflater.inflate(layout, container, false);
    accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
    accountSpinner.setEnabled(false);
    accountSpinner.setAdapter(adapter);

    if (!multiple) {
      String subreddit = getSubredditArgument();
      if (!Subreddits.hasSidebar(subreddit)) {
        subreddit = null;
      }
      int length = subreddit != null ? subreddit.length() : 0;
      nameField = (EditText) v.findViewById(R.id.subreddit_name);
      nameField.setText(subreddit);
      nameField.setSelection(length, length);
      nameField.setFilters(InputFilters.SUBREDDIT_NAME_FILTERS);
    }

    cancel = (Button) v.findViewById(R.id.cancel);
    cancel.setOnClickListener(this);

    ok = (Button) v.findViewById(R.id.ok);
    ok.setOnClickListener(this);
    ok.setEnabled(false);

    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
    return new AccountLoader(getActivity(), true, false);
  }

  @Override
  public void onLoadFinished(
      Loader<AccountResult> loader,
      AccountResult result) {
    int visibility = result.accountNames.length > 1 ? View.VISIBLE : View.GONE;
    accountSpinner.setVisibility(visibility);
    accountSpinner.setEnabled(true);
    ok.setEnabled(true);
    adapter.setAccountResult(result);
    if (!restoringState) {
      int index = adapter.findAccountName(result.getLastAccount(getActivity()));
      accountSpinner.setSelection(index);
    }
  }

  @Override
  public void onLoaderReset(Loader<AccountResult> loader) {
    adapter.setAccountResult(null);
  }

  @Override
  public void onClick(View v) {
    if (v == cancel) {
      handleCancel();
    } else if (v == ok) {
      handleOk();
    }
  }

  private void handleCancel() {
    dismiss();
  }

  private void handleOk() {
    if (isAddingMultipleSubreddits()) {
      addMultipleSubreddits();
    } else {
      addSubreddit();
    }
  }

  private void addMultipleSubreddits() {
    Provider.addSubredditsAsync(getActivity(),
        getSelectedAccountName(),
        getMultipleSubredditsArgument());
    dismiss();
  }

  private String getSelectedAccountName() {
    int pos = accountSpinner.getSelectedItemPosition();
    return adapter.getItem(pos).getAccountName();
  }

  private void addSubreddit() {
    String subreddit = nameField.getText().toString();
    if (TextUtils.isEmpty(subreddit)) {
      nameField.setError(getString(R.string.error_blank_field));
      return;
    }
    Provider.addSubredditsAsync(getActivity(), getSelectedAccountName(),
        subreddit);
    dismiss();
  }

  private boolean isAddingMultipleSubreddits() {
    return getMultipleSubredditsArgument() != null;
  }

  private String getSubredditArgument() {
    return getArguments().getString(ARG_SUBREDDIT);
  }

  private String[] getMultipleSubredditsArgument() {
    return getArguments().getStringArray(ARG_MULTIPLE_SUBREDDITS);
  }
}
