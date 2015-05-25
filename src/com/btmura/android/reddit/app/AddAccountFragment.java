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

package com.btmura.android.reddit.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.btmura.android.reddit.BuildConfig;
import com.btmura.android.reddit.accounts.AccountAuthenticator;
import com.btmura.android.reddit.net.AccessTokenResult;

import java.io.IOException;

public class AddAccountFragment extends Fragment {

    private static final String TAG = "AddAccountFragment";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String ARG_OAUTH_CALLBACK_URL = "oauthCallbackUrl";

    private OAuthRedirectFragment.OnAccountAddedListener listener;
    private AddAccountTask task;

    public static AddAccountFragment newInstance(String oauthCallbackUrl) {
        // TODO(btmura): add precondition check for oauth url
        Bundle args = new Bundle(1);
        args.putString(ARG_OAUTH_CALLBACK_URL, oauthCallbackUrl);

        AddAccountFragment frag = new AddAccountFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OAuthRedirectFragment.OnAccountAddedListener) {
            listener = (OAuthRedirectFragment.OnAccountAddedListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (task == null) {
            task = new AddAccountTask();
            task.execute();
        }
    }

    class AddAccountTask extends AsyncTask<Void, Integer, Bundle> {

        private final Context ctx = getActivity().getApplicationContext();

        @Override
        protected Bundle doInBackground(Void... params) {
            try {
                Uri uri = getOAuthCallbackUri();
                if (DEBUG) {
                    Log.d(TAG, "uri: " + uri);
                }

                String username = getUsername(uri);
                if (DEBUG) {
                    Log.d(TAG, "username: " + username);
                }
                if (!isValidUsername(username)) {
                    // TODO(btmura): show error message
                    return Bundle.EMPTY;
                }

                String code = getCode(uri);
                if (DEBUG) {
                    Log.d(TAG, "code: " + code);
                }
                if (!isValidCode(code)) {
                    // TODO(btmura): show error message
                    return Bundle.EMPTY;
                }

                AccessTokenResult atr = getAccessTokenResult(ctx, code);
                if (DEBUG) {
                    Log.d(TAG, "atr: " + atr);
                }
                if (!isValidAccessTokenResult(atr)) {
                    // TODO(btmura): show error message
                    return Bundle.EMPTY;
                }

                Account a = new Account(username, AccountAuthenticator.getAccountType(ctx));
                AccountManager am = AccountManager.get(ctx);
                am.addAccountExplicitly(a, null /* password */, null /* userdata */);
                am.setAuthToken(a, AccountAuthenticator.AUTH_TOKEN_ACCESS_TOKEN, atr.accessToken);
                am.setAuthToken(a, AccountAuthenticator.AUTH_TOKEN_REFRESH_TOKEN, atr.refreshToken);

                Bundle b = new Bundle(2);
                b.putString(AccountManager.KEY_ACCOUNT_NAME, a.name);
                b.putString(AccountManager.KEY_ACCOUNT_TYPE, a.type);
                return b;
            } catch (IOException e) {
                // TODO(btmura): show error message to user
                Log.e(TAG, "error getting access token", e);
            }
            return Bundle.EMPTY;
        }

        private Uri getOAuthCallbackUri() {
            return Uri.parse(getArguments().getString(ARG_OAUTH_CALLBACK_URL));
        }

        private String getUsername(Uri uri) {
            String s = getQueryParameter(uri, "state");
            if (TextUtils.isEmpty(s)) {
                return null;
            }
            return s.substring(4);
        }

        private boolean isValidUsername(String username) {
            return !TextUtils.isEmpty(username);
        }

        private String getCode(Uri uri) {
            return getQueryParameter(uri, "code");
        }

        private boolean isValidCode(String code) {
            return !TextUtils.isEmpty(code);
        }

        private AccessTokenResult getAccessTokenResult(Context ctx, String code)
                throws IOException {
            return AccessTokenResult.getAccessToken(ctx, code);
        }

        private boolean isValidAccessTokenResult(AccessTokenResult atr) {
            return !TextUtils.isEmpty(atr.accessToken) && !TextUtils.isEmpty(atr.refreshToken);
        }

        private String getQueryParameter(Uri uri, String key) {
            try {
                return uri.getQueryParameter(key);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "error parsing callback url", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            task = null;
            if (bundle != Bundle.EMPTY && listener != null) {
                listener.onAccountAdded(bundle);
            }
        }
    }
}
