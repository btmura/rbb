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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.R;

import java.io.IOException;

public class AccountUtils {

  private static final String TAG = "AccountUtils";

  public static final String NO_ACCOUNT = "";

  public static boolean isAccount(String accountName) {
    return !TextUtils.isEmpty(accountName);
  }

  public static Account getAccount(Context ctx, String accountName) {
    return new Account(accountName, getAccountType(ctx));
  }

  public static String getAccountType(Context ctx) {
    return ctx.getString(R.string.account_type);
  }

  @Nullable
  public static String getAccessToken(Context ctx, String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    return getAuthToken(ctx, accountName, AccountAuthenticator.ACCESS_TOKEN);
  }

  @Nullable
  public static String getRefreshToken(Context ctx, String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    return getAuthToken(ctx, accountName, AccountAuthenticator.REFRESH_TOKEN);
  }

  @Nullable
  private static String getAuthToken(
      Context ctx,
      String accountName,
      String authTokenType) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    if (!isAccount(accountName)) {
      return null;
    }
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);
    return am.blockingGetAuthToken(a, authTokenType, true /* notify */);
  }

  public static boolean hasCredentials(Context ctx, String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    return !isAccount(accountName)
        || getAccessToken(ctx, accountName) != null
        && getRefreshToken(ctx, accountName) != null;
  }

  public static boolean hasExpiredCredentials(Context ctx, String accountName)
      throws AuthenticatorException, OperationCanceledException, IOException {
    if (!isAccount(accountName) || !hasCredentials(ctx, accountName)) {
      return false;
    }
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);
    String expValue = am.getUserData(a, AccountAuthenticator.EXPIRATION_MS);
    if (TextUtils.isEmpty(expValue)) {
      Log.wtf(TAG, "expiration is missing");
      return true;
    }
    try {
      long expirationMs = Long.valueOf(expValue);
      return System.currentTimeMillis() >= expirationMs;
    } catch (NumberFormatException e) {
      Log.wtf(TAG, e);
      return true;
    }
  }

  public static boolean addAccount(
      Context ctx,
      String accountName,
      String accessToken,
      String refreshToken,
      long expirationMs,
      String scopes) {
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);

    // This returns false if an account already exists.
    // We will just overwrite the existing tokens and info.
    am.addAccountExplicitly(a, null /* pw */, null /* userdata */);

    am.setAuthToken(a, AccountAuthenticator.ACCESS_TOKEN, accessToken);
    am.setAuthToken(a, AccountAuthenticator.REFRESH_TOKEN, refreshToken);
    am.setUserData(a, AccountAuthenticator.EXPIRATION_MS,
        Long.toString(expirationMs));
    am.setUserData(a, AccountAuthenticator.SCOPES, scopes);
    return true;
  }

  public static void updateAccount(
      Context ctx,
      String accountName,
      String accessToken,
      long expirationMs,
      String scopes) {
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);
    am.setAuthToken(a, AccountAuthenticator.ACCESS_TOKEN, accessToken);
    am.setUserData(a, AccountAuthenticator.EXPIRATION_MS,
        Long.toString(expirationMs));
    am.setUserData(a, AccountAuthenticator.SCOPES, scopes);
  }

  public static boolean removeAccount(Context ctx, String accountName) {
    // TODO(btmura): use new API to remove account when possible
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);
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

  private AccountUtils() {
  }
}
