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
import android.app.AlertDialog;
import android.content.DialogInterface;
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

  private static final String ARG_FIXED_ACCOUNT_NAME = "fan";
  private static final String ARG_CODE = "c";

  private static final String STATE_SUBMITTED_ACCOUNT_NAME = "san";
  private static final String STATE_SUBMIT_ERROR = "se";

  private OnAddAccountListener listener;
  private String submittedAccountName;
  private String submitError;

  private EditText accountNameText;
  private ProgressBar progressBar;
  private Button addButton;
  private Button cancelButton;
  private AlertDialog errorDialog;

  public interface OnAddAccountListener {
    void onAddAccountSuccess(Bundle result);

    void onAddAccountCancelled();
  }

  public static AddAccountFragment newInstance(
      String fixedAccountName,
      String code) {
    Bundle args = new Bundle(2);
    args.putString(ARG_FIXED_ACCOUNT_NAME, fixedAccountName);
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
      submittedAccountName =
          savedInstanceState.getString(STATE_SUBMITTED_ACCOUNT_NAME);
      submitError = savedInstanceState.getString(STATE_SUBMIT_ERROR);
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    getDialog().setTitle(R.string.title_add_account);

    View v = inflater.inflate(R.layout.add_account_frag, container, false);

    accountNameText = (EditText) v.findViewById(R.id.account_name_text);
    accountNameText.setFilters(InputFilters.NO_SPACE_FILTERS);

    // Don't allow editing if re-adding an existing account.
    if (hasFixedAccountName()) {
      accountNameText.setText(getFixedAccountName());
      accountNameText.setEnabled(false);
    }

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
    refresh();
    if (!TextUtils.isEmpty(submittedAccountName)) {
      getLoaderManager().initLoader(0, null, this);
    }
  }

  private void refresh() {
    if (!TextUtils.isEmpty(submittedAccountName)) {
      showProgressBar();
    } else {
      hideProgressBar();
    }
    if (!TextUtils.isEmpty(submitError)) {
      showErrorDialog();
    } else {
      hideErrorDialog();
    }
  }

  private void showProgressBar() {
    progressBar.setVisibility(View.VISIBLE);
    accountNameText.setEnabled(false);
    cancelButton.setEnabled(false);
    addButton.setEnabled(false);
  }

  private void hideProgressBar() {
    progressBar.setVisibility(View.INVISIBLE);
    accountNameText.setEnabled(!hasFixedAccountName());
    cancelButton.setEnabled(true);
    addButton.setEnabled(true);
  }

  private void showErrorDialog() {
    if (errorDialog == null) {
      errorDialog = new AlertDialog.Builder(getActivity())
          .setPositiveButton(android.R.string.ok,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  reset();
                }
              })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              reset();
            }
          })
          .create();
    }
    errorDialog.setMessage(submitError);
    errorDialog.show();
  }

  private void hideErrorDialog() {
    if (errorDialog != null) {
      errorDialog.hide();
    }
  }

  private void submit(String username) {
    submittedAccountName = username;
    submitError = null;
    refresh();
    getLoaderManager().initLoader(0, null, this);
  }

  private void reset() {
    submittedAccountName = null;
    submitError = null;
    refresh();
    getLoaderManager().destroyLoader(0);
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
    if (TextUtils.isEmpty(accountNameText.getText())) {
      accountNameText.setError(getString(R.string.error_blank_field));
      return;
    }
    if (accountNameText.getError() == null) {
      submit(accountNameText.getText().toString());
    }
  }

  private void handleCancel() {
    if (listener != null) {
      listener.onAddAccountCancelled();
    }
    dismiss();
  }

  @Override
  public Loader<Bundle> onCreateLoader(int i, Bundle args) {
    return new AddAccountLoader(getActivity(), submittedAccountName, getCode());
  }

  @Override
  public void onLoadFinished(Loader<Bundle> loader, Bundle result) {
    submitError = result.getString(AccountManager.KEY_ERROR_MESSAGE);
    refresh();
    if (TextUtils.isEmpty(submitError) && listener != null) {
      listener.onAddAccountSuccess(result);
    }
  }

  @Override
  public void onLoaderReset(Loader<Bundle> loader) {
    refresh();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(STATE_SUBMITTED_ACCOUNT_NAME, submittedAccountName);
    outState.putString(STATE_SUBMIT_ERROR, submitError);
  }

  private boolean hasFixedAccountName() {
    return !TextUtils.isEmpty(getFixedAccountName());
  }

  private String getFixedAccountName() {
    return getArguments().getString(ARG_FIXED_ACCOUNT_NAME);
  }

  private String getCode() {
    return getArguments().getString(ARG_CODE);
  }
}
