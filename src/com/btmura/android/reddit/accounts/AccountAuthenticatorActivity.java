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

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.app.AddAccountFragment;
import com.btmura.android.reddit.app.AddAccountFragment.OnAccountAddedListener;
import com.btmura.android.reddit.content.ThemePrefs;

public class AccountAuthenticatorActivity extends android.accounts.AccountAuthenticatorActivity
        implements OnAccountAddedListener {

    public static final String TAG = "AccountAuthenticatorActivity";

    public static final String EXTRA_LOGIN = "login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemePrefs.getDialogTheme(this));
        setContentView(R.layout.account_authenticator);

        if (savedInstanceState == null) {
            Fragment frag = AddAccountFragment.newInstance(getIntent().getStringExtra(EXTRA_LOGIN));
            // FragmentTransaction ft = getFragmentManager().beginTransaction();
            // ft.replace(R.id.account_authenticator_container, frag);
            // ft.commit();
        }
    }

    public void onAccountAdded(Bundle result) {
        setAccountAuthenticatorResult(result);
        setResult(RESULT_OK);
        finish();
    }

    public void onAccountCancelled() {
        finish();
    }
}
