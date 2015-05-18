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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.btmura.android.reddit.net.LoginResult;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.InputFilters;

import java.io.IOException;

public class AddAccountFragment extends Fragment implements OnClickListener {

    public static final String TAG = "AddAccountFragment";

    private static final String ARG_LOGIN = "login";

    public interface OnAccountAddedListener {
        void onAccountAdded(Bundle result);

        void onAccountCancelled();
    }

    private OnAccountAddedListener listener;
    private LoginTask task;

    private EditText login;
    private ProgressBar progress;
    private Button ok;
    private Button cancel;

    public static AddAccountFragment newInstance(String login) {
        Bundle args = new Bundle(1);
        args.putString(ARG_LOGIN, login);

        AddAccountFragment frag = new AddAccountFragment();
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
        View v = inflater.inflate(R.layout.add_account, container, false);

        login = (EditText) v.findViewById(R.id.login);
        login.setFilters(InputFilters.NO_SPACE_FILTERS);
        login.setText(getArguments().getString(ARG_LOGIN));

        progress = (ProgressBar) v.findViewById(R.id.progress);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        ok = (Button) v.findViewById(R.id.ok);
        ok.setOnClickListener(this);

        hideProgress();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (task != null) {
            showProgress();
        } else {
            hideProgress();
        }
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        login.setEnabled(false);
        cancel.setEnabled(false);
        ok.setEnabled(false);
    }

    private void hideProgress() {
        progress.setVisibility(View.INVISIBLE);
        login.setEnabled(true);
        cancel.setEnabled(true);
        ok.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (v == cancel) {
            handleCancel();
        } else if (v == ok) {
            handleAdd();
        }
    }

    private void handleCancel() {
        if (listener != null) {
            listener.onAccountCancelled();
        }
    }

    private void handleAdd() {
        if (login.getText().length() <= 0) {
            login.setError(getString(R.string.error_blank_field));
            return;
        }
        if (login.getError() == null && listener != null) {
            if (task != null) {
                task.cancel(true);
            }
            task = new LoginTask(getActivity(), login.getText(), "");
            task.execute();
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
                if (!AccountProvider.initializeAccount(context, login, result.cookie)) {
                    return errorBundle(R.string.login_importing_error);
                }

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
            task = null;
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
