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

package com.btmura.android.reddit.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.LoginLoader;
import com.btmura.android.reddit.content.LoginLoader.LoginResult;

public class LoginFragment extends DialogFragment implements LoaderCallbacks<LoginResult> {

    public static final String TAG = "LoginFragment";

    private static final String ARG_LOGIN = "l";
    private static final String ARG_PASSWORD = "p";

    public interface LoginResultListener {
        void onLoginResult(LoginResult result);
    }

    public static LoginFragment newInstance(String login, String password) {
        Bundle args = new Bundle(2);
        args.putString(ARG_LOGIN, login);
        args.putString(ARG_PASSWORD, password);
        LoginFragment frag = new LoginFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.logging_in));
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<LoginResult> onCreateLoader(int id, Bundle args) {
        return new LoginLoader(getActivity().getApplicationContext(), getLogin(), getPassword());
    }

    public void onLoadFinished(Loader<LoginResult> loader, final LoginResult result) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }
            
            @Override
            protected void onPostExecute(Void voidRay) {
                dismiss();
                getLoginResultListener().onLoginResult(result);
            }
        }.execute();        
    }

    public void onLoaderReset(Loader<LoginResult> loader) {
    }

    public String getLogin() {
        return getArguments().getString(ARG_LOGIN);
    }

    public String getPassword() {
        return getArguments().getString(ARG_PASSWORD);
    }

    private LoginResultListener getLoginResultListener() {
        return (LoginResultListener) getTargetFragment();
    }
}
