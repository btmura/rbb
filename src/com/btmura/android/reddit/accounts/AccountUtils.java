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

import java.io.IOException;

public class AccountUtils {

  public static final String NO_ACCOUNT = "";

  public static boolean isAccount(String accountName) {
    return !TextUtils.isEmpty(accountName);
  }

  public static Account getAccount(Context ctx, String accountName) {
    return new Account(accountName, AccountAuthenticator.getAccountType(ctx));
  }

  public static boolean hasTokens(Context ctx, String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    return getAccessToken(ctx, accountName) != null
        && getRefreshToken(ctx, accountName) != null;
  }

  @Nullable
  public static String getAccessToken(Context ctx, String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    return getAuthToken(ctx, accountName,
        AccountAuthenticator.AUTH_TOKEN_ACCESS_TOKEN);
  }

  @Nullable
  public static String getRefreshToken(Context ctx, String accountName) throws
      AuthenticatorException,
      OperationCanceledException,
      IOException {
    return getAuthToken(ctx, accountName,
        AccountAuthenticator.AUTH_TOKEN_REFRESH_TOKEN);
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
    return am.blockingGetAuthToken(a, authTokenType,
        true /* display notification */);
  }

  public static void setAccessToken(
      Context ctx,
      String accountName,
      String accessToken) {
    // TODO(btmura): check accountName is not empty
    Account a = getAccount(ctx, accountName);
    AccountManager am = AccountManager.get(ctx);
    am.setAuthToken(a, AccountAuthenticator.AUTH_TOKEN_ACCESS_TOKEN,
        accessToken);
  }

  private AccountUtils() {
  }
}
