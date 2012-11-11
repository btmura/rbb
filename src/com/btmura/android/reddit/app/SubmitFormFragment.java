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
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.util.Array;
import com.btmura.android.reddit.widget.AccountNameAdapter;

public class SubmitFormFragment extends Fragment implements LoaderCallbacks<AccountResult>,
        OnCheckedChangeListener {

    public static final String TAG = "SubmitFormFragment";

    private SubredditNameHolder subredditNameHolder;
    private OnSubmitFormListener submitFormListener;
    private AccountNameAdapter adapter;

    private Spinner accountSpinner;
    private EditText subredditText;
    private EditText titleText;
    private RadioButton linkButton;
    private RadioButton textButton;
    private EditText textText;

    public interface OnSubmitFormListener {
        void onSubmitForm(Bundle submitExtras);

        void onSubmitFormCancelled();
    }

    public static SubmitFormFragment newInstance() {
        return new SubmitFormFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SubredditNameHolder) {
            subredditNameHolder = (SubredditNameHolder) activity;
        }
        if (activity instanceof OnSubmitFormListener) {
            submitFormListener = (OnSubmitFormListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new AccountNameAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.submit_form, container, false);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);

        subredditText = (EditText) v.findViewById(R.id.subreddit_text);
        if (subredditNameHolder != null) {
            subredditText.setText(subredditNameHolder.getSubredditName());
        }

        titleText = (EditText) v.findViewById(R.id.title);
        if (!TextUtils.isEmpty(subredditText.getText())) {
            titleText.requestFocus();
        }

        linkButton = (RadioButton) v.findViewById(R.id.link_radio_button);
        linkButton.setOnCheckedChangeListener(this);

        textButton = (RadioButton) v.findViewById(R.id.text_radio_button);
        textButton.setOnCheckedChangeListener(this);

        textText = (EditText) v.findViewById(R.id.text);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        return new AccountLoader(getActivity(), false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        setAccountNames(result.accountNames);
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        setAccountNames(null);
    }

    private void setAccountNames(String[] accountNames) {
        adapter.setAccountNames(accountNames);
        if (Array.isEmpty(accountNames) && submitFormListener != null) {
            submitFormListener.onSubmitFormCancelled();
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (linkButton == buttonView) {
                textText.setHint(R.string.submit_form_link);
            } else if (textButton == buttonView) {
                textText.setHint(R.string.submit_form_text);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.submit_form_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_submit:
                return handleSubmit();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean handleSubmit() {
        if (subredditText.getText().length() <= 0) {
            subredditText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (titleText.getText().length() <= 0) {
            titleText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (textText.getText().length() <= 0) {
            textText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (submitFormListener != null) {
            String accountName = adapter.getItem(accountSpinner.getSelectedItemPosition());
            String subreddit = subredditText.getText().toString();
            String title = titleText.getText().toString();
            String text = textButton.isChecked() ? textText.getText().toString() : null;
            String link = linkButton.isChecked() ? textText.getText().toString() : null;
            Bundle submitExtras = SubmitLinkFragment.newSubmitExtras(accountName, subreddit,
                    title, text, link);
            submitFormListener.onSubmitForm(submitExtras);
        }
        return true;
    }
}
