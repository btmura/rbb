/*
 * Copyright (C) 2015 Brian Muramatsu
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

import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AddAccountLoader;
import com.btmura.android.reddit.text.InputFilters;

public class AddAccountFragment extends DialogFragment
    implements OnClickListener, LoaderManager.LoaderCallbacks<Bundle> {

  private static final String ARG_CODE = "code";

  private static final String STATE_SUBMITTED_USERNAME = "submittedUsername";

  private OnAddAccountListener listener;
  private String submittedUsername;

  private EditText usernameText;
  private ProgressBar progressBar;
  private Button addButton;
  private Button cancelButton;

  public interface OnAddAccountListener {
    void onAddAccountSuccess(Bundle result);

    void onAddAccountCancelled();
  }

  public static AddAccountFragment newInstance(String code) {
    Bundle args = new Bundle(1);
    args.putString(ARG_CODE, code);

    AddAccountFragment frag = new AddAccountFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof OnAddAccountListener) {
      listener = (OnAddAccountListener) activity;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      submittedUsername =
          savedInstanceState.getString(STATE_SUBMITTED_USERNAME);
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    getDialog().setTitle(R.string.add_account_fragment_title);

    View v = inflater.inflate(R.layout.add_account_frag, container, false);

    usernameText = (EditText) v.findViewById(R.id.username);
    usernameText.setFilters(InputFilters.NO_SPACE_FILTERS);

    progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);

    cancelButton = (Button) v.findViewById(R.id.cancel);
    cancelButton.setOnClickListener(this);

    addButton = (Button) v.findViewById(R.id.ok);
    addButton.setOnClickListener(this);

    return v;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (!TextUtils.isEmpty(submittedUsername)) {
      submit(submittedUsername);
    } else {
      reset();
    }
  }

  private void submit(String username) {
    submittedUsername = username;
    showProgressBar();
    getLoaderManager().initLoader(0, null, this);
  }

  private void reset() {
    submittedUsername = null;
    hideProgressBar();
    getLoaderManager().destroyLoader(0);
  }

  private void showProgressBar() {
    progressBar.setVisibility(View.VISIBLE);
    usernameText.setEnabled(false);
    cancelButton.setEnabled(false);
    addButton.setEnabled(false);
  }

  private void hideProgressBar() {
    progressBar.setVisibility(View.INVISIBLE);
    usernameText.setEnabled(true);
    cancelButton.setEnabled(true);
    addButton.setEnabled(true);
  }

  @Override
  public void onClick(View v) {
    if (v == addButton) {
      handleAdd();
    } else if (v == cancelButton) {
      handleCancel();
    }
  }

  private void handleAdd() {
    if (TextUtils.isEmpty(usernameText.getText())) {
      usernameText.setError(getString(R.string.error_blank_field));
      return;
    }
    if (usernameText.getError() == null) {
      submit(usernameText.getText().toString());
    }
  }

  private void handleCancel() {
    if (listener != null) {
      listener.onAddAccountCancelled();
    }
  }

  @Override
  public Loader<Bundle> onCreateLoader(int i, Bundle args) {
    return new AddAccountLoader(getActivity(), submittedUsername, getCode());
  }

  @Override
  public void onLoadFinished(Loader<Bundle> loader, Bundle result) {
    String error = result.getString(AccountManager.KEY_ERROR_MESSAGE);
    if (error != null) {
      MessageDialogFragment.showMessage(getFragmentManager(), error);
      reset();
    } else if (listener != null) {
      listener.onAddAccountSuccess(result);
    }
  }

  @Override
  public void onLoaderReset(Loader<Bundle> loader) {
    reset();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_SUBMITTED_USERNAME, submittedUsername);
  }

  private String getCode() {
    return getArguments().getString(ARG_CODE);
  }
}
