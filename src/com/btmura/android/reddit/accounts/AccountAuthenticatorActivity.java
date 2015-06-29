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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.AddAccountFragment;
import com.btmura.android.reddit.app.LoginFragment;
import com.btmura.android.reddit.content.ThemePrefs;

public class AccountAuthenticatorActivity
    extends SupportAccountAuthenticatorActivity
    implements LoginFragment.OnLoginListener,
    AddAccountFragment.OnAccountAddedListener {

  private static final String TAG_ADD_ACCOUNT = "AddAccount";

  public static final String EXTRA_USERNAME = "username";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(ThemePrefs.getTheme(this));
    setContentView(R.layout.account_authenticator);
    if (savedInstanceState == null) {
      LoginFragment frag = LoginFragment.newInstance(newStateToken());
      FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
      ft.replace(R.id.account_authenticator_container, frag);
      ft.commit();
    }
  }

  private static CharSequence newStateToken() {
    return new StringBuilder("rbb_").append(System.currentTimeMillis());
  }

  @Override
  public void onLogin(String oauthCallbackUrl) {
    // TODO(btmura): compare state tokens
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    AddAccountFragment.newInstance(oauthCallbackUrl)
        .show(ft, TAG_ADD_ACCOUNT);
  }

  @Override
  public void onAccountAdded(Bundle result) {
    setAccountAuthenticatorResult(result);
    setResult(RESULT_OK);
    finish();
  }

  @Override
  public void onAccountCancelled() {
    finish();
  }
}
