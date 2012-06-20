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

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

import com.btmura.android.reddit.R;
import com.btmura.android.reddit.content.LoginLoader.LoginResult;
import com.btmura.android.reddit.text.InputFilters;

public class AddAccountFragment extends Fragment implements
        OnCheckedChangeListener,
        OnClickListener,
        LoginFragment.LoginResultListener {

    public static final String TAG = "AddAccountFragment";

    private static final InputFilter[] INPUT_FILTERS = new InputFilter[] {
            InputFilters.LOGIN_FILTER,
    };

    public interface OnAccountAddedListener {
        void onAccountAdded(String login, String cookie, String modhash);
        void onAccountCancelled();
    }

    private OnAccountAddedListener listener;

    private EditText login;
    private EditText password;
    private Button add;
    private Button cancel;

    public static AddAccountFragment newInstance() {
        return new AddAccountFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnAccountAddedListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_account, container, false);

        login = (EditText) v.findViewById(R.id.login);
        login.setFilters(INPUT_FILTERS);

        password = (EditText) v.findViewById(R.id.password);

        CheckBox showPassword = (CheckBox) v.findViewById(R.id.show_password);
        showPassword.setOnCheckedChangeListener(this);

        cancel = (Button) v.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        add = (Button) v.findViewById(R.id.add);
        add.setOnClickListener(this);

        return v;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        TransformationMethod method = null;
        if (!isChecked) {
            method = PasswordTransformationMethod.getInstance();
        }
        password.setTransformationMethod(method);
    }

    public void onClick(View v) {
        if (v == cancel) {
            handleCancel();
        } else if (v == add) {
            handleAdd();
        }
    }

    private void handleCancel() {
        if (listener != null) {
            listener.onAccountCancelled();
        }
    }

    private void handleAdd() {
        if (login.getText().length() <= 0) {
            login.setError(getString(R.string.error_blank_field));
        }
        if (password.getText().length() <= 0) {
            password.setError(getString(R.string.error_blank_field));
        }
        if (login.getError() == null && password.getError() == null) {
            LoginFragment frag = LoginFragment.newInstance(login.getText().toString(),
                    password.getText().toString());
            frag.setTargetFragment(this, 0);
            frag.show(getFragmentManager(), LoginFragment.TAG);
        }
    }

    public void onLoginResult(LoginResult result) {
        FragmentManager fm = getFragmentManager();
        if (result == null) {
            SimpleDialogFragment.showMessage(fm, getString(R.string.error));
        } else if (result.error != null) {
            SimpleDialogFragment.showMessage(fm, getString(R.string.error_reddit, result.error));
        } else {
            LoginFragment frag = (LoginFragment) fm.findFragmentByTag(LoginFragment.TAG);
            if (listener != null) {
                listener.onAccountAdded(frag.getLogin(), result.cookie, result.modhash);
            }
        }
    }
}
