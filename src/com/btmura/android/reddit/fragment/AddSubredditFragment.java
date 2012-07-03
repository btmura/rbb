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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.text.InputFilters;
import com.btmura.android.reddit.widget.AccountSpinnerAdapter;

public class AddSubredditFragment extends DialogFragment implements
        LoaderCallbacks<AccountResult>,
        OnCheckedChangeListener,
        OnClickListener {

    public static final String TAG = "AddSubredditFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
            InputFilters.SUBREDDIT_NAME_FILTER,
    };

    private SubredditNameHolder subredditNameHolder;
    private AccountSpinnerAdapter adapter;
    private boolean restoringState;

    private Spinner accountSpinner;
    private EditText nameField;
    private CheckBox addFrontPage;
    private Button cancel;
    private Button add;

    private View accountText;

    private View subredditText;

    public static AddSubredditFragment newInstance() {
        return new AddSubredditFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        subredditNameHolder = (SubredditNameHolder) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AccountSpinnerAdapter(getActivity(), false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.add_subreddit);
        View v = inflater.inflate(R.layout.add_subreddit, container, false);

        accountText = v.findViewById(R.id.account_text);
        subredditText = v.findViewById(R.id.subreddit_text);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);

        CharSequence name = subredditNameHolder.getSubredditName();
        int length = name != null ? name.length() : 0;
        nameField = (EditText) v.findViewById(R.id.subreddit_name);
        nameField.setText(name);
        nameField.setSelection(length, length);
        nameField.setFilters(INPUT_FILTERS);

        addFrontPage = (CheckBox) v.findViewById(R.id.add_front_page);
        addFrontPage.setOnCheckedChangeListener(this);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        add = (Button) v.findViewById(R.id.add);
        add.setOnClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        restoringState = savedInstanceState != null;
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity());
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        int visiblility = result.accountNames.length > 1 ? View.VISIBLE : View.GONE;
        accountText.setVisibility(visiblility);
        subredditText.setVisibility(visiblility);
        accountSpinner.setVisibility(visiblility);

        adapter.setAccountNames(result.accountNames);
        if (!restoringState) {
            int index = AccountLoader.getLastAccountIndex(result.prefs, result.accountNames);
            accountSpinner.setSelection(index);
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        nameField.setEnabled(!isChecked);
        if (!nameField.isEnabled()) {
            nameField.setError(null);
        }
    }

    public void onClick(View v) {
        if (v == cancel) {
            handleCancel();
        } else if (v == add) {
            handleAdd();
        }
    }

    private void handleCancel() {
        dismiss();
    }

    private void handleAdd() {
        String subredditName;
        if (addFrontPage.isChecked()) {
            subredditName = "";
        } else {
            subredditName = nameField.getText().toString();
        }

        if (!addFrontPage.isChecked() && TextUtils.isEmpty(subredditName)) {
            nameField.setError(getString(R.string.error_blank_field));
            return;
        }

        int position = accountSpinner.getSelectedItemPosition();
        String accountName = adapter.getAccountName(position);
        SubredditProvider.addInBackground(getActivity(), accountName, subredditName);
        dismiss();
    }
}
