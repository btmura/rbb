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

import com.btmura.android.reddit.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

public class AccountUtils {

    public static boolean isAccount(String accountName) {
        return !TextUtils.isEmpty(accountName);
    }

    public static String getCookie(Context context, String accountName)
            throws OperationCanceledException, AuthenticatorException, IOException {
        if (TextUtils.isEmpty(accountName)) {
            return null;
        }

        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(context.getString(R.string.account_type));
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equals(accountName)) {
                return manager.blockingGetAuthToken(accounts[i],
                        AccountAuthenticator.AUTH_TOKEN_COOKIE,
                        true);
            }
        }
        return null;
    }

    private AccountUtils() {
    }
}
