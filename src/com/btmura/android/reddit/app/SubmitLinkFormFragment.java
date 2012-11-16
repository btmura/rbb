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

import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.Spinner;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.widget.AccountNameAdapter;

public class SubmitLinkFormFragment extends Fragment implements LoaderCallbacks<AccountResult>,
        OnClickListener {

    public static final String TAG = "SubmitLinkFormFragment";

    private static final String ARG_SUBREDDIT = "subreddit";

    private static final Pattern LINK_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9+-.]*?://[^ ]+");

    private OnSubmitFormListener listener;
    private AccountNameAdapter adapter;
    private boolean restoringState;
    private Spinner accountSpinner;
    private EditText subredditText;
    private EditText titleText;
    private EditText textText;
    private View ok;
    private View cancel;

    public interface OnSubmitFormListener {
        void onSubmitForm(Bundle submitExtras);

        void onSubmitFormCancelled();
    }

    public static SubmitLinkFormFragment newInstance(String subreddit) {
        Bundle args = new Bundle(1);
        args.putString(ARG_SUBREDDIT, subreddit);
        SubmitLinkFormFragment frag = new SubmitLinkFormFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSubmitFormListener) {
            listener = (OnSubmitFormListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        adapter = new AccountNameAdapter(getActivity());
        restoringState = savedInstanceState != null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.submit_link_form, container, false);
        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(adapter);

        subredditText = (EditText) v.findViewById(R.id.subreddit_text);
        subredditText.setText(getArguments().getString(ARG_SUBREDDIT));

        titleText = (EditText) v.findViewById(R.id.title);
        if (!TextUtils.isEmpty(subredditText.getText())) {
            titleText.requestFocus();
        }

        textText = (EditText) v.findViewById(R.id.text);

        if (getActivity().getActionBar() == null) {
            ViewStub vs = (ViewStub) v.findViewById(R.id.button_bar_stub);
            View buttonBar = vs.inflate();
            ok = buttonBar.findViewById(R.id.ok);
            ok.setOnClickListener(this);
            cancel = buttonBar.findViewById(R.id.cancel);
            cancel.setOnClickListener(this);
        }

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
        adapter.setAccountNames(result.accountNames);
        if (!restoringState) {
            accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
        }
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
        adapter.setAccountNames(null);
        if (listener != null) {
            listener.onSubmitFormCancelled();
        }
    }

    public void onClick(View v) {
        if (v == ok) {
            handleSubmit();
        } else if (v == cancel && listener != null) {
            listener.onSubmitFormCancelled();
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
        if (TextUtils.isEmpty(subredditText.getText())) {
            subredditText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (TextUtils.isEmpty(titleText.getText())) {
            titleText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (TextUtils.isEmpty(textText.getText())) {
            textText.setError(getString(R.string.error_blank_field));
            return true;
        }
        if (listener != null) {
            String textOrLink = textText.getText().toString().trim();
            boolean isLink = LINK_PATTERN.matcher(textOrLink).matches();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "isLink: " + isLink);
            }
            Bundle submitExtras = SubmitLinkFragment.newSubmitExtras(
                    adapter.getItem(accountSpinner.getSelectedItemPosition()),
                    subredditText.getText().toString(),
                    titleText.getText().toString(),
                    textOrLink,
                    isLink);
            listener.onSubmitForm(submitExtras);
        }
        return true;
    }
}
