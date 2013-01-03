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

import java.util.regex.Matcher;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.ComposeActivity.OnComposeActivityListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.text.InputFilters;
import com.btmura.android.reddit.widget.AccountNameAdapter;

/**
 * {@link Fragment} that displays a form for composing submissions and messages.
 */
public class ComposeFormFragment extends Fragment implements LoaderCallbacks<AccountResult>,
        TextWatcher, OnComposeActivityListener {

    // This fragment only reports back the user's input and doesn't handle
    // modifying the database. The caller of this fragment should handle that.

    /** Integer argument indicating the type of composition. */
    public static final String ARG_TYPE = "composition";

    /** String extra to pre-fill the subreddit field for new posts. */
    public static final String ARG_SUBREDDIT_DESTINATION = "subredditDestination";

    /** String extra to pre-fill the user name field for new messages. */
    public static final String ARG_MESSAGE_DESTINATION = "messageDestination";

    /** Optional string extra with suggested title. */
    private static final String ARG_TITLE = "title";

    /** Optional string extra with suggested text. */
    private static final String ARG_TEXT = "text";

    /** Integer ID that may be used to identify this fragment. */
    private static final String ARG_ID = "id";

    public interface OnComposeFormListener {

        /**
         * Method fired when OK is pressed and basic validations has passed.
         *
         * @param accountName of the account composing the message
         * @param destination whether subreddit or user name
         * @param title or subject of the composition
         * @param text of the composition
         * @param isLink is true if the text is a link
         */
        void onComposeForm(String accountName, String destination, String title, String text,
                boolean isLink);
    }

    /** Listener to notify on compose events. */
    private OnComposeFormListener listener;

    /** Flag for initially setting account spinner once. */
    private boolean restoringState;

    /** Adapter of account names to select who will be composing. */
    private AccountNameAdapter adapter;

    /** {@link Spinner} containing all the acounts who can compose. */
    private Spinner accountSpinner;

    /** {@link EditText} for either subreddit or username. */
    private EditText destinationText;

    /** {@link EditText} for either title or subject. */
    private EditText titleText;

    /** {@link Switch} indicating text if off and link if on. */
    private Switch linkSwitch;

    /** {@link EditText} for either submission text or message. */
    private EditText textText;

    /** Matcher used to check whether the text is a link or not. */
    private Matcher linkMatcher;

    public static ComposeFormFragment newInstance(int type, String subredditDestination,
            String messageDestination, String title, String text, int id) {
        Bundle args = new Bundle(6);
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_SUBREDDIT_DESTINATION, subredditDestination);
        args.putString(ARG_MESSAGE_DESTINATION, messageDestination);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        args.putInt(ARG_ID, id);
        ComposeFormFragment frag = new ComposeFormFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnComposeFormListener) {
            listener = (OnComposeFormListener) activity;
        }
        if (activity instanceof ComposeActivity) {
            ((ComposeActivity) activity).addOnComposeActivityListener(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        restoringState = savedInstanceState != null;

        // Fill the adapter with loading to avoid jank.
        adapter = new AccountNameAdapter(getActivity(), R.layout.account_name_row);
        adapter.add(getString(R.string.loading));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compose_form, container, false);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setEnabled(false);
        accountSpinner.setAdapter(adapter);

        destinationText = (EditText) v.findViewById(R.id.destination_text);

        titleText = (EditText) v.findViewById(R.id.title_text);
        titleText.setText(getArguments().getString(ARG_TITLE));

        linkSwitch = (Switch) v.findViewById(R.id.link_switch);

        textText = (EditText) v.findViewById(R.id.text_text);
        textText.setText(getArguments().getString(ARG_TEXT));
        if (textText.length() > 0) {
            validateText(textText.getText());
        }

        switch (getArguments().getInt(ARG_TYPE)) {
            case ComposeActivity.TYPE_MESSAGE:
                destinationText.setText(getArguments().getString(ARG_MESSAGE_DESTINATION));
                destinationText.setHint(R.string.hint_username);
                destinationText.setFilters(InputFilters.NO_SPACE_FILTERS);
                titleText.setHint(R.string.hint_subject);
                textText.setHint(R.string.hint_message);
                linkSwitch.setVisibility(View.GONE);
                break;

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
                destinationText.setVisibility(View.GONE);
                titleText.setVisibility(View.GONE);
                textText.setHint(R.string.hint_comment);
                linkSwitch.setVisibility(View.GONE);
                break;

            default:
                destinationText.setText(getArguments().getString(ARG_SUBREDDIT_DESTINATION));
                destinationText.setHint(R.string.hint_subreddit);
                destinationText.setFilters(InputFilters.SUBREDDIT_NAME_FILTERS);
                titleText.setHint(R.string.hint_title);
                textText.setHint(R.string.hint_text_or_link);
                textText.addTextChangedListener(this);
                linkSwitch.setVisibility(View.VISIBLE);
                break;
        }

        if (!TextUtils.isEmpty(titleText.getText())) {
            textText.requestFocus();
        } else if (!TextUtils.isEmpty(destinationText.getText())) {
            titleText.requestFocus();
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        // Create loader that doesn't show the app storage account.
        return new AccountLoader(getActivity(), false);
    }

    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        boolean hasAccounts = result.accountNames.length > 0;
        accountSpinner.setEnabled(hasAccounts);
        // if (ok != null) {
        // ok.setEnabled(hasAccounts);
        // }

        adapter.clear();
        if (hasAccounts) {
            adapter.addAll(result.accountNames);

            // Only setup spinner when not changing configs. Widget will handle
            // selecting the last account on config changes on its own.
            if (!restoringState) {
                accountSpinner.setSelection(adapter.findAccountName(result.getLastAccount()));
            }
        } else {
            adapter.add(getString(R.string.empty_accounts));
        }
        getActivity().invalidateOptionsMenu();
    }

    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.compose_form_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_submit).setEnabled(accountSpinner.isEnabled());
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

    public void onOkClicked(int id) {
        if (getArguments().getInt(ARG_ID) == id) {
            handleSubmit();
        }
    }

    private boolean handleSubmit() {
        // CommentActions don't have a choice of destination or title.
        int composition = getArguments().getInt(ARG_TYPE);
        if (composition == ComposeActivity.TYPE_POST
                || composition == ComposeActivity.TYPE_MESSAGE) {
            if (TextUtils.isEmpty(destinationText.getText())) {
                destinationText.setError(getString(R.string.error_blank_field));
                return true;
            }
            if (TextUtils.isEmpty(titleText.getText())) {
                titleText.setError(getString(R.string.error_blank_field));
                return true;
            }
        }

        if (TextUtils.isEmpty(textText.getText())) {
            textText.setError(getString(R.string.error_blank_field));
            return true;
        }

        if (listener != null) {
            listener.onComposeForm(adapter.getItem(accountSpinner.getSelectedItemPosition()),
                    destinationText.getText().toString(),
                    titleText.getText().toString(),
                    textText.getText().toString(),
                    linkSwitch.isChecked());
        }

        return true;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        validateText(s);
    }

    private void validateText(CharSequence s) {
        if (linkMatcher == null) {
            linkMatcher = Patterns.WEB_URL.matcher(s);
        }
        linkSwitch.setChecked(linkMatcher.reset(s).matches());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable s) {
    }
}
