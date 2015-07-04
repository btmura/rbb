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

package com.btmura.android.reddit.content;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.util.Log;

import com.btmura.android.reddit.net.AccountInfoResult;
import com.btmura.android.reddit.net.RedditApi;

import java.io.IOException;

/**
 * {@link BaseAsyncTaskLoader} that loads a user's account info.
 * It used to show a user's karma count in the action bar dropdown.
 */
public class UserInfoLoader extends BaseAsyncTaskLoader<AccountInfoResult> {

  public static final String TAG = "UserInfoLoader";

  private final String accountName;
  private final String user;

  public UserInfoLoader(Context ctx, String accountName, String user) {
    super(ctx.getApplicationContext());
    this.accountName = accountName;
    this.user = user;
  }

  @Override
  public AccountInfoResult loadInBackground() {
    try {
      return RedditApi.getUserInfo(getContext(), accountName, user);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage(), e);
    } catch (AuthenticatorException e) {
      Log.e(TAG, e.getMessage(), e);
    } catch (OperationCanceledException e) {
      Log.e(TAG, e.getMessage(), e);
    }
    return null;
  }
}
