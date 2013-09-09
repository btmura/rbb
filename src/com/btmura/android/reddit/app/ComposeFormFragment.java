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

import java.io.IOException;
import java.util.regex.Matcher;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.app.CaptchaFragment.OnCaptchaGuessListener;
import com.btmura.android.reddit.content.AccountLoader;
import com.btmura.android.reddit.content.AccountLoader.AccountResult;
import com.btmura.android.reddit.database.Subreddits;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Result;
import com.btmura.android.reddit.provider.Provider;
import com.btmura.android.reddit.text.InputFilters;
import com.btmura.android.reddit.util.ComparableFragments;
import com.btmura.android.reddit.util.Strings;
import com.btmura.android.reddit.util.Views;
import com.btmura.android.reddit.widget.AccountNameAdapter;
import com.btmura.android.reddit.widget.AccountSubredditAdapter;
import com.btmura.android.reddit.widget.SubredditAdapter;

/**
 * {@link Fragment} that displays a form for composing submissions and messages.
 */
public class ComposeFormFragment extends Fragment implements
        ComparableFragment,
        LoaderCallbacks<AccountResult>,
        OnCaptchaGuessListener,
        OnClickListener,
        OnItemSelectedListener,
        OnItemClickListener,
        TextWatcher {

    public static final String TAG = "ComposeFragment";

    private static final String ARG_TYPE = "type";

    private static final String ARG_SUBREDDIT_DESTINATION = "subredditDestination";

    private static final String ARG_MESSAGE_DESTINATION = "messageDestination";

    private static final String ARG_TITLE = Intent.EXTRA_SUBJECT;

    private static final String ARG_TEXT = Intent.EXTRA_TEXT;

    private static final String ARG_IS_REPLY = "isReply";

    private static final String ARG_EXTRAS = "extras";

    // The following extras should be passed for COMMENT_REPLY.

    public static final String EXTRA_COMMENT_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_COMMENT_THING_ID = "thingId";

    // The following extras should be passed for MESSAGE_REPLY.

    public static final String EXTRA_MESSAGE_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_MESSAGE_THING_ID = "thingId";

    // The following extras should be passed for EDIT.

    public static final String EXTRA_EDIT_PARENT_THING_ID = "parentThingId";
    public static final String EXTRA_EDIT_THING_ID = "thingId";

    public interface OnComposeFormListener {

        void onComposeFinished();
    }

    private OnComposeFormListener listener;
    private SubmitTask task;
    private boolean isAccountNameInitialized;

    private View progressView;
    private View noAccountView;
    private View accountView;
    private View addAccountButton;

    private View leftContainer;
    private Spinner accountSpinner;
    private AutoCompleteTextView destinationText;
    private EditText titleText;
    private Switch linkSwitch;
    private EditText textText;
    private ProgressBar submitProgress;

    private AccountNameAdapter accountAdapter;
    private SubredditAdapter subredditAdapter;
    private Matcher linkMatcher;

    public static ComposeFormFragment newInstance(int type,
            String subredditDestination,
            String messageDestination,
            String title,
            String text,
            boolean isReply,
            Bundle extras) {
        Bundle args = new Bundle(7);
        args.putInt(ARG_TYPE, type);
        args.putString(ARG_SUBREDDIT_DESTINATION, subredditDestination);
        args.putString(ARG_MESSAGE_DESTINATION, messageDestination);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        args.putBoolean(ARG_IS_REPLY, isReply);
        args.putBundle(ARG_EXTRAS, extras);

        ComposeFormFragment frag = new ComposeFormFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public boolean fragmentEquals(ComparableFragment o) {
        return ComparableFragments.baseEquals(this, o)
                && ComparableFragments.equalInts(this, o, ARG_TYPE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnComposeFormListener) {
            listener = (OnComposeFormListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountAdapter = new AccountNameAdapter(getActivity(), R.layout.account_name_row);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.compose_form, container, false);

        progressView = v.findViewById(R.id.progress);
        noAccountView = v.findViewById(R.id.no_account);
        accountView = v.findViewById(R.id.has_account);

        addAccountButton = v.findViewById(R.id.add_account_button);
        addAccountButton.setOnClickListener(this);

        accountSpinner = (Spinner) v.findViewById(R.id.account_spinner);
        accountSpinner.setAdapter(accountAdapter);
        accountSpinner.setOnItemSelectedListener(this);

        leftContainer = v.findViewById(R.id.left_container);
        destinationText = (AutoCompleteTextView) v.findViewById(R.id.destination_text);
        titleText = (EditText) v.findViewById(R.id.title_text);
        linkSwitch = (Switch) v.findViewById(R.id.link_switch);
        textText = (EditText) v.findViewById(R.id.text_text);
        submitProgress = (ProgressBar) v.findViewById(R.id.submit_progress);

        int type = getType();

        // Set the title for all types.
        CharSequence title = Strings.ellipsize(getArguments().getString(ARG_TITLE), 100);
        switch (type) {
            case ComposeActivity.TYPE_POST:
                titleText.setText(title);
                titleText.setHint(R.string.hint_title);
                break;

            case ComposeActivity.TYPE_MESSAGE:
                if (!TextUtils.isEmpty(title) && getArguments().getBoolean(ARG_IS_REPLY)) {
                    title = getString(R.string.compose_message_reply_title, title);
                }
                titleText.setText(title);
                titleText.setHint(R.string.hint_subject);
                break;

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
            case ComposeActivity.TYPE_EDIT_POST:
            case ComposeActivity.TYPE_EDIT_COMMENT:
                titleText.setVisibility(View.GONE);
                break;

            default:
                throw new IllegalArgumentException();
        }

        // Set the destination for all types.
        switch (type) {
            case ComposeActivity.TYPE_POST:
                String subredditDestination = getArguments().getString(ARG_SUBREDDIT_DESTINATION);
                if (!Subreddits.hasSidebar(subredditDestination)) {
                    subredditDestination = null;
                }
                destinationText.setText(subredditDestination);
                destinationText.setHint(R.string.hint_subreddit);
                destinationText.setFilters(InputFilters.SUBREDDIT_NAME_FILTERS);

                subredditAdapter = AccountSubredditAdapter.newAutoCompleteInstance(getActivity());
                destinationText.setAdapter(subredditAdapter);
                destinationText.setOnItemClickListener(this);
                break;

            case ComposeActivity.TYPE_MESSAGE:
                String messageDestination = getArguments().getString(ARG_MESSAGE_DESTINATION);
                destinationText.setText(messageDestination);
                destinationText.setHint(R.string.hint_username);
                destinationText.setFilters(InputFilters.NO_SPACE_FILTERS);
                break;

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
            case ComposeActivity.TYPE_EDIT_POST:
            case ComposeActivity.TYPE_EDIT_COMMENT:
                destinationText.setVisibility(View.GONE);
                break;

            default:
                throw new IllegalArgumentException();
        }

        // Set text and link switch for all types.
        textText.setText(getArguments().getString(ARG_TEXT));
        if (textText.length() > 0) {
            validateText(textText.getText());
        }

        switch (type) {
            case ComposeActivity.TYPE_POST:
                textText.setHint(R.string.hint_text_or_link);
                textText.addTextChangedListener(this);
                linkSwitch.setVisibility(View.VISIBLE);
                break;

            case ComposeActivity.TYPE_MESSAGE:
                textText.setHint(R.string.hint_message);
                linkSwitch.setVisibility(View.GONE);
                break;

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
            case ComposeActivity.TYPE_EDIT_POST:
            case ComposeActivity.TYPE_EDIT_COMMENT:
                textText.setHint(R.string.hint_comment);
                linkSwitch.setVisibility(View.GONE);
                break;

            default:
                throw new IllegalArgumentException();
        }

        if (!TextUtils.isEmpty(titleText.getText())) {
            textText.requestFocus();
        } else if (!TextUtils.isEmpty(destinationText.getText())) {
            titleText.requestFocus();
        }

        setupAccountSpinner(type);

        if (task != null) {
            disableFields();
        } else {
            enableFields();
        }

        return v;
    }

    private void setupAccountSpinner(int type) {
        switch (type) {
            case ComposeActivity.TYPE_MESSAGE_REPLY:
                Views.setVisibility(View.GONE, leftContainer, accountSpinner);
                break;

            default:
                Views.setVisibility(View.VISIBLE, leftContainer, accountSpinner);
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    private void disableFields() {
        submitProgress.setVisibility(View.VISIBLE);
        accountSpinner.setEnabled(false);
        destinationText.setEnabled(false);
        titleText.setEnabled(false);
        textText.setEnabled(false);
        linkSwitch.setEnabled(false);
    }

    private void enableFields() {
        submitProgress.setVisibility(View.INVISIBLE);
        accountSpinner.setEnabled(true);
        destinationText.setEnabled(true);
        titleText.setEnabled(true);
        textText.setEnabled(true);
        linkSwitch.setEnabled(true);
    }

    @Override
    public Loader<AccountResult> onCreateLoader(int id, Bundle args) {
        // Create loader that doesn't show the app storage account.
        return new AccountLoader(getActivity(), false, false);
    }

    @Override
    public void onLoadFinished(Loader<AccountResult> loader, AccountResult result) {
        boolean hasAccounts = result.accountNames.length > 0;
        progressView.setVisibility(View.GONE);
        noAccountView.setVisibility(hasAccounts ? View.GONE : View.VISIBLE);
        accountView.setVisibility(hasAccounts ? View.VISIBLE : View.GONE);

        accountAdapter.clear();
        if (hasAccounts) {
            accountAdapter.addAll(result.accountNames);
            if (!isAccountNameInitialized) {
                int index = accountAdapter.findAccountName(result.getLastAccount(getActivity()));
                accountSpinner.setSelection(index);
                isAccountNameInitialized = true;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountResult> loader) {
    }

    @Override
    public void onClick(View v) {
        if (v == addAccountButton) {
            MenuHelper.startAddAccountActivity(getActivity());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        if (subredditAdapter != null) {
            String accountName = accountAdapter.getItem(accountSpinner.getSelectedItemPosition());
            subredditAdapter.setAccountName(accountName);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String subreddit = subredditAdapter.getName(position);
        destinationText.setText(subreddit);
        destinationText.setSelection(subreddit.length(), subreddit.length());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.compose_form_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_submit:
                handleSubmit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleSubmit() {
        // Require an account to submit anything.
        if (accountAdapter.isEmpty() || accountSpinner == null || !accountSpinner.isEnabled()) {
            return;
        }

        // CommentActions don't have a choice of destination or title.
        int composition = getArguments().getInt(ARG_TYPE);
        if (composition == ComposeActivity.TYPE_POST
                || composition == ComposeActivity.TYPE_MESSAGE) {
            if (TextUtils.isEmpty(destinationText.getText())) {
                destinationText.setError(getString(R.string.error_blank_field));
                return;
            }
            if (TextUtils.isEmpty(titleText.getText())) {
                titleText.setError(getString(R.string.error_blank_field));
                return;
            }
        }

        if (TextUtils.isEmpty(textText.getText())) {
            textText.setError(getString(R.string.error_blank_field));
            return;
        }

        submit(null, null);
    }

    private void submit(String captchaId, String captchaGuess) {
        onSubmitStarted();

        String accountName = accountAdapter.getItem(accountSpinner.getSelectedItemPosition());
        String destination = destinationText.getText().toString();
        String title = titleText.getText().toString();
        String text = textText.getText().toString();
        boolean isLink = linkSwitch.isChecked();

        int type = getType();
        switch (type) {
            case ComposeActivity.TYPE_POST:
            case ComposeActivity.TYPE_MESSAGE:
                handlePostOrMessage(accountName,
                        destination,
                        title,
                        text,
                        isLink,
                        captchaId,
                        captchaGuess);
                break;

            case ComposeActivity.TYPE_COMMENT_REPLY:
                handleCommentReply(accountName, text);
                break;

            case ComposeActivity.TYPE_MESSAGE_REPLY:
                handleMessageReply(accountName, text);
                break;

            case ComposeActivity.TYPE_EDIT_POST:
            case ComposeActivity.TYPE_EDIT_COMMENT:
                handleEdit(accountName, text);
                break;
        }
    }

    private void handlePostOrMessage(String accountName,
            String destination,
            String title,
            String text,
            boolean isLink,
            String captchaId,
            String captchaGuess) {
        resetTask();
        task = new SubmitTask(getActivity(),
                accountName,
                destination,
                title,
                text,
                isLink,
                captchaId,
                captchaGuess);
        task.execute();
    }

    private void handleCommentReply(String accountName, String text) {
        Bundle extras = getExtras();
        String parentThingId = extras.getString(EXTRA_COMMENT_PARENT_THING_ID);
        String replyThingId = extras.getString(EXTRA_COMMENT_THING_ID);
        Provider.insertCommentAsync(
                getActivity(),
                accountName,
                text,
                parentThingId,
                replyThingId);
        onSubmitFinished();
    }

    private void handleMessageReply(String accountName, String text) {
        Bundle extras = getExtras();
        String parentThingId = extras.getString(EXTRA_MESSAGE_PARENT_THING_ID);
        String thingId = extras.getString(EXTRA_MESSAGE_THING_ID);
        Provider.insertMessageAsync(
                getActivity(),
                accountName,
                text,
                parentThingId,
                thingId);
        onSubmitFinished();
    }

    private void handleEdit(String accountName, String text) {
        Bundle extras = getExtras();
        String parentThingId = extras.getString(EXTRA_EDIT_PARENT_THING_ID);
        String thingId = extras.getString(EXTRA_EDIT_THING_ID);
        Provider.editCommentAsync(
                getActivity(),
                accountName,
                text,
                parentThingId,
                thingId);
        onSubmitFinished();
    }

    @Override
    public void onCaptchaGuess(String id, String guess) {
        submit(id, guess);
    }

    @Override
    public void onCaptchaCancelled() {
        onSubmitCancelled();
    }

    private void onSubmitStarted() {
        disableFields();
    }

    private void onSubmitCancelled() {
        enableFields();
        resetTask();
    }

    private void onSubmitError() {
        enableFields();
        resetTask();
    }

    private void resetTask() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    private void onSubmitFinished() {
        if (listener != null) {
            listener.onComposeFinished();
        }
    }

    class SubmitTask extends AsyncTask<Void, Void, Result> {

        private final Context context;
        private final String accountName;
        private final String destination;
        private final String title;
        private final String text;
        private final boolean isLink;
        private final String captchaId;
        private final String captchaGuess;

        SubmitTask(Context context,
                String accountName,
                String destination,
                String title,
                String text,
                boolean isLink,
                String captchaId,
                String captchaGuess) {
            this.context = context.getApplicationContext();
            this.accountName = accountName;
            this.destination = destination;
            this.title = title;
            this.text = text;
            this.isLink = isLink;
            this.captchaId = captchaId;
            this.captchaGuess = captchaGuess;
        }

        @Override
        protected Result doInBackground(Void... voidRay) {
            try {
                AccountManager manager = AccountManager.get(context);
                Account account = AccountUtils.getAccount(context, accountName);

                // Get account cookie or bail out.
                String cookie = AccountUtils.getCookie(manager, account);
                if (cookie == null) {
                    return null;
                }

                // Get account modhash or bail out.
                String modhash = AccountUtils.getModhash(manager, account);
                if (modhash == null) {
                    return null;
                }

                switch (getType()) {
                    case ComposeActivity.TYPE_POST:
                        return RedditApi.submit(destination, title, text, isLink,
                                captchaId, captchaGuess, cookie, modhash);

                    case ComposeActivity.TYPE_MESSAGE:
                        return RedditApi.compose(destination, title, text,
                                captchaId, captchaGuess, cookie, modhash);

                    default:
                        throw new IllegalArgumentException();
                }

            } catch (OperationCanceledException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (AuthenticatorException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            onSubmitCancelled();
        }

        @Override
        protected void onPostExecute(Result result) {
            if (result == null) {
                showError(getString(R.string.error));
            } else if (result.hasRateLimitError()) {
                showError(result.getErrorMessage(context));
            } else if (result.hasBadCaptchaError()) {
                showCaptcha(result.captcha);
            } else if (result.hasErrors()) {
                showError(result.getErrorMessage(context));
            } else {
                finish();
            }
        }

        private void showCaptcha(String captchaId) {
            CaptchaFragment frag = CaptchaFragment.newInstance(captchaId);
            frag.setTargetFragment(ComposeFormFragment.this, 0);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(frag, CaptchaFragment.TAG);
            ft.commit();
        }

        private void showError(CharSequence error) {
            MessageDialogFragment.showMessage(getFragmentManager(), error);
            onSubmitError();
        }

        private void finish() {
            onSubmitFinished();
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        validateText(s);
    }

    private void validateText(CharSequence s) {
        if (linkMatcher == null) {
            linkMatcher = Patterns.WEB_URL.matcher(s);
        }
        linkSwitch.setChecked(linkMatcher.reset(s).matches());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    public String getTitle(Context context) {
        switch (getType()) {
            case ComposeActivity.TYPE_POST:
                return context.getString(R.string.compose_title_post);

            case ComposeActivity.TYPE_MESSAGE:
                return context.getString(R.string.compose_title_message);

            case ComposeActivity.TYPE_COMMENT_REPLY:
            case ComposeActivity.TYPE_MESSAGE_REPLY:
                return context.getString(R.string.compose_title_reply, getMessageDestination());

            case ComposeActivity.TYPE_EDIT_POST:
                return context.getString(R.string.compose_title_edit_post);

            case ComposeActivity.TYPE_EDIT_COMMENT:
                return context.getString(R.string.compose_title_edit_comment);

            default:
                throw new IllegalArgumentException();
        }
    }

    private int getType() {
        return getArguments().getInt(ARG_TYPE);
    }

    private String getMessageDestination() {
        return getArguments().getString(ARG_MESSAGE_DESTINATION);
    }

    private Bundle getExtras() {
        return getArguments().getBundle(ARG_EXTRAS);
    }
}
