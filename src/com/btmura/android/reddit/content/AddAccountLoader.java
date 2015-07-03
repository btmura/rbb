/*
 * Copyright (C) 2015 Brian Muramatsu
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

package com.btmura.android.reddit.content;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.net.AccessTokenResult;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;

import java.io.IOException;

/**
 * Loader that adds an account given a accountName and the OAuth callback URL.
 */
public class AddAccountLoader extends BaseAsyncTaskLoader<Bundle> {

  private static final String TAG = "AddAccountLoader";

  private final String accountName;
  private final String code;

  public AddAccountLoader(Context context, String accountName, String code) {
    super(context.getApplicationContext());
    this.accountName = accountName;
    this.code = code;
  }

  @Override
  public Bundle loadInBackground() {
    try {
      Context ctx = getContext();
      AccessTokenResult atr = RedditApi.getAccessToken(ctx, code);
      if (!hasRequiredTokens(atr)) {
        return errorBundle(R.string.error_bad_access_token);
      }

      Account a = AccountUtils.getAccount(ctx, accountName);
      if (!AccountUtils.addAccount(ctx, a.name, atr.accessToken,
          atr.refreshToken, atr.expirationMs, atr.scope)) {
        return errorBundle(R.string.error_adding_account);
      }

      if (!AccountProvider.initializeAccount(ctx, accountName)) {
        AccountUtils.removeAccount(ctx, accountName);
        return errorBundle(R.string.error_initializing_account);
      }

      // Set this account as the last account to make the UI switch to the
      // new account after the user returns to the app. If somehow we crash
      // before the account is added, that is ok, because the AccountLoader
      // will fall back to the app storage account.
      AccountPrefs.setLastAccount(ctx, accountName);

      ContentResolver.setSyncAutomatically(a,
          AccountProvider.AUTHORITY, true);
      ContentResolver.setSyncAutomatically(a,
          SubredditProvider.AUTHORITY, true);
      ContentResolver.setSyncAutomatically(a,
          ThingProvider.AUTHORITY, true);

      Bundle b = new Bundle(2);
      b.putString(AccountManager.KEY_ACCOUNT_NAME, a.name);
      b.putString(AccountManager.KEY_ACCOUNT_TYPE, a.type);
      return b;
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
      return errorBundle(R.string.error_io);
    }
  }

  private boolean hasRequiredTokens(AccessTokenResult atr) {
    return !TextUtils.isEmpty(atr.accessToken)
        && !TextUtils.isEmpty(atr.refreshToken);
  }

  private Bundle errorBundle(int resId) {
    Bundle b = new Bundle(1);
    b.putString(AccountManager.KEY_ERROR_MESSAGE,
        getContext().getString(resId));
    return b;
  }
}


