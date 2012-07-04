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

import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;

import com.btmura.android.reddit.Debug;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.fragment.AddAccountFragment;
import com.btmura.android.reddit.fragment.AddAccountFragment.OnAccountAddedListener;

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
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {



                return null;
            }
        }.execute();


//        new AsyncTask<Void, Void, Bundle>() {
//            @Override
//            protected Bundle doInBackground(Void... params) {
//                String accountType = AccountAuthenticator.getAccountType(AccountAuthenticatorActivity.this);
//                Account account = new Account(login, accountType);
//
//                AccountManager manager = AccountManager.get(AccountAuthenticatorActivity.this);
//                manager.addAccountExplicitly(account, null, null);
//                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_COOKIE, cookie);
//                manager.setAuthToken(account, AccountAuthenticator.AUTH_TOKEN_MODHASH, modhash);
//
//                ContentResolver.setSyncAutomatically(account, SubredditProvider.AUTHORITY, true);
//
//                Bundle extras = new Bundle(2);
//                extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
//                extras.putBoolean(SyncAdapterService.EXTRA_INITIAL_SYNC, true);
//                ContentResolver.requestSync(account, SubredditProvider.AUTHORITY, extras);
//
//                Bundle result = new Bundle(2);
//                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
//                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
//                return result;
//            }
//
//            @Override
//            protected void onPostExecute(Bundle result) {
//                setAccountAuthenticatorResult(result);
//                setResult(RESULT_OK);
//                finish();
//            }
//        }.execute();
    }

    public void onAccountCancelled() {
        finish();
    }
}

