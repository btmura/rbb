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
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.R;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

  public static final String TAG = "AccountAuthenticator";

  /** Access token that must not be expired to make requests. */
  static final String ACCESS_TOKEN = "accessToken";

  /** Refresh token that can be used to refresh the access token. */
  static final String REFRESH_TOKEN = "refreshToken";

  /** Expiration time in milliseconds of the access token. */
  static final String EXPIRATION_MS = "expirationMs";

  /** Scopes the user granted the account. Example: read history identity */
  static final String SCOPES = "scopes";

  private final Context ctx;

  public static String getAccountType(Context context) {
    return context.getString(R.string.account_type);
  }

  public AccountAuthenticator(Context ctx) {
    super(ctx);
    this.ctx = ctx;
  }

  @Override
  public Bundle addAccount(
      AccountAuthenticatorResponse response,
      String accountType,
      String authTokenType,
      String[] requiredFeatures,
      Bundle options) throws NetworkErrorException {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "addAccount");
    }

    Intent intent = new Intent(ctx, AccountAuthenticatorActivity.class);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
        response);

    Bundle result = new Bundle(1);
    result.putParcelable(AccountManager.KEY_INTENT, intent);
    return result;
  }

  @Override
  public Bundle confirmCredentials(
      AccountAuthenticatorResponse response,
      Account account,
      Bundle options) throws NetworkErrorException {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "confirmCredentials");
    }
    return createIntentResult(response, account.name);
  }

  @Override
  public Bundle editProperties(
      AccountAuthenticatorResponse response,
      String accountType) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "editProperties");
    }
    return createIntentResult(response, null);
  }

  @Override
  public Bundle getAuthToken(
      AccountAuthenticatorResponse response,
      Account account,
      String authTokenType,
      Bundle options) throws NetworkErrorException {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "getAuthToken: authTokenType: " + authTokenType);
    }
    return createIntentResult(response, account.name);
  }

  @Override
  public String getAuthTokenLabel(String authTokenType) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "getAuthTokenLabel");
    }
    return null; // OK to return null according to docs.
  }

  @Override
  public Bundle hasFeatures(
      AccountAuthenticatorResponse response,
      Account account,
      String[] features) throws NetworkErrorException {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "hasFeatures");
    }

    Bundle result = new Bundle(1);
    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
    return result;
  }

  @Override
  public Bundle updateCredentials(
      AccountAuthenticatorResponse response,
      Account account,
      String authTokenType,
      Bundle options) throws NetworkErrorException {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "updateCredentials");
    }
    return createIntentResult(response, account.name);
  }

  private Bundle createIntentResult(
      AccountAuthenticatorResponse response,
      String accountName) {
    Intent intent = new Intent(ctx, AccountAuthenticatorActivity.class);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
        response);
    if (!TextUtils.isEmpty(accountName)) {
      intent.putExtra(AccountAuthenticatorActivity.EXTRA_ACCOUNT_NAME,
          accountName);
    }

    Bundle result = new Bundle(1);
    result.putParcelable(AccountManager.KEY_INTENT, intent);
    return result;
  }
}
