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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

    public static final String TAG = "AccountAuthenticator";

    public static final String AUTH_TOKEN_COOKIE = "cookie";
    public static final String AUTH_TOKEN_MODHASH = "modhash";

    private final Context context;

    public static String getAccountType(Context context) {
        return context.getString(R.string.account_type);
    }

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "addAccount");
        }

        Intent intent = new Intent(context, AccountAuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle result = new Bundle(1);
        result.putParcelable(AccountManager.KEY_INTENT, intent);
        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "confirmCredentials");
        }
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "editProperties");
        }
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getAuthToken");
        }
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "getAuthTokenLabel");
        }
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "hasFeatures");
        }
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "updateCredentials");
        }
        return null;
    }
}
