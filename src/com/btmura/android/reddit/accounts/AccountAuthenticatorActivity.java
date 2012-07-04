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

package com.btmura.android.reddit.accounts;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.entity.LoginResult;
import com.btmura.android.reddit.fragment.AddAccountFragment;
import com.btmura.android.reddit.fragment.AddAccountFragment.OnAccountAddedListener;
import com.btmura.android.reddit.fragment.SimpleDialogFragment;
import com.btmura.android.reddit.provider.NetApi;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.SyncAdapterService;

public class AccountAuthenticatorActivity extends android.accounts.AccountAuthenticatorActivity
        implements OnAccountAddedListener {

    public static final String TAG = "AccountAuthenticatorActivity";
    public static final boolean DEBUG = Debug.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_authenticator);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.single_container, AddAccountFragment.newInstance());
        ft.commit();
    }

    public void onAccountAdded(final String login, final String password) {
        new LoginTask(login, password).execute();
    }

    public void onAccountCancelled() {
        finish();
    }

    class LoginTask extends AsyncTask<Void, Integer, Bundle> {

        private final String login;
        private final String password;
        private final ProgressDialog progress;

        LoginTask(String login, String password) {
            this.login = login;
            this.password = password;
            this.progress = new ProgressDialog(AccountAuthenticatorActivity.this);
        }

        @Override
        protected void onPreExecute() {
            progress.setMessage(getString(R.string.authenticator_logging_in));
            progress.show();
        }

        @Override
        protected Bundle doInBackground(Void... params) {
            try {
                LoginResult result = NetApi.login(AccountAuthenticatorActivity.this, login, password);
                if (result.error != null) {
                    return errorBundle(R.string.authenticator_reddit_error, result.error);
                }

                publishProgress(R.string.authenticator_adding_account);

                String accountType = AccountAuthenticator.getAccountType(AccountAuthenticatorActivity.this);
                Account account = new Account(login, accountType);

                AccountManager manager = AccountManager.get(AccountAuthenticatorActivity.this);
                manager.addAccountExplicitly(account, null, null);
                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_COOKIE, result.cookie);
                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_MODHASH, result.modhash);

                ContentResolver.setSyncAutomatically(account, SubredditProvider.AUTHORITY, true);

                Bundle extras = new Bundle(2);
                extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                extras.putBoolean(SyncAdapterService.EXTRA_INITIAL_SYNC, true);
                ContentResolver.requestSync(account, SubredditProvider.AUTHORITY, extras);

                Bundle b = new Bundle(2);
                b.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                b.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
                return b;

            } catch (IOException e) {
                Log.e(TAG, "doInBackground", e);
                return errorBundle(R.string.authenticator_error, e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setMessage(getString(values[0]));
        }

        @Override
        protected void onCancelled(Bundle result) {
            progress.dismiss();
        }

        @Override
        protected void onPostExecute(Bundle result) {
            progress.dismiss();

            String error = result.getString(AccountManager.KEY_ERROR_MESSAGE);
            if (error != null) {
                SimpleDialogFragment.showMessage(getFragmentManager(), error);
            } else {
                setAccountAuthenticatorResult(result);
                setResult(RESULT_OK);
                finish();
            }
        }

        private Bundle errorBundle(int resId, String... formatArgs) {
            Bundle b = new Bundle(1);
            b.putString(AccountManager.KEY_ERROR_MESSAGE, getString(resId, (Object[]) formatArgs));
            return b;
        }
    }
}

