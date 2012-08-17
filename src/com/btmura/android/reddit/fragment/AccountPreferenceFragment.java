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

package com.btmura.android.reddit.fragment;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;

public class AccountPreferenceFragment extends PreferenceFragment {

    public static final String TAG = "AccountPreferenceFragment";

    public static final String ARG_ACCOUNT_NAME = "accountName";

    private String accountName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        accountName = getArguments().getString(ARG_ACCOUNT_NAME);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.account_preference_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // TODO: Figure out how to move this somehow to SettingsActivity.
        menu.findItem(R.id.menu_add_account).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_remove_account:
                handleRemoveAccount();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleRemoveAccount() {
        AccountManager manager = AccountManager.get(getActivity());
        String accountType = AccountAuthenticator.getAccountType(getActivity());
        Account account = new Account(accountName, accountType);
        final AccountManagerFuture<Boolean> result = manager.removeAccount(account, null, null);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    return result.getResult();
                } catch (OperationCanceledException e) {
                    Log.e(TAG, "handleRemoveAccount", e);
                } catch (AuthenticatorException e) {
                    Log.e(TAG, "handleRemoveAccount", e);
                } catch (IOException e) {
                    Log.e(TAG, "handleRemoveAccount", e);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    PreferenceActivity prefActivity = (PreferenceActivity) getActivity();
                    prefActivity.finishPreferencePanel(AccountPreferenceFragment.this,
                            Activity.RESULT_OK, new Intent());
                } else {
                    Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }
}
