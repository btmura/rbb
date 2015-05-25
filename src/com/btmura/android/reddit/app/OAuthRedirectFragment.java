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
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.content.Contexts;
import com.btmura.android.reddit.net.LoginResult;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.net.Urls;
import com.btmura.android.reddit.net.Urls2;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.InputFilters;

import java.io.IOException;

public class OAuthRedirectFragment extends Fragment implements OnClickListener {

    public static final String TAG = "OAuthRedirectFragment";

    private static final String ARG_USERNAME = "username";

    // TODO(btmura): move to AddAccountFragment
    public interface OnAccountAddedListener {
        void onAccountAdded(Bundle result);

        void onAccountCancelled();
    }

    private OnAccountAddedListener listener;

    private EditText username;
    private ProgressBar progress;
    private Button loginButton;
    private Button cancelButton;

    public static OAuthRedirectFragment newInstance(String username) {
        Bundle args = new Bundle(1);
        args.putString(ARG_USERNAME, username);

        OAuthRedirectFragment frag = new OAuthRedirectFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnAccountAddedListener) {
            listener = (OnAccountAddedListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.oauth_redirect, container, false);

        username = (EditText) v.findViewById(R.id.username);
        username.setFilters(InputFilters.NO_SPACE_FILTERS);
        username.setText(getArguments().getString(ARG_USERNAME));

        progress = (ProgressBar) v.findViewById(R.id.progress);

        cancelButton = (Button) v.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(this);

        loginButton = (Button) v.findViewById(R.id.ok);
        loginButton.setText(R.string.login);
        loginButton.setOnClickListener(this);

        hideProgress();

        return v;
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        username.setEnabled(false);
        cancelButton.setEnabled(false);
        loginButton.setEnabled(false);
    }

    private void hideProgress() {
        progress.setVisibility(View.INVISIBLE);
        username.setEnabled(true);
        cancelButton.setEnabled(true);
        loginButton.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v == cancelButton) {
            handleCancel();
        } else if (v == loginButton) {
            handleAdd();
        }
    }

    private void handleCancel() {
        if (listener != null) {
            listener.onAccountCancelled();
        }
    }

    private void handleAdd() {
        if (TextUtils.isEmpty(username.getText())) {
            username.setError(getString(R.string.error_blank_field));
            return;
        }
        if (username.getError() == null && listener != null) {
            // TODO(btmura): check for existing account
            forwardToBrowserLogin(username.getText());
        }
    }

    private void forwardToBrowserLogin(CharSequence username) {
        StringBuilder state = new StringBuilder("rbb_").append(username);
        CharSequence url = Urls2.authorize(getActivity(), state);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url.toString()));
        if (!Contexts.startActivity(getActivity(), intent)) {
            // TODO(btmura): handle no browser case
        }
    }

    class LoginTask extends AsyncTask<Void, Integer, Bundle> {

        private final Context context;
        private final String login;
        private final String password;

        LoginTask(Context context, CharSequence login, CharSequence password) {
            this.context = context.getApplicationContext();
            this.login = login.toString();
            this.password = password.toString();
        }

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Bundle doInBackground(Void... params) {
            try {
                LoginResult result = RedditApi.login(login, password);
                if (result.error != null) {
                    return errorBundle(R.string.reddit_error, result.error);
                }

                // Initialize database data for the account. If somehow we fail
                // to add the account later to the AccountManager, then this
                // data will just sit there in the db. If there is an existing
                // account, then they will still see their subreddits but any
                // pending actions will be erased...
//                if (!AccountProvider.initializeAccount(context, login, result.cookie)) {
//                    return errorBundle(R.string.login_importing_error);
//                }

                // Set this account as the last account to make the UI switch to
                // the new account after the user returns to the app. If somehow
                // we crash before the account is added, that is ok, because the
                // AccountLoader will fall back to the app storage account.
                AccountPrefs.setLastAccount(context, login);

                String accountType = AccountAuthenticator.getAccountType(context);
                Account account = new Account(login, accountType);

                AccountManager manager = AccountManager.get(context);
                manager.addAccountExplicitly(account, null, null);
                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_COOKIE,
                        result.cookie);
                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_MODHASH,
                        result.modhash);

                ContentResolver.setSyncAutomatically(account, AccountProvider.AUTHORITY, true);
                ContentResolver.setSyncAutomatically(account, SubredditProvider.AUTHORITY, true);
                ContentResolver.setSyncAutomatically(account, ThingProvider.AUTHORITY, true);

                Bundle b = new Bundle(2);
                b.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                b.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                return b;

            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                return errorBundle(R.string.login_error, e.getMessage());
            }
        }

        @Override
        protected void onCancelled(Bundle result) {
            hideProgress();
        }

        @Override
        protected void onPostExecute(Bundle result) {
            String error = result.getString(AccountManager.KEY_ERROR_MESSAGE);
            if (error != null) {
                MessageDialogFragment.showMessage(getFragmentManager(), error);
                hideProgress();
            } else if (listener != null) {
                listener.onAccountAdded(result);
            }
        }

        private Bundle errorBundle(int resId, String... formatArgs) {
            Bundle b = new Bundle(1);
            b.putString(AccountManager.KEY_ERROR_MESSAGE,
                    context.getString(resId, (Object[]) formatArgs));
            return b;
        }
    }
}
