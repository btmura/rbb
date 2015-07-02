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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.accounts.AccountUtils;
import com.btmura.android.reddit.net.AccessTokenResult;
import com.btmura.android.reddit.net.RedditApi;
import com.btmura.android.reddit.provider.AccountProvider;
import com.btmura.android.reddit.provider.SubredditProvider;
import com.btmura.android.reddit.provider.ThingProvider;

import java.io.IOException;

/**
 * Loader that adds an account given a username and the OAuth callback URL.
 */
public class AddAccountLoader extends BaseAsyncTaskLoader<Bundle> {

  private static final String TAG = "AddAccountLoader";

  private static final boolean DEBUG = BuildConfig.DEBUG;

  private final String username;
  private final String code;

  public AddAccountLoader(Context context, String username, String code) {
    super(context.getApplicationContext());
    this.username = username;
    this.code = code;
  }

  @Override
  public Bundle loadInBackground() {
    try {
      // TODO(btmura): remove this once dialog is properly restored
      if (DEBUG) {
        SystemClock.sleep(10 * 1000);
      }

      Context ctx = getContext();
      AccessTokenResult atr = RedditApi.getAccessToken(ctx, code);
      if (!hasRequiredTokens(atr)) {
        return errorBundle(R.string.error_bad_access_token);
      }

      Account a = AccountUtils.getAccount(ctx, username);
      AccountManager am = AccountManager.get(ctx);
      if (!addAccount(am, a, atr)) {
        return errorBundle(R.string.error_adding_account);
      }

      if (!AccountProvider.initializeAccount(ctx, username)) {
        removeAccount(am, a);
        return errorBundle(R.string.error_initializing_account);
      }

      // Set this account as the last account to make the UI switch to the
      // new account after the user returns to the app. If somehow we crash
      // before the account is added, that is ok, because the AccountLoader
      // will fall back to the app storage account.
      AccountPrefs.setLastAccount(ctx, username);

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

  private boolean addAccount(
      AccountManager am,
      Account a,
      AccessTokenResult atr) {
    if (am.addAccountExplicitly(a, null /* pw */, null /* userdata */)) {
      am.setAuthToken(a, AccountAuthenticator.AUTH_TOKEN_ACCESS_TOKEN,
          atr.accessToken);
      am.setAuthToken(a, AccountAuthenticator.AUTH_TOKEN_REFRESH_TOKEN,
          atr.refreshToken);
      return true;
    }
    return false;
  }

  private boolean removeAccount(AccountManager am, Account a) {
    // TODO(btmura): use new API to remove account when possible
    // TODO(btmura): removed code duplication with AccountListFragment
    try {
      return am.removeAccount(a, null, null).getResult();
    } catch (OperationCanceledException e) {
      Log.e(TAG, e.getMessage(), e);
      return false;
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
      return false;
    } catch (AuthenticatorException e) {
      Log.e(TAG, e.getMessage(), e);
      return false;
    }
  }

  private Bundle errorBundle(int resId) {
    Bundle b = new Bundle(1);
    b.putString(AccountManager.KEY_ERROR_MESSAGE,
        getContext().getString(resId));
    return b;
  }
}


