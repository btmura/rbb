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

import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
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
import com.btmura.android.reddit.widget.AccountNameAdapter;

public class AddSubredditFragment extends DialogFragment implements LoaderCallbacks<AccountResult>,
        OnClickListener {

    public static final String TAG = "AddSubredditFragment";

    private SubredditNameHolder subredditNameHolder;
    private AccountNameAdapter adapter;
    private boolean restoringState;
    private Spinner accountSpinner;
    private EditText nameField;
    private Button cancel;
    private Button ok;

    public static AddSubredditFragment newInstance() {
        return new AddSubredditFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SubredditNameHolder) {
            subredditNameHolder = (SubredditNameHolder) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AccountNameAdapter(getActivity(), R.layout.account_name_row);
        adapter.add(getString(R.string.loading));
        restoringState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getDialog().setTitle(R.string.add_subreddit);
        View v = inflater.inflate(R.layout.add_subreddit, container, false);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setEnabled(false);
        accountSpinner.setAdapter(adapter);

        String name = subredditNameHolder.getSubredditName();
        if (!Subreddits.hasSidebar(name)) {
            name = null;
        }
        int length = name != null ? name.length() : 0;
        nameField = (EditText) v.findViewById(R.id.subreddit_name);
        nameField.setText(name);
        nameField.setSelection(length, length);
        nameField.setFilters(InputFilters.SUBREDDIT_NAME_FILTERS);

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

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), true);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        int visiblility = result.accountNames.length > 1 ? View.VISIBLE : View.GONE;
        accountSpinner.setVisibility(visiblility);
        accountSpinner.setEnabled(true);
        ok.setEnabled(true);
        adapter.clear();
        adapter.addAll(result.accountNames);
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.clear();
    }

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
        String subreddit = nameField.getText().toString();
        if (TextUtils.isEmpty(subreddit)) {
            nameField.setError(getString(R.string.error_blank_field));
            return;
        }

        String accountName = adapter.getItem(accountSpinner.getSelectedItemPosition());
        Provider.addSubredditAsync(getActivity(), accountName, subreddit);
        dismiss();
    }
}
