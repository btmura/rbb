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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.content.AccountPrefs;
import com.btmura.android.reddit.net.LoginResult;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;
import com.btmura.android.reddit.text.InputFilters;

public class AddAccountFragment extends Fragment implements
        OnCheckedChangeListener,
        OnClickListener {

    public static final String TAG = "AddAccountFragment";

    private static final String ARG_LOGIN = "login";

    public interface OnAccountAddedListener {
        void onAccountAdded(Bundle result);

        void onAccountCancelled();
    }

    private OnAccountAddedListener listener;

    private EditText login;
    private EditText password;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnAccountAddedListener) {
            listener = (OnAccountAddedListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_account, container, false);

        login = (EditText) v.findViewById(R.id.login);
        login.setFilters(InputFilters.NO_SPACE_FILTERS);
        login.setText(getArguments().getString(ARG_LOGIN));

        password = (EditText) v.findViewById(R.id.password);
        if (!TextUtils.isEmpty(login.getText())) {
            password.requestFocus();
        }

        CheckBox showPassword = (CheckBox) v.findViewById(R.id.show_password);
        showPassword.setOnCheckedChangeListener(this);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        ok = (Button) v.findViewById(R.id.ok);
        ok.setOnClickListener(this);

        return v;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransformationMethod method = null;
        if (!isChecked) {
            method = PasswordTransformationMethod.getInstance();
        }
        password.setTransformationMethod(method);
    }

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
        if (password.getText().length() <= 0) {
            password.setError(getString(R.string.error_blank_field));
            return;
        }
        if (login.getError() == null && password.getError() == null && listener != null) {
            new LoginTask(getActivity(), login.getText().toString(), password.getText().toString()).execute();
        }
    }

    class LoginTask extends AsyncTask<Void, Integer, Bundle> {

        private final Context context;
        private final String login;
        private final String password;

        LoginTask(Context context, String login, String password) {
            this.context = context.getApplicationContext();
            this.login = login;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getFragmentManager(),
                    context.getString(R.string.login_logging_in));
        }

        @Override
        protected Bundle doInBackground(Void... params) {
            try {
                LoginResult result = RedditApi.login(context, login, password);
                if (result.error != null) {
                    return errorBundle(R.string.reddit_error, result.error);
                }

                publishProgress(R.string.login_importing);

                // Initialize database data for the account. If somehow we fail
                // to add the account later to the AccountManager, then this
                // data will just sit there in the db. If there is an existing
                // account, then they will still see their subreddits but any
                // pending actions will be erased...
                if (!AccountProvider.initializeAccount(context, login, result.cookie)) {
                    return errorBundle(R.string.login_importing_error);
                }

                publishProgress(R.string.login_adding_account);

                // Set this account as the last account to make the UI switch to
                // the new account after the user returns to the app. If somehow
                // we crash before the account is added, that is ok, because the
                // AccountLoader will fall back to the app storage account.
                AccountPrefs.setLastAccount(context, login);

                String accountType = AccountAuthenticator.getAccountType(context);
                Account account = new Account(login, accountType);

                AccountManager manager = AccountManager.get(context);
                manager.addAccountExplicitly(account, null, null);
                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_COOKIE, result.cookie);
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
        protected void onProgressUpdate(Integer... resIds) {
            ProgressDialogFragment.showDialog(getFragmentManager(), context.getString(resIds[0]));
        }

        @Override
        protected void onCancelled(Bundle result) {
            ProgressDialogFragment.dismissDialog(getFragmentManager());
        }

        @Override
        protected void onPostExecute(Bundle result) {
            ProgressDialogFragment.dismissDialog(getFragmentManager());

            String error = result.getString(AccountManager.KEY_ERROR_MESSAGE);
            if (error != null) {
                MessageDialogFragment.showMessage(getFragmentManager(), error);
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
